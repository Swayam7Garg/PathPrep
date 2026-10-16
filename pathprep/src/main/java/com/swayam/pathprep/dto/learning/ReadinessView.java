package com.swayam.pathprep.dto.learning;

import java.math.BigDecimal;

public record ReadinessView(
        BigDecimal percent,
        String reason,
        int basedOnQuestions
) {}
