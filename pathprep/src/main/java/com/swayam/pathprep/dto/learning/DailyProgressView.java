package com.swayam.pathprep.dto.learning;

import java.time.LocalDate;

public record DailyProgressView(
        LocalDate date,
        int questionsAnswered,
        int correctAnswers,
        int reviewsAnswered,
        int sessionsCompleted,
        int activeMinutes
) {}
