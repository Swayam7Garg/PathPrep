package com.swayam.pathprep.dto.learning;

import com.swayam.pathprep.enums.PlanItemType;

public record PlanItem(
        PlanItemType type,
        String title,
        String detail,
        int estimatedMinutes,
        Long topicId,
        Long sessionId,
        Integer questionCount
) {}
