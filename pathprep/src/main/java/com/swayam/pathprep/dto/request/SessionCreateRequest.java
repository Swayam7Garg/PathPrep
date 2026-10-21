package com.swayam.pathprep.dto.request;

import com.swayam.pathprep.enums.QuizDifficulty;
import com.swayam.pathprep.enums.SessionMode;
import com.swayam.pathprep.enums.SessionStyle;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request payload for configuring and starting a new practice session (track, mode, topics, difficulty, count).
 */
public record SessionCreateRequest(
    @NotNull(message = "Mode is required")
    SessionMode mode,

    SessionStyle style,

    @NotNull(message = "Track ID is required")
    Long trackId,

    List<Long> topicIds,

    @Min(value = 1, message = "Question count must be at least 1")
    Integer questionCount,

    QuizDifficulty difficulty
) {}
