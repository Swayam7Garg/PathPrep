package com.swayam.pathprep.dto.learning;

/**
 * Response returned after submitting a review, detailing updated SM-2 interval, next due date, and score.
 */
public record ReviewAnswerResponse(
        Long reviewId,
        boolean correct,
        Long selectedOptionId,
        Long correctOptionId,
        String explanation,
        String selectedOptionExplanation,
        ReviewScheduleView schedule,
        long dueRemaining
) {}
