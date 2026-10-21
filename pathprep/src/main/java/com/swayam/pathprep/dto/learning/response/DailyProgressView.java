package com.swayam.pathprep.dto.learning.response;

import java.time.LocalDate;

/**
 * Data transfer object capturing a single day's practice activity (questions answered, accuracy, date).
 */
public record DailyProgressView(
        LocalDate date,
        int questionsAnswered,
        int correctAnswers,
        int reviewsAnswered,
        int sessionsCompleted,
        int activeMinutes
) {}
