package com.swayam.pathprep.dto.learning;

import java.math.BigDecimal;
import java.time.Instant;

public record RecentSessionView(
        Long sessionId,
        String trackName,
        BigDecimal scorePercent,
        Instant completedAt
) {}
