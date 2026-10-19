package com.swayam.pathprep.dto.learning;

import java.util.List;

/**
 * Response payload providing longitudinal telemetry, weekly activity graphs, and total practice metrics.
 */
public record ProgressResponse(
        int range,
        ProgressTotals totals,
        StreakView streak,
        List<DailyProgressView> days,
        List<RecentSessionView> recentSessions
) {}
