package com.swayam.pathprep.dto.admin.response;

import java.util.List;

public record BulkPublishItemResult(
        Long questionId,
        String status, // "PUBLISHED", "INVALID"
        List<String> errors
) {
    public static BulkPublishItemResult published(Long questionId) {
        return new BulkPublishItemResult(questionId, "PUBLISHED", List.of());
    }

    public static BulkPublishItemResult invalid(Long questionId, List<String> errors) {
        return new BulkPublishItemResult(questionId, "INVALID", errors);
    }
}
