package com.swayam.pathprep.service;

import com.swayam.pathprep.dto.learning.*;
import com.swayam.pathprep.entity.DailyActivity;
import com.swayam.pathprep.entity.PracticeSession;
import com.swayam.pathprep.enums.SessionStatus;
import com.swayam.pathprep.exception.BadRequestException;
import com.swayam.pathprep.repository.DailyActivityRepository;
import com.swayam.pathprep.repository.PracticeSessionRepository;
import com.swayam.pathprep.security.CurrentUser;
import com.swayam.pathprep.service.learning.StreakCalculator;
import com.swayam.pathprep.util.AppDay;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressService {

    private final CurrentUser currentUser;
    private final AppDay appDay;
    private final DailyActivityRepository dailyActivityRepository;
    private final PracticeSessionRepository practiceSessionRepository;

    @Transactional(readOnly = true)
    public ProgressResponse getProgress(Integer rangeParam) {
        int range = (rangeParam == null) ? 30 : rangeParam;
        if (range != 7 && range != 30 && range != 90) {
            throw new BadRequestException("INVALID_PARAMETER", "Range must be 7, 30, or 90");
        }

        Long userId = currentUser.getUserId();
        LocalDate today = appDay.today();

        int sessionsCompleted = practiceSessionRepository.countByUserIdAndStatus(userId, SessionStatus.COMPLETED);
        long questionsAnswered = dailyActivityRepository.sumAllTimeQuestionsAnswered(userId);
        long correctAnswers = dailyActivityRepository.sumAllTimeCorrectAnswers(userId);
        long activeSeconds = dailyActivityRepository.sumAllTimeActiveSeconds(userId);

        BigDecimal accuracyPercent = (questionsAnswered > 0)
                ? BigDecimal.valueOf(correctAnswers)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(questionsAnswered), 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);

        long activeMinutes = Math.round(activeSeconds / 60.0);
        ProgressTotals totals = new ProgressTotals(
                sessionsCompleted,
                questionsAnswered,
                correctAnswers,
                accuracyPercent,
                activeMinutes
        );

        List<LocalDate> activeDates = dailyActivityRepository.findActiveDatesInPastYear(userId, today.minusDays(364));
        StreakCalculator.StreakResult streakRes = new StreakCalculator().calculate(activeDates, today);
        StreakView streak = new StreakView(streakRes.current(), streakRes.longest(), streakRes.activeToday(), streakRes.atRisk());

        LocalDate startDate = today.minusDays(range - 1);
        List<DailyActivity> activities = dailyActivityRepository.findByUserIdAndActivityDateGreaterThanEqual(userId, startDate);
        Map<LocalDate, DailyActivity> activityMap = activities.stream()
                .collect(Collectors.toMap(DailyActivity::getActivityDate, Function.identity()));

        List<DailyProgressView> days = new ArrayList<>(range);
        for (LocalDate d = startDate; !d.isAfter(today); d = d.plusDays(1)) {
            DailyActivity act = activityMap.get(d);
            if (act != null) {
                int dayActiveMinutes = (int) Math.round(act.getActiveSeconds() / 60.0);
                days.add(new DailyProgressView(
                        d,
                        act.getQuestionsAnswered(),
                        act.getCorrectAnswers(),
                        act.getReviewsAnswered(),
                        act.getSessionsCompleted(),
                        dayActiveMinutes
                ));
            } else {
                days.add(new DailyProgressView(d, 0, 0, 0, 0, 0));
            }
        }

        List<PracticeSession> recentSessionsEntities = practiceSessionRepository.findRecentSessionsByUserIdAndStatus(
                userId, SessionStatus.COMPLETED, PageRequest.of(0, 10));

        List<RecentSessionView> recentSessions = recentSessionsEntities.stream()
                .map(s -> new RecentSessionView(
                        s.getId(),
                        s.getTrack().getName(),
                        s.getScorePercent(),
                        s.getCompletedAt()
                ))
                .toList();

        return new ProgressResponse(range, totals, streak, days, recentSessions);
    }
}
