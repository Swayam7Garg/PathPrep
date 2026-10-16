package com.swayam.pathprep.service;

import com.swayam.pathprep.config.LearningProperties;
import com.swayam.pathprep.dto.learning.*;
import com.swayam.pathprep.entity.PracticeSession;
import com.swayam.pathprep.entity.RoleTrack;
import com.swayam.pathprep.entity.SkillMastery;
import com.swayam.pathprep.enums.PlanItemType;
import com.swayam.pathprep.enums.SessionStatus;
import com.swayam.pathprep.repository.*;
import com.swayam.pathprep.repository.projection.TopicRow;
import com.swayam.pathprep.security.CurrentUser;
import com.swayam.pathprep.service.learning.ReadinessCalculator;
import com.swayam.pathprep.service.learning.StreakCalculator;
import com.swayam.pathprep.util.AppDay;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanService {

    private final CurrentUser currentUser;
    private final AppDay appDay;
    private final LearningProperties learningProperties;
    private final DailyActivityRepository dailyActivityRepository;
    private final PracticeSessionRepository practiceSessionRepository;
    private final ReviewQueueRepository reviewQueueRepository;
    private final SkillMasteryRepository skillMasteryRepository;
    private final TopicRepository topicRepository;
    private final SkillService skillService;

    @Transactional(readOnly = true)
    public TodayPlanResponse getTodayPlan() {
        Long userId = currentUser.getUserId();
        LocalDate today = appDay.today();

        // 1. Streak
        List<LocalDate> activeDates = dailyActivityRepository.findActiveDatesInPastYear(userId, today.minusDays(364));
        StreakCalculator.StreakResult streakRes = new StreakCalculator().calculate(activeDates, today);
        StreakView streakView = new StreakView(streakRes.current(), streakRes.longest(), streakRes.activeToday(), streakRes.atRisk());

        // 2. Today questions answered (quiz answers only)
        int todayQuestions = dailyActivityRepository.findQuestionsAnsweredOnDate(userId, today).orElse(0);

        // 3. Due review questions
        long reviewDueCount = reviewQueueRepository.countDueByUserId(userId, today);

        // 4. In-progress session
        PracticeSession activeSession = practiceSessionRepository
                .findFirstByUserIdAndStatus(userId, SessionStatus.IN_PROGRESS)
                .orElse(null);
        Long activeSessionId = (activeSession != null) ? activeSession.getId() : null;

        // 5. Readiness and Focus
        RoleTrack track = skillService.resolveTrack(userId, null);
        ReadinessView readinessView;
        RecommendedFocus recommendedFocus = null;
        SkillGapView topGap = null;

        if (track != null) {
            List<TopicRow> topics = topicRepository.findTopicsForTrack(track.getId());
            List<Long> topicIds = topics.stream().map(TopicRow::getId).toList();
            Map<Long, SkillMastery> masteryMap = skillMasteryRepository.findByUserIdAndTopicIdIn(userId, topicIds)
                    .stream()
                    .collect(Collectors.toMap(sm -> sm.getTopic().getId(), sm -> sm));

            List<ReadinessCalculator.TopicWeightAndMastery> readinessTopics = topics.stream()
                    .map(t -> {
                        SkillMastery sm = masteryMap.get(t.getId());
                        BigDecimal m = (sm != null) ? sm.getMasteryScore() : null;
                        return new ReadinessCalculator.TopicWeightAndMastery(t.getWeight(), m, (int) t.getQuestionCount());
                    })
                    .toList();

            int totalQuestionsAnswered = skillMasteryRepository.sumAttemptsCountByUserId(userId);
            long totalEverQueued = reviewQueueRepository.countAllEverQueuedByUserId(userId);
            long graduated = reviewQueueRepository.countGraduatedByUserId(userId);

            ReadinessCalculator.ReadinessResult readinessRes = new ReadinessCalculator(
                    learningProperties.getMinAnswersForReadiness(),
                    learningProperties.getMinMistakesForClearance()
            ).calculate(totalQuestionsAnswered, readinessTopics, graduated, totalEverQueued);

            readinessView = new ReadinessView(readinessRes.percent(), readinessRes.reason(), readinessRes.basedOnQuestions());

            SkillGapsResponse gaps = skillService.getSkillGaps(track.getId(), 1);
            if (!gaps.gaps().isEmpty()) {
                topGap = gaps.gaps().get(0);
                recommendedFocus = new RecommendedFocus(topGap.topicId(), topGap.topicName(), topGap.kind(), topGap.reason());
            }
        } else {
            readinessView = new ReadinessView(null, "NOT_ENOUGH_DATA", 0);
        }

        // 6. Plan Items (Maximum 3 items)
        List<PlanItem> items = new ArrayList<>();

        if (activeSession != null) {
            int remaining = Math.max(1, activeSession.getTotalQuestions() - activeSession.getAnsweredCount());
            items.add(new PlanItem(
                    PlanItemType.RESUME,
                    "Resume in-progress quiz",
                    "Continue quiz in " + activeSession.getTrack().getName(),
                    remaining,
                    null,
                    activeSession.getId(),
                    remaining
            ));
        }

        if (reviewDueCount > 0 && items.size() < 3) {
            int estMinutes = (int) Math.ceil(reviewDueCount / 2.0);
            items.add(new PlanItem(
                    PlanItemType.REVIEW,
                    "Review mistakes",
                    reviewDueCount + " questions due for spaced repetition",
                    estMinutes,
                    null,
                    null,
                    (int) reviewDueCount
            ));
        }

        if (topGap != null && items.size() < 3) {
            items.add(new PlanItem(
                    PlanItemType.DRILL,
                    "Practice " + topGap.topicName(),
                    "Strengthen " + topGap.topicName() + " with a short 5-question drill",
                    5,
                    topGap.topicId(),
                    null,
                    5
            ));
        }

        return new TodayPlanResponse(
                today,
                appDay.getZoneId().getId(),
                streakView,
                learningProperties.getDailyGoal(),
                todayQuestions,
                readinessView,
                reviewDueCount,
                activeSessionId,
                recommendedFocus,
                items
        );
    }
}
