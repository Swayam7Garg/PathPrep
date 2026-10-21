package com.swayam.pathprep.dto.learning.response;

import com.swayam.pathprep.dto.response.QuestionView;

import java.time.Instant;

/**
 * Data transfer object representing a flashcard or question currently due for spaced repetition review.
 */
public record DueReviewItem(
        Long reviewId,
        int step,
        int intervalDays,
        Instant lastMissedAt,
        QuestionView question
) {}
