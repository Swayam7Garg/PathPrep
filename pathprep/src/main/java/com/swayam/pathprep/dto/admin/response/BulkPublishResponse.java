package com.swayam.pathprep.dto.admin.response;

import java.util.List;

public record BulkPublishResponse(
        int total,
        int publishedCount,
        int invalidCount,
        List<BulkPublishItemResult> items
) {}
