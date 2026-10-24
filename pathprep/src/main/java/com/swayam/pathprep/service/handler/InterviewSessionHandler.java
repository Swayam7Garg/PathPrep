package com.swayam.pathprep.service.handler;

import com.swayam.pathprep.dto.request.AnswerRequest;
import com.swayam.pathprep.dto.response.AnswerResponse;
import com.swayam.pathprep.dto.response.QuestionView;
import com.swayam.pathprep.entity.*;
import com.swayam.pathprep.enums.SessionMode;
import com.swayam.pathprep.enums.SessionStatus;
import com.swayam.pathprep.enums.SessionStyle;
import com.swayam.pathprep.exception.BadRequestException;
import com.swayam.pathprep.exception.ConflictException;
import com.swayam.pathprep.exception.NotFoundException;
import com.swayam.pathprep.repository.*;
import com.swayam.pathprep.service.InterviewService;
import com.swayam.pathprep.service.SessionFinalizer;
import com.swayam.pathprep.mapper.QuestionMapper;
import com.swayam.pathprep.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Handles text-based answer submissions for INTERVIEW mode sessions.
 * Graded by AI rubric via InterviewService, with zero impact on MCQ Quiz logic.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewSessionHandler {

    private final PracticeSessionRepository practiceSessionRepository;
    private final SessionQuestionRepository sessionQuestionRepository;
    private final QuestionRepository questionRepository;
    private final AttemptRepository attemptRepository;
    private final InterviewService interviewService;
    private final SessionFinalizer sessionFinalizer;
    private final QuestionMapper questionMapper;
    private final CurrentUser currentUser;
    private final Clock clock;

    @Transactional
    public AnswerResponse submitInterviewAnswer(Long sessionId, AnswerRequest request) {
        log.info("Interview answer submission started: sessionId={}, idempotencyKey={}",
                sessionId, request.idempotencyKey());

        // 1. Idempotency check
        Optional<Attempt> existing = attemptRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            return buildResponseFromAttempt(existing.get());
        }

        // 2. Validate user & lock session
        Long userId = currentUser.getUserId();
        PracticeSession session = practiceSessionRepository.findByIdAndUserIdWithLock(sessionId, userId)
                .orElseThrow(() -> new NotFoundException("SESSION_NOT_FOUND", "Session not found: " + sessionId));

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new ConflictException("SESSION_ALREADY_COMPLETED", "Session " + sessionId + " is already " + session.getStatus());
        }

        if (session.getMode() != SessionMode.INTERVIEW) {
            throw new BadRequestException("MODE_NOT_APPLICABLE", "Session is not in INTERVIEW mode");
        }

        if (request.answerText() == null || request.answerText().trim().length() < 3) {
            throw new BadRequestException("VALIDATION_FAILED", "answerText is required for interview mode");
        }

        // 3. Current question
        int currentPosition = session.getCurrentIndex();
        SessionQuestionId sqId = new SessionQuestionId(sessionId, currentPosition);
        SessionQuestion currentSq = sessionQuestionRepository.findById(sqId)
                .orElseThrow(() -> new NotFoundException("QUESTION_NOT_FOUND", "No question at index " + currentPosition));

        Question question = currentSq.getQuestion();
        Instant now = clock.instant();

        // 4. Save attempt
        Attempt attempt = new Attempt();
        attempt.setSession(session);
        attempt.setUser(session.getUser());
        attempt.setQuestion(question);
        attempt.setAnswerText(request.answerText().trim());
        attempt.setTimeSpentSec(request.timeSpentSec());
        attempt.setIdempotencyKey(request.idempotencyKey());
        attempt.setAnsweredAt(now);
        attempt.setCorrect(false); // will be updated upon evaluation
        attempt = attemptRepository.saveAndFlush(attempt);

        // 5. Evaluate answer via InterviewService
        Evaluation evaluation = interviewService.gradeInterviewAnswer(sessionId, userId, attempt.getId(), request.answerText().trim());

        // 6. Advance index and session progress
        session.setAnsweredCount(session.getAnsweredCount() + 1);
        if (attempt.isCorrect()) {
            session.setCorrectCount(session.getCorrectCount() + 1);
        }

        int nextIndex = currentPosition + 1;
        session.setCurrentIndex(nextIndex);
        session.setCurrentQuestionStartedAt(now);

        boolean isFinal = nextIndex >= session.getTotalQuestions();
        if (isFinal) {
            sessionFinalizer.finalizeSession(session, false);
        } else {
            practiceSessionRepository.save(session);
        }

        // 7. Next question view
        QuestionView nextQuestionView = null;
        if (!isFinal) {
            SessionQuestionId nextSqId = new SessionQuestionId(sessionId, nextIndex);
            nextQuestionView = sessionQuestionRepository.findById(nextSqId)
                    .map(sq -> questionRepository.findWithDetailsById(sq.getQuestion().getId()).orElse(sq.getQuestion()))
                    .map(questionMapper::toView)
                    .orElse(null);
        }

        return new AnswerResponse(
                attempt.isCorrect(),
                0L,
                evaluation.getFeedback(),
                session.getStatus(),
                session.getCurrentIndex(),
                session.getTotalQuestions(),
                nextQuestionView
        );
    }

    private AnswerResponse buildResponseFromAttempt(Attempt attempt) {
        PracticeSession session = attempt.getSession();
        int nextIndex = session.getCurrentIndex();
        QuestionView nextQuestionView = null;
        if (session.getStatus() == SessionStatus.IN_PROGRESS && nextIndex < session.getTotalQuestions()) {
            SessionQuestionId nextSqId = new SessionQuestionId(session.getId(), nextIndex);
            nextQuestionView = sessionQuestionRepository.findById(nextSqId)
                    .map(sq -> questionRepository.findWithDetailsById(sq.getQuestion().getId()).orElse(sq.getQuestion()))
                    .map(questionMapper::toView)
                    .orElse(null);
        }

        return new AnswerResponse(
                attempt.isCorrect(),
                0L,
                "Previously graded answer",
                session.getStatus(),
                session.getCurrentIndex(),
                session.getTotalQuestions(),
                nextQuestionView
        );
    }
}
