package com.swayam.pathprep.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for submitting an answer choice and time spent for a question in an active session.
 */
public record AnswerRequest(
        Long selectedOptionId,

        String answerText,

        @NotBlank(message = "idempotencyKey is required")
        @Size(max = 64, message = "idempotencyKey must not exceed 64 characters")
        String idempotencyKey,

        Integer timeSpentSec,

        String turnId
) {
    public AnswerRequest(Long selectedOptionId, String idempotencyKey) {
        this(selectedOptionId, null, idempotencyKey, null, null);
    }

    public AnswerRequest(Long selectedOptionId, String answerText, String idempotencyKey, Integer timeSpentSec) {
        this(selectedOptionId, answerText, idempotencyKey, timeSpentSec, null);
    }
}
