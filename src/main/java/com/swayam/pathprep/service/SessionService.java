package com.swayam.pathprep.service;

import com.swayam.pathprep.config.SessionProperties;
import com.swayam.pathprep.dto.AnswerRequest;
import com.swayam.pathprep.dto.AnswerResponse;
import com.swayam.pathprep.dto.QuestionView;
import com.swayam.pathprep.dto.SessionCreateRequest;
import com.swayam.pathprep.dto.SessionResponse;
import com.swayam.pathprep.entity.*;
import com.swayam.pathprep.enums.QuizDifficulty;
import com.swayam.pathprep.enums.SessionMode;
import com.swayam.pathprep.exception.BadRequestException;
import com.swayam.pathprep.exception.FeatureNotAvailableException;
import com.swayam.pathprep.exception.NotFoundException;
import com.swayam.pathprep.exception.UnprocessableException;
import com.swayam.pathprep.mapper.QuestionMapper;
import com.swayam.pathprep.repository.*;
import com.swayam.pathprep.repository.projection.QuestionCandidateRow;
import com.swayam.pathprep.security.CurrentUser;
import com.swayam.pathprep.service.handler.QuizSessionHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core domain service managing practice sessions (Phase 3).
 *
 * <p>Key responsibilities:
 * <ul>
 *   <li>Validates requested track, topics, difficulty, and question count.</li>
 *   <li>Finds published candidate questions and invokes {@link QuestionSelector}
 *       to distribute questions proportionally according to topic weights.</li>
 *   <li>Persists the session and deterministic question ordering (0..N-1) in {@code session_questions}.</li>
 *   <li>Loads and sanitizes Question 1 to prevent answer leakage in the initial payload.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final CurrentUser currentUser;
    private final UserRepository userRepository;
    private final RoleTrackRepository roleTrackRepository;
    private final TrackTopicRepository trackTopicRepository;
    private final QuestionRepository questionRepository;
    private final PracticeSessionRepository practiceSessionRepository;
    private final SessionQuestionRepository sessionQuestionRepository;
    private final QuestionSelector questionSelector;
    private final QuestionMapper questionMapper;
    private final SessionProperties sessionProperties;
    // Phase 4: delegates MCQ grading to a dedicated handler (strategy pattern)
    private final QuizSessionHandler quizSessionHandler;

    /**
     * Initializes a new practice quiz session in a single transactional unit of work.
     *
     * <h3>Workflow:</h3>
     * <ol>
     *   <li><b>Feature Guard:</b> Rejects INTERVIEW mode (reserved for Phase 8 AI mock interviews).</li>
     *   <li><b>Identity & Track Verification:</b> Confirms existence of active user and target track.</li>
     *   <li><b>Topic Validation & Weight Extraction:</b> Verifies that all requested topic IDs belong
     *       to the track; falls back to all track topics if none specified. Extracts weights for selection.</li>
     *   <li><b>Constraint Enforcement:</b> Clamps question count between 1 and configured {@code maxQuestions}.</li>
     *   <li><b>Candidate Discovery:</b> Fetches lightweight projection rows (ID + topicId) of published questions.</li>
     *   <li><b>Fair Selection:</b> Calls {@link QuestionSelector} to allocate questions across topics fairly.</li>
     *   <li><b>Session & Sequence Persistence:</b> Saves {@link PracticeSession} and ordered {@link SessionQuestion} entries.</li>
     *   <li><b>Sanitized First Question:</b> Pre-loads Question 1 without answer keys and builds {@link SessionResponse}.</li>
     * </ol>
     *
     * @param request user input payload specifying track, optional topics, difficulty, and count
     * @return {@link SessionResponse} containing session metadata, sanitized Question 1, and any warnings
     * @throws FeatureNotAvailableException if mode is INTERVIEW
     * @throws NotFoundException if user or track cannot be found
     * @throws BadRequestException if topic does not belong to track or count is invalid
     * @throws UnprocessableException if no published questions exist for criteria
     */
    @Transactional
    public SessionResponse createSession(SessionCreateRequest request) {
        log.info("Processing session creation request: trackId={}, mode={}, difficulty={}, questionCount={}",
                request.trackId(), request.mode(), request.difficulty(), request.questionCount());

        // Step 1: Feature guard - Interview mode requires Groq LLM integration (Phase 8)
        if (request.mode() == SessionMode.INTERVIEW) {
            log.info("Interview mode requested but not yet available in current phase");
            throw new FeatureNotAvailableException("MODE_NOT_AVAILABLE", "Interview mode is not available yet. Please use QUIZ mode.");
        }

        // Step 2: Resolve current authenticated user context
        Long userId = currentUser.getUserId();
        log.info("Authenticating session user: userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.info("User lookup failed: userId={}", userId);
                    return new NotFoundException("USER_NOT_FOUND", "User not found: " + userId);
                });

        // Step 3: Validate that the requested role track exists in the catalog
        RoleTrack track = roleTrackRepository.findById(request.trackId())
                .orElseThrow(() -> {
                    log.info("Track lookup failed: trackId={}", request.trackId());
                    return new NotFoundException("TRACK_NOT_FOUND", "Track not found: " + request.trackId());
                });

        // Step 4: Load track topics to validate requested topic IDs and fetch placement weights
        List<TrackTopic> trackTopics = trackTopicRepository.findByTrackId(track.getId());
        Set<Long> validTopicIds = trackTopics.stream()
                .map(tt -> tt.getTopic().getId())
                .collect(Collectors.toSet());
        Map<Long, BigDecimal> topicWeights = trackTopics.stream()
                .collect(Collectors.toMap(tt -> tt.getTopic().getId(), TrackTopic::getWeight));

        // Step 5: Resolve target topic IDs.
        // If the user specified topic IDs, ensure all of them belong to this track (prevents cross-track leakage).
        // If omitted, default to all topics mapped to this track.
        List<Long> targetTopicIds;
        if (request.topicIds() != null && !request.topicIds().isEmpty()) {
            for (Long topicId : request.topicIds()) {
                if (!validTopicIds.contains(topicId)) {
                    log.info("Topic validation failed: topicId={} does not belong to trackId={}", topicId, track.getId());
                    throw new BadRequestException("TOPIC_NOT_IN_TRACK", "Topic " + topicId + " does not belong to track " + track.getId());
                }
            }
            targetTopicIds = request.topicIds();
        } else {
            targetTopicIds = new ArrayList<>(validTopicIds);
        }

        // Step 6: Apply difficulty and question count defaults/boundaries
        QuizDifficulty difficulty = request.difficulty() != null ? request.difficulty() : QuizDifficulty.MIXED;
        int requestedCount = request.questionCount() != null ? request.questionCount() : sessionProperties.getDefaultQuestionCount();

        if (requestedCount > sessionProperties.getMaxQuestions()) {
            log.info("Requested question count exceeds configured maximum: count={}, max={}",
                    requestedCount, sessionProperties.getMaxQuestions());
            throw new BadRequestException("INVALID_QUESTION_COUNT", "Question count cannot exceed " + sessionProperties.getMaxQuestions());
        }
        if (requestedCount < 1) {
            log.info("Invalid non-positive question count requested: count={}", requestedCount);
            throw new BadRequestException("INVALID_QUESTION_COUNT", "Question count must be at least 1");
        }

        // Step 7: Search published candidate questions matching target topics and difficulty level.
        // Uses lightweight projection rows (QuestionCandidateRow: id, topicId) to minimize database payload.
        log.info("Querying candidate questions: targetTopicIds={}, difficultyLevel={}", targetTopicIds, difficulty.getLevel());
        List<QuestionCandidateRow> candidateRows = questionRepository.findCandidateQuestions(targetTopicIds, difficulty.getLevel());
        log.info("Found {} candidate question rows across {} topics", candidateRows.size(), targetTopicIds.size());

        // Group candidate question IDs by topicId for fair weighted distribution
        Map<Long, List<Long>> candidatesByTopic = candidateRows.stream()
                .collect(Collectors.groupingBy(
                        QuestionCandidateRow::getTopicId,
                        Collectors.mapping(QuestionCandidateRow::getId, Collectors.toList())
                ));

        // Step 8: Delegate to QuestionSelector to allocate questions across topics using largest remainder method
        List<Long> selectedQuestionIds = questionSelector.select(targetTopicIds, topicWeights, candidatesByTopic, requestedCount);
        if (selectedQuestionIds.isEmpty()) {
            log.info("No published questions available for trackId={} with topicIds={}", track.getId(), targetTopicIds);
            throw new UnprocessableException("NOT_ENOUGH_QUESTIONS", "Not enough published questions available for the selected track and topics");
        }

        log.info("Successfully selected {} questions for session: {}", selectedQuestionIds.size(), selectedQuestionIds);

        // Step 9: Persist the parent PracticeSession entity
        PracticeSession session = new PracticeSession(
                user,
                track,
                request.mode(),
                request.style(),
                difficulty,
                selectedQuestionIds.size()
        );
        session = practiceSessionRepository.save(session);
        log.info("Saved PracticeSession: sessionId={}, userId={}, trackId={}", session.getId(), user.getId(), track.getId());

        // Step 10: Batch-load selected questions and persist SessionQuestion linking records
        // with deterministic 0-indexed positions (0..N-1) to preserve order during the session.
        List<Question> questions = questionRepository.findAllById(selectedQuestionIds);
        Map<Long, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        for (int i = 0; i < selectedQuestionIds.size(); i++) {
            Long questionId = selectedQuestionIds.get(i);
            Question question = questionMap.get(questionId);
            SessionQuestion sessionQuestion = new SessionQuestion(session, question, i);
            sessionQuestionRepository.save(sessionQuestion);
        }

        // Step 11: Eagerly load details and options for Question 1 (position 0).
        // Uses QuestionMapper to map to QuestionView, stripping isCorrect and explanation to prevent answer leakage.
        Long firstQuestionId = selectedQuestionIds.get(0);
        Question firstQuestion = questionRepository.findWithDetailsById(firstQuestionId)
                .orElse(questionMap.get(firstQuestionId));
        QuestionView firstQuestionView = questionMapper.toView(firstQuestion);

        // Step 12: Generate a non-blocking warning if the database had fewer questions than requested
        List<String> warnings = null;
        if (selectedQuestionIds.size() < requestedCount) {
            warnings = List.of("Only " + selectedQuestionIds.size() + " questions were available for the requested count of " + requestedCount);
            log.info("Session created with deficit warning: {}", warnings);
        }

        log.info("Session {} created successfully with first questionId={}", session.getId(), firstQuestion.getId());
        return questionMapper.toSessionResponse(session, firstQuestionView, warnings);
    }

    /**
     * Submits and instantly grades one MCQ answer for an in-progress quiz session.
     *
     * <p>All logic (idempotency, ownership, grading, progression) is handled inside
     * {@link QuizSessionHandler} to keep this class focused on session lifecycle.
     *
     * @param sessionId the target session
     * @param request   the answer payload
     * @return grading result plus next question (null if session is now COMPLETED)
     */
    public AnswerResponse submitAnswer(Long sessionId, AnswerRequest request) {
        log.info("Delegating answer submission to QuizSessionHandler: sessionId={}", sessionId);
        return quizSessionHandler.submitAnswer(sessionId, request);
    }
}
