package com.swayam.pathprep.event;

import com.swayam.pathprep.config.LearningProperties;
import com.swayam.pathprep.entity.*;
import com.swayam.pathprep.enums.ReviewStatus;
import com.swayam.pathprep.repository.*;
import com.swayam.pathprep.service.learning.MasteryCalculator;
import com.swayam.pathprep.util.AppDay;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class LearningEventListener {

    private final LearningProperties learningProperties;
    private final AppDay appDay;
    private final Clock clock;
    private final SkillMasteryRepository skillMasteryRepository;
    private final ReviewQueueRepository reviewQueueRepository;
    private final DailyActivityRepository dailyActivityRepository;
    private final UserRepository userRepository;
    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final AttemptRepository attemptRepository;

    @EventListener
    public void onAttemptGraded(AttemptGradedEvent event) {
        log.info("Processing AttemptGradedEvent: userId={}, topicId={}, correct={}",
                event.getUserId(), event.getTopicId(), event.isCorrect());

        MasteryCalculator masteryCalculator = new MasteryCalculator(
                learningProperties.getMasteryWindow(),
                learningProperties.getMasteryPriorCorrect(),
                learningProperties.getMasteryPriorAttempts(),
                learningProperties.getMasteredThreshold()
        );

        Instant now = clock.instant();
        LocalDate today = appDay.today();

        // 1. skill_mastery update
        SkillMastery mastery = skillMasteryRepository
                .findByUserIdAndTopicIdWithLock(event.getUserId(), event.getTopicId())
                .orElse(null);

        if (mastery == null) {
            mastery = new SkillMastery();
            User userRef = userRepository.getReferenceById(event.getUserId());
            Topic topicRef = topicRepository.getReferenceById(event.getTopicId());
            mastery.setUser(userRef);
            mastery.setTopic(topicRef);
            mastery.setRecentOutcomes("");
            mastery.setAttemptsCount(0);
            mastery.setCorrectCount(0);
        }

        String updatedOutcomes = masteryCalculator.appendOutcome(mastery.getRecentOutcomes(), event.isCorrect());
        mastery.setRecentOutcomes(updatedOutcomes);
        mastery.setAttemptsCount(mastery.getAttemptsCount() + 1);
        if (event.isCorrect()) {
            mastery.setCorrectCount(mastery.getCorrectCount() + 1);
        }
        mastery.setMasteryScore(masteryCalculator.calculateScore(updatedOutcomes));
        mastery.setConfidenceLevel(masteryCalculator.calculateConfidence(mastery.getAttemptsCount()));
        mastery.setLastPracticedAt(now);
        mastery.setUpdatedAt(now);
        skillMasteryRepository.saveAndFlush(mastery);

        // 2. daily_activity update (atomic native upsert)
        int cappedSeconds = Math.min(event.getTimeSpentSec(), learningProperties.getMaxSecondsPerAnswer());
        dailyActivityRepository.upsertDailyActivity(
                event.getUserId(),
                today,
                1,
                event.isCorrect() ? 1 : 0,
                0,
                0,
                cappedSeconds
        );

        // 3. review_queue update if wrong answer
        if (!event.isCorrect()) {
            ReviewQueueItem item = reviewQueueRepository
                    .findByUserIdAndQuestionId(event.getUserId(), event.getQuestionId())
                    .orElse(null);

            Attempt attemptRef = attemptRepository.getReferenceById(event.getAttemptId());

            if (item == null) {
                item = new ReviewQueueItem();
                item.setUser(userRepository.getReferenceById(event.getUserId()));
                item.setQuestion(questionRepository.getReferenceById(event.getQuestionId()));
                item.setCreatedAt(now);
            }
            item.setLastAttempt(attemptRef);
            item.setDueOn(today.plusDays(1));
            item.setIntervalDays(1);
            item.setRepetitionCount(0);
            item.setStatus(ReviewStatus.PENDING);
            reviewQueueRepository.saveAndFlush(item);
            log.info("ReviewQueueItem queued for wrong answer: userId={}, questionId={}, dueOn={}",
                    event.getUserId(), event.getQuestionId(), item.getDueOn());
        }
    }

    @EventListener
    public void onSessionCompleted(SessionCompletedEvent event) {
        log.info("Processing SessionCompletedEvent in LearningEventListener: userId={}, sessionId={}",
                event.userId(), event.sessionId());

        LocalDate today = appDay.today();
        dailyActivityRepository.upsertDailyActivity(
                event.userId(),
                today,
                0,
                0,
                0,
                1,
                0
        );
    }
}
