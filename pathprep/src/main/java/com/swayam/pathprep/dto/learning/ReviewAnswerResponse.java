package com.swayam.pathprep.dto.learning;

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
