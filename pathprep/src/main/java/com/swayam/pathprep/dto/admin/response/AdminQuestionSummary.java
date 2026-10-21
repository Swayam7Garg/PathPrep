package com.swayam.pathprep.dto.admin.response;

import com.swayam.pathprep.enums.QuestionSource;
import com.swayam.pathprep.enums.QuestionStatus;
import com.swayam.pathprep.enums.QuestionType;

import java.time.Instant;

public record AdminQuestionSummary(
        Long id,
        Long topicId,
        String topicName,
        QuestionType type,
        int difficulty,
        String body,
        QuestionStatus status,
        QuestionSource source,
        String subtopic,
        int optionsCount,
        boolean isQuizEligible,
        Instant createdAt,
        Instant updatedAt
) {}
