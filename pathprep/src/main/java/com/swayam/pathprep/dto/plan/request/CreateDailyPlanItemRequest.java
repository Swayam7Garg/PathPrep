package com.swayam.pathprep.dto.plan.request;

import com.swayam.pathprep.enums.DailyPlanItemType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateDailyPlanItemRequest(
        @NotNull(message = "Item type is required")
        DailyPlanItemType itemType,

        @Size(max = 255, message = "Title cannot exceed 255 characters")
        String title,

        Long trackId,

        Long topicId,

        @Min(value = 1, message = "Question count must be at least 1")
        @Max(value = 20, message = "Question count cannot exceed 20")
        Integer questionCount
) {
}
