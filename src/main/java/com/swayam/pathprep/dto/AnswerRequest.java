package com.swayam.pathprep.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/v1/sessions/{id}/answers}.
 *
 * @param selectedOptionId the ID of the MCQ option the student chose
 * @param idempotencyKey   client-generated UUID that deduplicates retried requests;
 *                         the server returns the cached result if this key was already processed
 */
public record AnswerRequest(

        @NotNull(message = "selectedOptionId is required")
        Long selectedOptionId,

        @NotBlank(message = "idempotencyKey is required")
        @Size(max = 64, message = "idempotencyKey must not exceed 64 characters")
        String idempotencyKey
) {}
