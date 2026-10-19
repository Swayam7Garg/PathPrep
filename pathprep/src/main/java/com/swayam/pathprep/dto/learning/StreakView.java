package com.swayam.pathprep.dto.learning;

/**
 * Current and longest consecutive daily practice streak information, including at-risk alerts.
 */
public record StreakView(
        int current,
        int longest,
        boolean activeToday,
        boolean atRisk
) {}
