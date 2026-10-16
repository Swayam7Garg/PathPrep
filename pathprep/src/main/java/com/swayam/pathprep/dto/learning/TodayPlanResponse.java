package com.swayam.pathprep.dto.learning;

import java.time.LocalDate;
import java.util.List;

public record TodayPlanResponse(
        LocalDate date,
        String timeZone,
        StreakView streak,
        int dailyGoal,
        int todayQuestionsAnswered,
        ReadinessView readiness,
        long reviewDueCount,
        Long activeSessionId,
        RecommendedFocus recommendedFocus,
        List<PlanItem> items
) {}
