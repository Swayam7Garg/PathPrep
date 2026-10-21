package com.swayam.pathprep.dto.admin.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BulkImportRequest(
        @Size(max = 64, message = "Batch name cannot exceed 64 characters")
        String batch,

        Boolean dryRun,

        @NotEmpty(message = "Questions list cannot be empty")
        @Valid
        List<ImportQuestionItem> questions
) {
    public boolean isDryRun() {
        return dryRun == null || dryRun;
    }
}
