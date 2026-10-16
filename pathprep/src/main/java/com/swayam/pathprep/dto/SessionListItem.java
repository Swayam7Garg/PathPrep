package com.swayam.pathprep.dto;

import com.swayam.pathprep.enums.QuizDifficulty;
import com.swayam.pathprep.enums.SessionMode;
import com.swayam.pathprep.enums.SessionStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record SessionListItem(
        Long id,
        SessionMode mode,
        SessionStatus status,
        Long trackId,
        String trackName,
        QuizDifficulty difficulty,
        int totalQuestions,
        int answeredCount,
        int correctCount,
        BigDecimal scorePercent,
        Instant startedAt,
        Instant completedAt,
        boolean resumable
) {}
