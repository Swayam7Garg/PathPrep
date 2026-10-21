package com.swayam.pathprep.dto.learning.response;

/**
 * Current and longest consecutive daily practice streak information, including at-risk alerts.
 */
public record StreakView(
        int current,
        int longest,
        boolean activeToday,
        boolean atRisk
) {}
