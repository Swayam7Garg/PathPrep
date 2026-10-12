package com.swayam.pathprep.service.handler;

import com.swayam.pathprep.dto.AnswerRequest;
import com.swayam.pathprep.dto.AnswerResponse;
import com.swayam.pathprep.dto.QuestionView;
import com.swayam.pathprep.entity.*;
import com.swayam.pathprep.enums.SessionMode;
import com.swayam.pathprep.enums.SessionStatus;
import com.swayam.pathprep.event.AttemptGradedEvent;
import com.swayam.pathprep.exception.ConflictException;
import com.swayam.pathprep.exception.NotFoundException;
import com.swayam.pathprep.exception.UnprocessableException;
import com.swayam.pathprep.mapper.QuestionMapper;
import com.swayam.pathprep.repository.*;
import com.swayam.pathprep.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Handles {@code POST /api/v1/sessions/{id}/answers} for QUIZ-mode sessions (Phase 4).
 *
 * <h3>Processing pipeline (all inside a single {@code @Transactional}):</h3>
 * <ol>
 *   <li><b>Idempotency check</b> — if this key was already processed, return the stored
 *       result immediately without touching any other state.</li>
 *   <li><b>Session ownership + lock</b> — load the session with {@code PESSIMISTIC_WRITE}
 *       to prevent concurrent submissions racing on the same row. Returns 404 (never 403)
 *       if the session is not found or belongs to another user.</li>
 *   <li><b>Status guard</b> — reject with 409 CONFLICT if session is not {@code IN_PROGRESS}.</li>
 *   <li><b>Mode guard</b> — reject with 422 if the session mode is not {@code QUIZ}.</li>
 *   <li><b>Current question lookup</b> — find the {@link SessionQuestion} at
 *       {@code position = currentIndex}.</li>
 *   <li><b>Option ownership</b> — verify the submitted {@code selectedOptionId} belongs
 *       to the current question; throw 404 if not.</li>
 *   <li><b>Java grading</b> — compare {@code selectedOption.isCorrect()} (zero AI, zero Groq).</li>
 *   <li><b>Persist attempt</b> — save to {@code attempts} table with idempotency key.</li>
 *   <li><b>Advance index</b> — increment {@code currentIndex} on the session.</li>
 *   <li><b>Completion check</b> — if {@code currentIndex == totalQuestions}, mark session
 *       {@code COMPLETED} and set {@code completedAt}.</li>
 *   <li><b>Publish event</b> — fire {@link AttemptGradedEvent} for future mastery hooks.</li>
 *   <li><b>Build response</b> — include grading result, correct option info, and the next
 *       sanitised question (or {@code null} if done).</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuizSessionHandler {

    private final CurrentUser currentUser;
    private final UserRepository userRepository;
    private final PracticeSessionRepository practiceSessionRepository;
    private final SessionQuestionRepository sessionQuestionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final AttemptRepository attemptRepository;
    private final QuestionRepository questionRepository;
    private final QuestionMapper questionMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Submits and grades a single MCQ answer for the current question in a quiz session.
     *
     * @param sessionId the session to submit against
     * @param request   the answer payload ({@code selectedOptionId} + {@code idempotencyKey})
     * @return fully populated {@link AnswerResponse} — same object is returned for retried requests
     */
    @Transactional
    public AnswerResponse submitAnswer(Long sessionId, AnswerRequest request) {
        log.info("Answer submission started: sessionId={}, selectedOptionId={}, idempotencyKey={}",
                sessionId, request.selectedOptionId(), request.idempotencyKey());

        // ── Step 1: Idempotency ───────────────────────────────────────────────────────────
        // Check the idempotency key FIRST, before acquiring any lock or reading session state.
        // If this key was already stored, return the previously computed result immediately.
        // This handles network retries and double-clicks safely.
        var existingAttempt = attemptRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existingAttempt.isPresent()) {
            Attempt cached = existingAttempt.get();
            log.info("Idempotent replay: sessionId={}, idempotencyKey={}, attemptId={}",
                    sessionId, request.idempotencyKey(), cached.getId());
            return buildResponseFromAttempt(cached);
        }

        // ── Step 2: Resolve the authenticated user ────────────────────────────────────────
        Long userId = currentUser.getUserId();
        log.info("Resolving user for answer submission: userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found: " + userId));

        // ── Step 3: Load session with PESSIMISTIC_WRITE lock (ownership enforced in query) ─
        // Using a DB-level row lock so two concurrent submissions cannot both read
        // currentIndex=N, both grade, and both write currentIndex=N+1 (double-advance bug).
        // The query also filters by user_id, so a student cannot touch another student's session.
        // Returns 404 (not 403) to avoid leaking session existence information.
        log.info("Acquiring session lock: sessionId={}, userId={}", sessionId, userId);
        PracticeSession session = practiceSessionRepository
                .findByIdAndUserIdWithLock(sessionId, userId)
                .orElseThrow(() -> {
                    log.info("Session not found or not owned: sessionId={}, userId={}", sessionId, userId);
                    return new NotFoundException("SESSION_NOT_FOUND", "Session not found: " + sessionId);
                });

        // ── Step 4: Status guard ──────────────────────────────────────────────────────────
        // Only IN_PROGRESS sessions can accept new answers.
        // COMPLETED / FAILED sessions return 409 CONFLICT (state conflict, not a bad request).
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            log.info("Session not in progress: sessionId={}, status={}", sessionId, session.getStatus());
            throw new ConflictException("SESSION_ALREADY_COMPLETED",
                    "Session " + sessionId + " is already " + session.getStatus());
        }

        // ── Step 5: Mode guard ────────────────────────────────────────────────────────────
        // This handler only grades QUIZ (MCQ) mode. INTERVIEW mode is handled differently
        // (Phase 8, AI-based rubric). Reject here with 422 to signal unsupported operation.
        if (session.getMode() != SessionMode.QUIZ) {
            log.info("Wrong session mode for answer submission: sessionId={}, mode={}", sessionId, session.getMode());
            throw new UnprocessableException("MODE_NOT_APPLICABLE",
                    "Answer submission is only supported for QUIZ mode sessions");
        }

        // ── Step 6: Current question lookup ──────────────────────────────────────────────
        // session_questions is ordered by position (0-indexed). currentIndex points to the
        // question the student should be answering right now.
        int currentPosition = session.getCurrentIndex();
        log.info("Looking up current question: sessionId={}, position={}", sessionId, currentPosition);

        // We use the composite key (sessionId, position) to find the current question directly
        SessionQuestionId sqId = new SessionQuestionId(sessionId, currentPosition);
        SessionQuestion currentSQ = sessionQuestionRepository.findById(sqId)
                .orElseThrow(() -> {
                    log.info("No session question at position={} for sessionId={}", currentPosition, sessionId);
                    return new NotFoundException("QUESTION_NOT_FOUND",
                            "No question at position " + currentPosition + " for session " + sessionId);
                });
        Question currentQuestion = currentSQ.getQuestion();
        log.info("Current question resolved: questionId={}", currentQuestion.getId());

        // ── Step 7: Option ownership validation ───────────────────────────────────────────
        // Ensure the submitted option actually belongs to the current question.
        // We load the option by its ID and verify question_id matches. This prevents a student
        // from submitting an option from a different question in the same session.
        // Returns 404 so we don't leak which option IDs exist.
        log.info("Validating option ownership: selectedOptionId={}, questionId={}",
                request.selectedOptionId(), currentQuestion.getId());
        QuestionOption selectedOption = questionOptionRepository.findById(request.selectedOptionId())
                .orElseThrow(() -> {
                    log.info("Option not found: selectedOptionId={}", request.selectedOptionId());
                    return new NotFoundException("OPTION_NOT_FOUND",
                            "Option not found: " + request.selectedOptionId());
                });

        // Confirm the option belongs to the current question (not a different question's option)
        if (!selectedOption.getQuestion().getId().equals(currentQuestion.getId())) {
            log.info("Option does not belong to current question: optionId={}, questionId={}, actualQuestionId={}",
                    selectedOption.getId(), currentQuestion.getId(), selectedOption.getQuestion().getId());
            throw new NotFoundException("OPTION_NOT_FOUND",
                    "Option " + request.selectedOptionId() + " does not belong to the current question");
        }

        // ── Step 8: Java grading (zero AI) ────────────────────────────────────────────────
        // Grading is done entirely in Java by checking the is_correct flag on the chosen option.
        // No LLM, no Groq, no external call. The correct option is found by scanning the
        // question's options list so we can always return correctOptionId + explanation.
        boolean correct = selectedOption.isCorrect();
        log.info("Grading result: sessionId={}, questionId={}, selectedOptionId={}, correct={}",
                sessionId, currentQuestion.getId(), selectedOption.getId(), correct);

        // Load full question with options to find the correct option's explanation
        Question questionWithOptions = questionRepository.findWithDetailsById(currentQuestion.getId())
                .orElse(currentQuestion);

        QuestionOption correctOption = questionWithOptions.getOptions().stream()
                .filter(QuestionOption::isCorrect)
                .findFirst()
                .orElse(selectedOption); // fallback: treat selected as correct if no correct option flagged

        // ── Step 9: Persist the attempt ────────────────────────────────────────────────────
        // Save to the attempts table. The UNIQUE constraint on idempotency_key acts as a
        // second safety net against races (in addition to the PESSIMISTIC_WRITE lock above).
        Attempt attempt = new Attempt();
        attempt.setSession(session);
        attempt.setUser(user);
        attempt.setQuestion(currentQuestion);
        attempt.setSelectedOption(selectedOption);
        attempt.setCorrect(correct);
        attempt.setIdempotencyKey(request.idempotencyKey());
        attempt.setAnsweredAt(Instant.now());
        attempt = attemptRepository.save(attempt);
        log.info("Attempt persisted: attemptId={}, correct={}", attempt.getId(), correct);

        // ── Step 10: Advance the session pointer ──────────────────────────────────────────
        // Increment currentIndex to point at the next question. The updated session will be
        // flushed to the DB when the transaction commits.
        session.setCurrentIndex(currentPosition + 1);

        // ── Step 11: Completion check ─────────────────────────────────────────────────────
        // If currentIndex now equals totalQuestions, all questions have been answered.
        // Mark the session COMPLETED and record the wall-clock finish time.
        boolean sessionDone = session.getCurrentIndex() >= session.getTotalQuestions();
        if (sessionDone) {
            session.setStatus(SessionStatus.COMPLETED);
            session.setCompletedAt(Instant.now());
            log.info("Session completed: sessionId={}, totalQuestions={}", sessionId, session.getTotalQuestions());
        } else {
            log.info("Session advancing: sessionId={}, newIndex={} of {}",
                    sessionId, session.getCurrentIndex(), session.getTotalQuestions());
        }
        practiceSessionRepository.save(session);

        // ── Step 12: Publish AttemptGradedEvent ────────────────────────────────────────────
        // Publish a Spring application event so future mastery-tracking listeners (Phase 7+)
        // can react without any changes to this submission flow.
        eventPublisher.publishEvent(new AttemptGradedEvent(this, attempt));
        log.info("AttemptGradedEvent published: attemptId={}", attempt.getId());

        // ── Step 13: Build and return response ─────────────────────────────────────────────
        // Load the next question (sanitised — no isCorrect, no explanation) only if not done.
        QuestionView nextQuestion = null;
        if (!sessionDone) {
            int nextPosition = session.getCurrentIndex();
            SessionQuestionId nextSqId = new SessionQuestionId(sessionId, nextPosition);
            nextQuestion = sessionQuestionRepository.findById(nextSqId)
                    .map(sq -> questionRepository.findWithDetailsById(sq.getQuestion().getId())
                            .orElse(sq.getQuestion()))
                    .map(questionMapper::toView)
                    .orElse(null);
            log.info("Next question loaded: sessionId={}, position={}, questionId={}",
                    sessionId, nextPosition, nextQuestion != null ? nextQuestion.id() : "null");
        }

        log.info("Answer submission complete: sessionId={}, correct={}, sessionStatus={}",
                sessionId, correct, session.getStatus());

        return new AnswerResponse(
                correct,
                correctOption.getId(),
                correctOption.getExplanation(),
                session.getStatus(),
                session.getCurrentIndex(),
                session.getTotalQuestions(),
                nextQuestion
        );
    }

    /**
     * Reconstructs an {@link AnswerResponse} from a previously persisted {@link Attempt}.
     *
     * <p>Called on idempotent replay (same key seen again). We re-build the response from
     * stored data so the client receives an identical answer to its first request.
     */
    private AnswerResponse buildResponseFromAttempt(Attempt cached) {
        log.info("Building idempotent replay response for attemptId={}", cached.getId());

        PracticeSession session = cached.getSession();

        // Find correct option for the question this attempt answered
        Question q = questionRepository.findWithDetailsById(cached.getQuestion().getId())
                .orElse(cached.getQuestion());
        QuestionOption correctOption = q.getOptions().stream()
                .filter(QuestionOption::isCorrect)
                .findFirst()
                .orElse(cached.getSelectedOption());

        // Rebuild the next question view (the session pointer has already been advanced)
        QuestionView nextQuestion = null;
        boolean sessionDone = session.getStatus() == SessionStatus.COMPLETED
                || session.getStatus() == SessionStatus.FAILED;
        if (!sessionDone) {
            int nextPosition = session.getCurrentIndex();
            SessionQuestionId nextSqId = new SessionQuestionId(session.getId(), nextPosition);
            nextQuestion = sessionQuestionRepository.findById(nextSqId)
                    .map(sq -> questionRepository.findWithDetailsById(sq.getQuestion().getId())
                            .orElse(sq.getQuestion()))
                    .map(questionMapper::toView)
                    .orElse(null);
        }

        return new AnswerResponse(
                cached.isCorrect(),
                correctOption.getId(),
                correctOption.getExplanation(),
                session.getStatus(),
                session.getCurrentIndex(),
                session.getTotalQuestions(),
                nextQuestion
        );
    }
}
