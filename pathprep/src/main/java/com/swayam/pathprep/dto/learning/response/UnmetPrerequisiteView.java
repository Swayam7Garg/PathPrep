package com.swayam.pathprep.dto.learning.response;

import java.math.BigDecimal;

/**
 * Represents an unmet foundational topic prerequisite that blocks or hinders progress in an advanced topic.
 */
public record UnmetPrerequisiteView(
        Long topicId,
        String name,
        BigDecimal masteryPercent
) {}
