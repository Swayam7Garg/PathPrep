package com.swayam.pathprep.dto.admin.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FlagQuestionRequest(
        @NotBlank(message = "Flag reason must not be blank")
        @Size(max = 500, message = "Flag reason must not exceed 500 characters")
        String reason
) {}
