package com.swayam.pathprep.dto;

import com.swayam.pathprep.enums.SessionStatus;

/**
 * Response payload returned after grading an answer attempt, providing correctness feedback and explanations.
 */
public record AnswerResponse(
        boolean correct,
        long correctOptionId,
        String explanation,
        SessionStatus sessionStatus,
        int currentIndex,
        int totalQuestions,
        QuestionView nextQuestion   // null when session is COMPLETED
) {}
