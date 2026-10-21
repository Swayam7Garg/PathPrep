package com.swayam.pathprep.dto.response;

import com.swayam.pathprep.enums.SessionMode;
import com.swayam.pathprep.enums.SessionStatus;
import com.swayam.pathprep.enums.SessionStyle;

import java.util.List;

/**
 * Core response payload returned upon session creation containing session ID, mode, and assigned questions.
 */
public record SessionResponse(
    long id,
    SessionMode mode,
    SessionStyle style,
    SessionStatus status,
    int totalQuestions,
    int currentIndex,
    QuestionView currentQuestion,
    List<String> warnings
) {}
