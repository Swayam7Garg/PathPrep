package com.swayam.pathprep.dto.learning;

import java.time.LocalDate;

/**
 * Summary of upcoming spaced repetition intervals and schedule projections for mastered questions.
 */
public record ReviewScheduleView(
        int step,
        Integer intervalDays,
        LocalDate nextDueOn,
        boolean graduated
) {}
