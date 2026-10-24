package com.swayam.pathprep.dto.plan.response;

import com.swayam.pathprep.enums.DailyPlanItemType;

import java.time.Instant;
import java.time.LocalDate;

public record DailyPlanItemResponse(
        Long id,
        LocalDate planDate,
        String title,
        DailyPlanItemType itemType,
        Long trackId,
        String trackName,
        Long topicId,
        String topicName,
        Integer questionCount,
        Long sessionId,
        boolean completed,
        Instant completedAt,
        int sortOrder
) {
}
