package com.swayam.pathprep.dto;

import com.swayam.pathprep.enums.SessionMode;
import com.swayam.pathprep.enums.SessionStatus;
import com.swayam.pathprep.enums.SessionStyle;

import java.util.List;

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
