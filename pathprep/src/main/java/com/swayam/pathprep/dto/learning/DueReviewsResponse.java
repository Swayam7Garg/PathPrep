package com.swayam.pathprep.dto.learning;

import java.util.List;

public record DueReviewsResponse(
        long dueCount,
        List<DueReviewItem> items
) {}
