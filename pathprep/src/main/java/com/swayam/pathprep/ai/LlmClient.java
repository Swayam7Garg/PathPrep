package com.swayam.pathprep.ai;

import java.util.List;
import java.util.Map;

/**
 * Interface abstracting LLM interactions (Groq in production, Mock in test).
 */
public interface LlmClient {

    /**
     * Grades candidate answer against rubric criteria and question key points.
     */
    EvaluationResult evaluateAnswer(EvaluationPrompt prompt);

    /**
     * Generates a single short follow-up question in LIVE interview style.
     */
    String generateFollowUp(FollowUpPrompt prompt);

    record EvaluationPrompt(
            String apiKey,
            String questionBody,
            String questionType,
            List<String> keyPoints,
            String modelAnswer,
            List<CriterionInfo> criteria,
            String candidateAnswer
    ) {}

    record CriterionInfo(
            Long id,
            String name,
            String description,
            int weight
    ) {}

    record EvaluationResult(
            double overallScore,
            String feedback,
            String strengths,
            String improvements,
            Map<Long, CriterionEvaluation> criterionEvaluations,
            int promptTokens,
            int completionTokens,
            String model
    ) {}

    record CriterionEvaluation(
            double score,
            String evidenceQuote,
            String feedback
    ) {}

    record FollowUpPrompt(
            String apiKey,
            String questionBody,
            String candidateAnswer,
            String modelAnswer
    ) {}
}
