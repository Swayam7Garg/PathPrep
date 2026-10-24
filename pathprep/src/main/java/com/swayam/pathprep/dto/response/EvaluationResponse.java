package com.swayam.pathprep.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record EvaluationResponse(
        Long id,
        Long attemptId,
        Long questionId,
        String questionBody,
        String answerText,
        String status,
        BigDecimal overallScore,
        String feedback,
        String strengths,
        String improvements,
        int tokensUsed,
        List<CriterionScoreDto> criteria,
        Instant evaluatedAt
) {
    public record CriterionScoreDto(
            Long criterionId,
            String criterionName,
            BigDecimal score,
            String evidenceQuote,
            String feedback
    ) {}
}
