package com.swayam.pathprep.dto;

import com.swayam.pathprep.enums.CompletionReason;
import com.swayam.pathprep.enums.SessionStatus;

import java.math.BigDecimal;

public record SessionCompleteResponse(
        Long sessionId,
        SessionStatus status,
        CompletionReason completionReason,
        int totalQuestions,
        int answeredCount,
        int correctCount,
        int unansweredCount,
        BigDecimal scorePercent,
        Long elapsedSec,
        int activeTimeSec,
        boolean reportAvailable
) {}
