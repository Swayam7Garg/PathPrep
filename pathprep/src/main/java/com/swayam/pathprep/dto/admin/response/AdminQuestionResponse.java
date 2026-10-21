package com.swayam.pathprep.dto.admin.response;

import com.swayam.pathprep.enums.QuestionSource;
import com.swayam.pathprep.enums.QuestionStatus;
import com.swayam.pathprep.enums.QuestionType;

import java.time.Instant;
import java.util.List;

public record AdminQuestionResponse(
        Long id,
        Long topicId,
        String topicName,
        QuestionType type,
        int difficulty,
        String body,
        String keyPoints,
        String modelAnswer,
        QuestionStatus status,
        QuestionSource source,
        String subtopic,
        String contentHash,
        String importBatch,
        String flagReason,
        Instant reviewedAt,
        List<AdminQuestionOptionDto> options,
        boolean isQuizEligible,
        Instant createdAt,
        Instant updatedAt
) {}
