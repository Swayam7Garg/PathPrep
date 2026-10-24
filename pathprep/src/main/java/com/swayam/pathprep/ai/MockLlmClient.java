package com.swayam.pathprep.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Mock LlmClient implementation for unit tests, integration tests, and offline mode.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "MOCK", matchIfMissing = true)
public class MockLlmClient implements LlmClient {

    @Override
    public EvaluationResult evaluateAnswer(EvaluationPrompt prompt) {
        log.info("MockLlmClient: evaluating answer for question='{}'", prompt.questionBody());

        Map<Long, CriterionEvaluation> criterionMap = new HashMap<>();
        double totalWeighted = 0;
        int totalWeight = 0;

        for (CriterionInfo c : prompt.criteria()) {
            // Predictable mock score: 85.0
            double score = 85.0;
            totalWeighted += score * c.weight();
            totalWeight += c.weight();
            criterionMap.put(c.id(), new CriterionEvaluation(
                    score,
                    "Candidate mentioned key components accurately.",
                    "Good understanding of " + c.name()
            ));
        }

        double overall = totalWeight > 0 ? (totalWeighted / totalWeight) : 85.0;

        return new EvaluationResult(
                overall,
                "Strong foundational answer with clear grasp of the core concepts.",
                "Clear structure, addressed key points directly.",
                "Could provide deeper concrete production examples or edge cases.",
                criterionMap,
                450,
                150,
                "mock-llama-3"
        );
    }

    @Override
    public String generateFollowUp(FollowUpPrompt prompt) {
        log.info("MockLlmClient: generating follow up question");
        return "How would you handle failure scenarios or race conditions in that approach?";
    }
}
