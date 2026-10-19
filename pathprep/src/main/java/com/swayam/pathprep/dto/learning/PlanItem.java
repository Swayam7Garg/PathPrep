package com.swayam.pathprep.dto.learning;

import com.swayam.pathprep.enums.PlanItemType;

/**
 * Action item in a student's personalized daily learning plan (e.g., due review, recommended drill).
 */
public record PlanItem(
        PlanItemType type,
        String title,
        String detail,
        int estimatedMinutes,
        Long topicId,
        Long sessionId,
        Integer questionCount
) {}
