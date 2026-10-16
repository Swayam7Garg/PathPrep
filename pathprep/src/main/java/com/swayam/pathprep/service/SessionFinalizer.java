package com.swayam.pathprep.service;

import com.swayam.pathprep.entity.Attempt;
import com.swayam.pathprep.entity.PracticeSession;
import com.swayam.pathprep.enums.CompletionReason;
import com.swayam.pathprep.enums.SessionStatus;
import com.swayam.pathprep.event.SessionCompletedEvent;
import com.swayam.pathprep.repository.AttemptRepository;
import com.swayam.pathprep.repository.PracticeSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionFinalizer {

    private final Clock clock;
    private final AttemptRepository attemptRepository;
    private final PracticeSessionRepository practiceSessionRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Finalizes a session row that is currently locked with PESSIMISTIC_WRITE.
     * Idempotent: does nothing if session is already COMPLETED or ABANDONED.
     */
    public void finalizeSession(PracticeSession session, boolean endedEarly) {
        if (session.getStatus() == SessionStatus.COMPLETED || session.getStatus() == SessionStatus.ABANDONED) {
            log.info("Session already finalized: sessionId={}, status={}", session.getId(), session.getStatus());
            return;
        }

        long answeredCount = attemptRepository.countBySessionId(session.getId());
        long correctCount = attemptRepository.countBySessionIdAndCorrectTrue(session.getId());

        session.setAnsweredCount((int) answeredCount);
        session.setCorrectCount((int) correctCount);
        session.setCompletedAt(clock.instant());

        if (answeredCount == 0) {
            session.setStatus(SessionStatus.ABANDONED);
            session.setCompletionReason(CompletionReason.ENDED_EARLY);
            session.setScorePercent(null);
            practiceSessionRepository.save(session);
            log.info("Session abandoned with 0 answers: sessionId={}", session.getId());
            return;
        }

        session.setStatus(SessionStatus.COMPLETED);
        if (!endedEarly && answeredCount == session.getTotalQuestions()) {
            session.setCompletionReason(CompletionReason.FINISHED);
        } else {
            session.setCompletionReason(CompletionReason.ENDED_EARLY);
        }

        if (session.getTotalQuestions() > 0) {
            BigDecimal score = BigDecimal.valueOf(correctCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(session.getTotalQuestions()), 1, RoundingMode.HALF_UP);
            session.setScorePercent(score);
        } else {
            session.setScorePercent(BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP));
        }

        practiceSessionRepository.save(session);
        log.info("Session finalized: sessionId={}, status={}, completionReason={}, scorePercent={}",
                session.getId(), session.getStatus(), session.getCompletionReason(), session.getScorePercent());

        // Publish SessionCompletedEvent for COMPLETED sessions
        List<Attempt> attempts = attemptRepository.findBySessionId(session.getId());
        Map<Long, int[]> topicStats = new LinkedHashMap<>();
        for (Attempt a : attempts) {
            Long topicId = a.getQuestion().getTopic().getId();
            topicStats.computeIfAbsent(topicId, k -> new int[2]);
            topicStats.get(topicId)[0]++;
            if (a.isCorrect()) {
                topicStats.get(topicId)[1]++;
            }
        }
        List<SessionCompletedEvent.TopicResult> perTopicResults = topicStats.entrySet().stream()
                .map(e -> new SessionCompletedEvent.TopicResult(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();

        eventPublisher.publishEvent(new SessionCompletedEvent(
                session.getUser().getId(),
                session.getId(),
                session.getTrack().getId(),
                session.getCompletedAt(),
                perTopicResults
        ));
    }
}
