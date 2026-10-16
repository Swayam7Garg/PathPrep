package com.swayam.pathprep.dto.learning;

import java.util.List;

public record ProgressResponse(
        int range,
        ProgressTotals totals,
        StreakView streak,
        List<DailyProgressView> days,
        List<RecentSessionView> recentSessions
) {}
