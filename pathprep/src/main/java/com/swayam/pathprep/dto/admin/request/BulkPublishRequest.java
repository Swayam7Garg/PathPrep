package com.swayam.pathprep.dto.admin.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BulkPublishRequest(
        @NotEmpty(message = "questionIds list cannot be empty")
        List<Long> questionIds
) {}
