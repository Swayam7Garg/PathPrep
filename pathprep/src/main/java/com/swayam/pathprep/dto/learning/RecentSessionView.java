package com.swayam.pathprep.dto.learning;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Lightweight summary of a recently completed session displayed in learning dashboards.
 */
public record RecentSessionView(
        Long sessionId,
        String trackName,
        BigDecimal scorePercent,
        Instant completedAt
) {}
