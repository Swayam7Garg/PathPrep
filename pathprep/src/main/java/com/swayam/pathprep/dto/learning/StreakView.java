package com.swayam.pathprep.dto.learning;

public record StreakView(
        int current,
        int longest,
        boolean activeToday,
        boolean atRisk
) {}
