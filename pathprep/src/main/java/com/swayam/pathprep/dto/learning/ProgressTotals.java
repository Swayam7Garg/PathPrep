package com.swayam.pathprep.dto.learning;

import java.math.BigDecimal;

public record ProgressTotals(
        int sessionsCompleted,
        long questionsAnswered,
        long correctAnswers,
        BigDecimal accuracyPercent,
        long activeMinutes
) {}
