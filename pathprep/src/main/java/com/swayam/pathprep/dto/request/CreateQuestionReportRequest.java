package com.swayam.pathprep.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateQuestionReportRequest(
        @NotBlank(message = "Reason must not be blank")
        @Size(max = 500, message = "Reason cannot exceed 500 characters")
        String reason
) {}
