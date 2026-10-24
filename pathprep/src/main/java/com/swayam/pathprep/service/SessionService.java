package com.swayam.pathprep.service;

import com.swayam.pathprep.config.SessionProperties;
import com.swayam.pathprep.dto.request.*;
import com.swayam.pathprep.dto.response.*;
import com.swayam.pathprep.entity.*;
import com.swayam.pathprep.enums.CompletionReason;
import com.swayam.pathprep.enums.QuizDifficulty;
import com.swayam.pathprep.enums.SessionMode;
import com.swayam.pathprep.enums.SessionStatus;
import com.swayam.pathprep.exception.*;
import com.swayam.pathprep.mapper.QuestionMapper;
import com.swayam.pathprep.repository.*;
import com.swayam.pathprep.repository.projection.QuestionCandidateRow;
import com.swayam.pathprep.security.CurrentUser;
import com.swayam.pathprep.service.handler.QuizSessionHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

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
    private final AttemptRepository attemptRepository;
    private final TopicRepository topicRepository;
    private final TopicPrereqRepository topicPrereqRepository;
    private final QuestionSelector questionSelector;
    private final QuestionMapper questionMapper;
    private final SessionProperties sessionProperties;
    private final QuizSessionHandler quizSessionHandler;
    private final com.swayam.pathprep.service.handler.InterviewSessionHandler interviewSessionHandler;
    private final SessionFinalizer sessionFinalizer;
    private final Clock clock;

    /**
     * Initializes a new practice quiz session.
     * Enforces the one-active-session rule: rejects if user already has an IN_PROGRESS session.
     */
    @Transactional
    public SessionResponse createSession(SessionCreateRequest request) {
        log.info("Processing session creation request: trackId={}, mode={}, difficulty={}, questionCount={}",
                request.trackId(), request.mode(), request.difficulty(), request.questionCount());

        // INTERVIEW or QUIZ supported
        if (request.mode() == SessionMode.INTERVIEW && request.style() == null) {
            log.info("Defaulting style to LITE for INTERVIEW mode");
        }

        Long userId = currentUser.getUserId();
        log.info("Authenticating session user: userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found: " + userId));

        // Active session check before creating a new one
        practiceSessionRepository.findFirstByUserIdAndStatus(userId, SessionStatus.IN_PROGRESS)
                .ifPresent(active -> {
                    log.warn("User {} already has an active session: {}", userId, active.getId());
                    throw new ActiveSessionExistsException(active.getId());
                });

        RoleTrack track = roleTrackRepository.findById(request.trackId())
                .orElseThrow(() -> new NotFoundException("TRACK_NOT_FOUND", "Track not found: " + request.trackId()));

        List<TrackTopic> trackTopics = trackTopicRepository.findByTrackId(track.getId());
        Set<Long> validTopicIds = trackTopics.stream()
                .map(tt -> tt.getTopic().getId())
                .collect(Collectors.toSet());
        Map<Long, BigDecimal> topicWeights = trackTopics.stream()
                .collect(Collectors.toMap(tt -> tt.getTopic().getId(), TrackTopic::getWeight));

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

        QuizDifficulty difficulty = request.difficulty() != null ? request.difficulty() : QuizDifficulty.MIXED;
        int requestedCount = request.questionCount() != null ? request.questionCount() : sessionProperties.getDefaultQuestionCount();

        if (requestedCount > sessionProperties.getMaxQuestions()) {
            throw new BadRequestException("INVALID_QUESTION_COUNT", "Question count cannot exceed " + sessionProperties.getMaxQuestions());
        }
        if (requestedCount < 1) {
            throw new BadRequestException("INVALID_QUESTION_COUNT", "Question count must be at least 1");
        }

        List<QuestionCandidateRow> candidateRows;
        if (request.mode() == SessionMode.INTERVIEW) {
            candidateRows = questionRepository.findCandidateInterviewQuestions(targetTopicIds, difficulty.getLevel());
        } else {
            candidateRows = questionRepository.findCandidateQuestions(targetTopicIds, difficulty.getLevel());
        }

        Map<Long, List<Long>> candidatesByTopic = candidateRows.stream()
                .collect(Collectors.groupingBy(
                        QuestionCandidateRow::getTopicId,
                        Collectors.mapping(QuestionCandidateRow::getId, Collectors.toList())
                ));

        List<Long> selectedQuestionIds = questionSelector.select(targetTopicIds, topicWeights, candidatesByTopic, requestedCount);
        if (selectedQuestionIds.isEmpty()) {
            throw new UnprocessableException("NOT_ENOUGH_QUESTIONS", "Not enough published questions available for the selected track and topics");
        }

        PracticeSession session = new PracticeSession(
                user,
                track,
                request.mode(),
                request.style(),
                difficulty,
                selectedQuestionIds.size()
        );
        Instant now = clock.instant();
        session.setStartedAt(now);
        session.setCurrentQuestionStartedAt(now);

        session = practiceSessionRepository.save(session);
        practiceSessionRepository.flush();

        List<SessionQuestion> sessionQuestions = new ArrayList<>(selectedQuestionIds.size());
        for (int i = 0; i < selectedQuestionIds.size(); i++) {
            Question questionProxy = questionRepository.getReferenceById(selectedQuestionIds.get(i));
            sessionQuestions.add(new SessionQuestion(session, questionProxy, i));
        }
        sessionQuestionRepository.saveAll(sessionQuestions);

        Long firstQuestionId = selectedQuestionIds.get(0);
        Question firstQuestion = questionRepository.findWithDetailsById(firstQuestionId)
                .orElseThrow(() -> new NotFoundException("QUESTION_NOT_FOUND", "Question not found: " + firstQuestionId));

        QuestionView questionView = questionMapper.toView(firstQuestion);
        List<String> warnings = new ArrayList<>();
        if (selectedQuestionIds.size() < requestedCount) {
            warnings.add("Requested " + requestedCount + " questions, but only " + selectedQuestionIds.size() + " published questions were available");
        }

        return new SessionResponse(
                session.getId(),
                session.getMode(),
                session.getStyle(),
                session.getStatus(),
                session.getTotalQuestions(),
                session.getCurrentIndex(),
                questionView,
                warnings
        );
    }

    /**
     * Submits an MCQ or interview text answer.
     */
    public AnswerResponse submitAnswer(Long sessionId, AnswerRequest request) {
        PracticeSession session = practiceSessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("SESSION_NOT_FOUND", "Session not found: " + sessionId));

        if (session.getMode() == SessionMode.INTERVIEW) {
            return interviewSessionHandler.submitInterviewAnswer(sessionId, request);
        }
        return quizSessionHandler.submitAnswer(sessionId, request);
    }

    /**
     * Retrieves session details for resuming or status display (Phase 5).
     * Read-only, no side effects, no locking.
     */
    @Transactional(readOnly = true)
    public SessionDetailResponse getSessionDetail(Long sessionId) {
        Long userId = currentUser.getUserId();
        PracticeSession session = practiceSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new NotFoundException("SESSION_NOT_FOUND", "Session not found: " + sessionId));

        QuestionView currentQuestion = null;
        if (session.getStatus() == SessionStatus.IN_PROGRESS && session.getCurrentIndex() < session.getTotalQuestions()) {
            SessionQuestionId sqId = new SessionQuestionId(sessionId, session.getCurrentIndex());
            currentQuestion = sessionQuestionRepository.findById(sqId)
                    .map(sq -> questionRepository.findWithDetailsById(sq.getQuestion().getId())
                            .orElse(sq.getQuestion()))
                    .map(questionMapper::toView)
                    .orElse(null);
        }

        boolean completed = session.getStatus() == SessionStatus.COMPLETED || session.getStatus() == SessionStatus.ABANDONED;
        SessionDetailResponse.Progress progress = new SessionDetailResponse.Progress(
                session.getAnsweredCount(),
                session.getCorrectCount(),
                session.getTotalQuestions(),
                completed
        );

        return new SessionDetailResponse(
                session.getId(),
                session.getMode(),
                session.getStyle(),
                session.getTrack().getId(),
                session.getTrack().getName(),
                session.getDifficulty(),
                session.getStatus(),
                session.getCompletionReason(),
                session.getTotalQuestions(),
                session.getCurrentIndex(),
                session.getStartedAt(),
                session.getCompletedAt(),
                session.getCurrentQuestionStartedAt(),
                currentQuestion,
                progress,
                session.getStatus() == SessionStatus.IN_PROGRESS,
                session.getStatus() == SessionStatus.COMPLETED
        );
    }

    /**
     * Completes an active quiz session early or explicitly (Phase 5).
     * Idempotent: returns 200 with same summary on already finished session.
     */
    @Transactional
    public SessionCompleteResponse completeSession(Long sessionId) {
        Long userId = currentUser.getUserId();
        PracticeSession session = practiceSessionRepository.findByIdAndUserIdWithLock(sessionId, userId)
                .orElseThrow(() -> new NotFoundException("SESSION_NOT_FOUND", "Session not found: " + sessionId));

        // Both QUIZ and INTERVIEW can be completed

        // Finalize if not already finalized
        if (session.getStatus() == SessionStatus.IN_PROGRESS) {
            boolean endedEarly = session.getAnsweredCount() < session.getTotalQuestions();
            sessionFinalizer.finalizeSession(session, endedEarly);
        }

        int unansweredCount = Math.max(0, session.getTotalQuestions() - session.getAnsweredCount());
        Long elapsedSec = null;
        if (session.getStartedAt() != null && session.getCompletedAt() != null) {
            elapsedSec = Math.max(0L, Duration.between(session.getStartedAt(), session.getCompletedAt()).toSeconds());
        }
        int activeTimeSec = attemptRepository.sumTimeSpentSecBySessionId(session.getId());

        return new SessionCompleteResponse(
                session.getId(),
                session.getStatus(),
                session.getCompletionReason(),
                session.getTotalQuestions(),
                session.getAnsweredCount(),
                session.getCorrectCount(),
                unansweredCount,
                session.getScorePercent(),
                elapsedSec,
                activeTimeSec,
                session.getStatus() == SessionStatus.COMPLETED
        );
    }

    /**
     * Generates a deterministic diagnostic report for a completed session (Phase 5).
     * Zero AI.
     */
    @Transactional(readOnly = true)
    public SessionReportResponse getSessionReport(Long sessionId) {
        Long userId = currentUser.getUserId();
        PracticeSession session = practiceSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new NotFoundException("SESSION_NOT_FOUND", "Session not found: " + sessionId));

        if (session.getStatus() == SessionStatus.IN_PROGRESS) {
            throw new SessionNotCompletedException(sessionId);
        }
        if (session.getStatus() == SessionStatus.ABANDONED) {
            throw new ReportNotAvailableException(sessionId);
        }

        int totalQuestions = session.getTotalQuestions();
        int answeredCount = session.getAnsweredCount();
        int correctCount = session.getCorrectCount();
        int unansweredCount = Math.max(0, totalQuestions - answeredCount);

        BigDecimal accuracyOfAnsweredPercent = (answeredCount > 0)
                ? BigDecimal.valueOf(correctCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(answeredCount), 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);

        SessionReportResponse.Summary summary = new SessionReportResponse.Summary(
                totalQuestions,
                answeredCount,
                correctCount,
                unansweredCount,
                session.getScorePercent(),
                accuracyOfAnsweredPercent
        );

        Long elapsedSec = null;
        if (session.getStartedAt() != null && session.getCompletedAt() != null) {
            elapsedSec = Math.max(0L, Duration.between(session.getStartedAt(), session.getCompletedAt()).toSeconds());
        }
        int activeTimeSec = attemptRepository.sumTimeSpentSecBySessionId(session.getId());

        BigDecimal previousScorePercent = practiceSessionRepository.findPreviousScorePercent(
                userId, session.getTrack().getId(), session.getId(), session.getCompletedAt()
        ).orElse(null);

        // Load attempts and session questions in batches to avoid N+1
        List<Attempt> attempts = attemptRepository.findBySessionIdOrderByAnsweredAtAsc(sessionId);
        Map<Long, Attempt> attemptByQuestionId = new HashMap<>();
        for (Attempt a : attempts) {
            attemptByQuestionId.put(a.getQuestion().getId(), a);
        }

        List<SessionQuestion> sessionQuestions = sessionQuestionRepository.findByIdSessionIdOrderByPositionAsc(sessionId);
        List<Long> questionIds = sessionQuestions.stream().map(sq -> sq.getQuestion().getId()).toList();
        List<Question> fullQuestions = questionRepository.findWithDetailsByIdIn(questionIds);
        Map<Long, Question> questionMap = fullQuestions.stream().collect(Collectors.toMap(Question::getId, q -> q));

        // Group by topic to build topic performance breakdown
        Map<Long, List<SessionQuestion>> sqByTopic = sessionQuestions.stream()
                .collect(Collectors.groupingBy(sq -> {
                    Question q = questionMap.get(sq.getQuestion().getId());
                    return q != null ? q.getTopic().getId() : sq.getQuestion().getTopic().getId();
                }, LinkedHashMap::new, Collectors.toList()));

        List<SessionReportResponse.TopicBreakdown> topicBreakdowns = new ArrayList<>();
        Map<Long, Integer> wrongCountByTopic = new HashMap<>();

        for (Map.Entry<Long, List<SessionQuestion>> entry : sqByTopic.entrySet()) {
            Long topicId = entry.getKey();
            List<SessionQuestion> sqList = entry.getValue();
            int inTopicCount = sqList.size();
            int ansInTopic = 0;
            int corrInTopic = 0;
            String topicName = "";

            for (SessionQuestion sq : sqList) {
                Question q = questionMap.get(sq.getQuestion().getId());
                if (q != null && topicName.isEmpty()) {
                    topicName = q.getTopic().getName();
                }
                Attempt att = attemptByQuestionId.get(sq.getQuestion().getId());
                if (att != null) {
                    ansInTopic++;
                    if (att.isCorrect()) {
                        corrInTopic++;
                    }
                }
            }

            int wrongInTopic = ansInTopic - corrInTopic;
            wrongCountByTopic.put(topicId, wrongInTopic);

            BigDecimal topicAccuracy = (ansInTopic > 0)
                    ? BigDecimal.valueOf(corrInTopic).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(ansInTopic), 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);

            String label = TopicPerformanceEvaluator.evaluateLabel(ansInTopic, topicAccuracy);

            topicBreakdowns.add(new SessionReportResponse.TopicBreakdown(
                    topicId,
                    topicName,
                    inTopicCount,
                    ansInTopic,
                    corrInTopic,
                    topicAccuracy,
                    label
            ));
        }

        // Build question details
        List<SessionReportResponse.QuestionDetail> questionDetails = new ArrayList<>();
        for (SessionQuestion sq : sessionQuestions) {
            Question q = questionMap.get(sq.getQuestion().getId());
            if (q == null) continue;

            Attempt att = attemptByQuestionId.get(q.getId());
            List<OptionView> optionViews = q.getOptions().stream()
                    .sorted(Comparator.comparingInt(QuestionOption::getPosition))
                    .map(o -> new OptionView(o.getId(), o.getPosition(), o.getText()))
                    .toList();

            if (att != null) {
                QuestionOption correctOpt = q.getOptions().stream()
                        .filter(QuestionOption::isCorrect)
                        .findFirst()
                        .orElse(null);

                Long correctOptionId = correctOpt != null ? correctOpt.getId() : null;
                String explanation = correctOpt != null ? correctOpt.getExplanation() : null;
                Long selectedOptId = att.getSelectedOption() != null ? att.getSelectedOption().getId() : null;
                String selectedOptExplanation = att.getSelectedOption() != null ? att.getSelectedOption().getExplanation() : null;

                questionDetails.add(new SessionReportResponse.QuestionDetail(
                        sq.getPosition(),
                        q.getId(),
                        q.getTopic().getId(),
                        q.getTopic().getName(),
                        q.getBody(),
                        true,
                        att.isCorrect(),
                        selectedOptId,
                        correctOptionId,
                        explanation,
                        selectedOptExplanation,
                        att.getTimeSpentSec(),
                        optionViews
                ));
            } else {
                // UNANSWERED question: do not leak correct answer or explanation
                questionDetails.add(new SessionReportResponse.QuestionDetail(
                        sq.getPosition(),
                        q.getId(),
                        q.getTopic().getId(),
                        q.getTopic().getName(),
                        q.getBody(),
                        false,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        optionViews
                ));
            }
        }

        // Recommendations generation (capped at 3, no duplicates)
        List<SessionReportResponse.Recommendation> recommendations = new ArrayList<>();
        Set<Long> recommendedTopicIds = new HashSet<>();
        Set<Long> sessionTopicIds = sqByTopic.keySet();

        // 1. REVIEW items: topics in session with >= 1 wrong answer
        List<SessionReportResponse.TopicBreakdown> weakTopics = topicBreakdowns.stream()
                .filter(tb -> wrongCountByTopic.getOrDefault(tb.topicId(), 0) > 0)
                .sorted((t1, t2) -> {
                    int w1 = wrongCountByTopic.getOrDefault(t1.topicId(), 0);
                    int w2 = wrongCountByTopic.getOrDefault(t2.topicId(), 0);
                    return Integer.compare(w2, w1);
                })
                .toList();

        for (SessionReportResponse.TopicBreakdown tb : weakTopics) {
            if (recommendations.size() >= 3) break;
            int wrong = wrongCountByTopic.getOrDefault(tb.topicId(), 0);
            recommendations.add(new SessionReportResponse.Recommendation(
                    "REVIEW",
                    tb.topicId(),
                    tb.topicName(),
                    "You missed " + wrong + " of " + tb.answered() + " questions here",
                    5
            ));
            recommendedTopicIds.add(tb.topicId());
        }

        // 2. PRACTICE items:
        // 2a. Prerequisites of REVIEW topics not in this session
        if (recommendations.size() < 3 && !recommendedTopicIds.isEmpty()) {
            List<TopicPrereq> prereqs = topicPrereqRepository.findByTopicIdIn(recommendedTopicIds);
            Set<Long> candidatePrereqIds = prereqs.stream()
                    .map(tp -> tp.getId().getPrereqId())
                    .filter(pid -> !sessionTopicIds.contains(pid) && !recommendedTopicIds.contains(pid))
                    .collect(Collectors.toSet());

            if (!candidatePrereqIds.isEmpty()) {
                List<Topic> prereqTopics = topicRepository.findAllById(candidatePrereqIds);
                Map<Long, String> prereqNames = prereqTopics.stream().collect(Collectors.toMap(Topic::getId, Topic::getName));

                for (TopicPrereq tp : prereqs) {
                    if (recommendations.size() >= 3) break;
                    Long pid = tp.getId().getPrereqId();
                    if (candidatePrereqIds.contains(pid) && !recommendedTopicIds.contains(pid)) {
                        String parentName = weakTopics.stream()
                                .filter(wt -> wt.topicId().equals(tp.getId().getTopicId()))
                                .map(SessionReportResponse.TopicBreakdown::topicName)
                                .findFirst().orElse("previous topic");
                        recommendations.add(new SessionReportResponse.Recommendation(
                                "PRACTICE",
                                pid,
                                prereqNames.getOrDefault(pid, "Topic " + pid),
                                "Prerequisite of " + parentName,
                                5
                        ));
                        recommendedTopicIds.add(pid);
                    }
                }
            }
        }

        // 2b. Track's highest-weight topics not in this session
        if (recommendations.size() < 3) {
            List<TrackTopic> trackTopics = trackTopicRepository.findByTrackId(session.getTrack().getId());
            List<TrackTopic> highestWeightNotVisited = trackTopics.stream()
                    .filter(tt -> !sessionTopicIds.contains(tt.getTopic().getId()) && !recommendedTopicIds.contains(tt.getTopic().getId()))
                    .sorted((tt1, tt2) -> tt2.getWeight().compareTo(tt1.getWeight()))
                    .toList();

            for (TrackTopic tt : highestWeightNotVisited) {
                if (recommendations.size() >= 3) break;
                recommendations.add(new SessionReportResponse.Recommendation(
                        "PRACTICE",
                        tt.getTopic().getId(),
                        tt.getTopic().getName(),
                        "Key topic in " + session.getTrack().getName(),
                        5
                ));
                recommendedTopicIds.add(tt.getTopic().getId());
            }
        }

        return new SessionReportResponse(
                session.getId(),
                session.getTrack().getId(),
                session.getTrack().getName(),
                session.getDifficulty(),
                session.getStatus(),
                session.getCompletionReason(),
                session.getStartedAt(),
                session.getCompletedAt(),
                elapsedSec,
                activeTimeSec,
                summary,
                previousScorePercent,
                topicBreakdowns,
                questionDetails,
                recommendations
        );
    }

    /**
     * Paged session history for the authenticated user (Phase 5).
     */
    @Transactional(readOnly = true)
    public PagedResponse<SessionListItem> getSessionHistory(
            int page,
            int size,
            SessionStatus status,
            SessionMode mode,
            Long trackId
    ) {
        if (page < 0) {
            throw new BadRequestException("INVALID_PARAMETER", "Page must be greater than or equal to 0");
        }
        if (size < 1 || size > 50) {
            throw new BadRequestException("INVALID_PARAMETER", "Page size must be between 1 and 50");
        }

        Long userId = currentUser.getUserId();
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("startedAt"), Sort.Order.desc("id")));
        Page<PracticeSession> sessionPage = practiceSessionRepository.findHistory(userId, status, mode, trackId, pageable);

        List<SessionListItem> items = sessionPage.getContent().stream()
                .map(s -> new SessionListItem(
                        s.getId(),
                        s.getMode(),
                        s.getStatus(),
                        s.getTrack().getId(),
                        s.getTrack().getName(),
                        s.getDifficulty(),
                        s.getTotalQuestions(),
                        s.getAnsweredCount(),
                        s.getCorrectCount(),
                        s.getScorePercent(),
                        s.getStartedAt(),
                        s.getCompletedAt(),
                        s.getStatus() == SessionStatus.IN_PROGRESS
                ))
                .toList();

        return new PagedResponse<>(
                items,
                sessionPage.getNumber(),
                sessionPage.getSize(),
                sessionPage.getTotalElements(),
                sessionPage.getTotalPages()
        );
    }
}
