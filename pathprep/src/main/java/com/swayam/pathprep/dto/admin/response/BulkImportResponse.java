package com.swayam.pathprep.dto.admin.response;

import java.util.List;

public record BulkImportResponse(
        String batch,
        int totalProcessed,
        int importedCount,
        int skippedDuplicatesCount,
        int invalidCount,
        List<ImportItemResult> items
) {}
