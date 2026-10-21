package com.swayam.pathprep.dto.learning.response;

import java.math.BigDecimal;

/**
 * Placement readiness index score and target benchmark breakdown for a career track.
 */
public record ReadinessView(
        BigDecimal percent,
        String reason,
        int basedOnQuestions,
        int minAnswersRequired
) {}
