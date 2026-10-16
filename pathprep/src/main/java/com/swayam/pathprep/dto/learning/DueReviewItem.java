package com.swayam.pathprep.dto.learning;

import com.swayam.pathprep.dto.QuestionView;

import java.time.Instant;

public record DueReviewItem(
        Long reviewId,
        int step,
        int intervalDays,
        Instant lastMissedAt,
        QuestionView question
) {}
