package com.swayam.pathprep.dto.learning;

import java.util.List;

/**
 * Response payload containing the list of flashcards and questions due for spaced repetition review today.
 */
public record DueReviewsResponse(
        long dueCount,
        List<DueReviewItem> items
) {}
