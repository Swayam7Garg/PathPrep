package com.swayam.pathprep.dto.learning;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for submitting a recall rating / answer for a spaced repetition review flashcard.
 */
public record ReviewAnswerRequest(
        @NotNull(message = "selectedOptionId is required")
        Long selectedOptionId,

        @Min(value = 0, message = "timeSpentSec must be >= 0")
        @Max(value = 3600, message = "timeSpentSec must be <= 3600")
        int timeSpentSec
) {}
