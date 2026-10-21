package com.swayam.pathprep.dto.admin.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminQuestionOptionDto(
        Long id,
        int position,
        @NotBlank(message = "Option text must not be blank")
        @Size(max = 500, message = "Option text must not exceed 500 characters")
        String text,
        @JsonProperty("isCorrect")
        boolean isCorrect,
        String explanation
) {}
