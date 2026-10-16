package com.swayam.pathprep.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/v1/sessions/{id}/answers}.
 */
public record AnswerRequest(

        @NotNull(message = "selectedOptionId is required")
        Long selectedOptionId,

        @NotBlank(message = "idempotencyKey is required")
        @Size(max = 64, message = "idempotencyKey must not exceed 64 characters")
        String idempotencyKey,

        Integer timeSpentSec
) {
    public AnswerRequest(Long selectedOptionId, String idempotencyKey) {
        this(selectedOptionId, idempotencyKey, null);
    }
}
