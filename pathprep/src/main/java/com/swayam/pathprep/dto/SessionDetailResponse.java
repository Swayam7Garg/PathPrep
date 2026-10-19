package com.swayam.pathprep.dto;

import com.swayam.pathprep.enums.CompletionReason;
import com.swayam.pathprep.enums.QuizDifficulty;
import com.swayam.pathprep.enums.SessionMode;
import com.swayam.pathprep.enums.SessionStatus;
import com.swayam.pathprep.enums.SessionStyle;

import java.time.Instant;

/**
 * Detailed response payload representing an active or completed practice session and its questions.
 */
public record SessionDetailResponse(
        Long id,
        SessionMode mode,
        SessionStyle style,
        Long trackId,
        String trackName,
        QuizDifficulty difficulty,
        SessionStatus status,
        CompletionReason completionReason,
        int totalQuestions,
        int currentIndex,
        Instant startedAt,
        Instant completedAt,
        Instant currentQuestionStartedAt,
        QuestionView currentQuestion,
        Progress progress,
        boolean resumable,
        boolean reportAvailable
) {
    public record Progress(
            int answeredCount,
            int correctCount,
            int totalQuestions,
            boolean completed
    ) {}
}
