package com.swayam.pathprep.dto.learning;

import java.math.BigDecimal;

public record UnmetPrerequisiteView(
        Long topicId,
        String name,
        BigDecimal masteryPercent
) {}
