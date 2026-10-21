package com.swayam.pathprep.dto.response;

import java.time.Instant;

public record QuestionReportResponse(
        Long id,
        Long userId,
        String userEmail,
        Long questionId,
        String questionBody,
        String reason,
        Instant createdAt
) {}
