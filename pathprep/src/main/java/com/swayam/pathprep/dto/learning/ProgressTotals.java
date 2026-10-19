package com.swayam.pathprep.dto.learning;

import java.math.BigDecimal;

/**
 * Cumulative learning telemetry aggregating total questions solved, total time invested, and average accuracy.
 */
public record ProgressTotals(
        int sessionsCompleted,
        long questionsAnswered,
        long correctAnswers,
        BigDecimal accuracyPercent,
        long activeMinutes
) {}
