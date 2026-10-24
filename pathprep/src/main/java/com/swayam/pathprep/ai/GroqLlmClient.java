package com.swayam.pathprep.ai;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Production Groq LlmClient using OpenAI-compatible Chat Completions API with structured JSON output.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "GROQ")
@RequiredArgsConstructor
public class GroqLlmClient implements LlmClient {

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    @Override
    public EvaluationResult evaluateAnswer(EvaluationPrompt prompt) {
        String apiKey = resolveApiKey(prompt.apiKey());

        StringBuilder promptText = new StringBuilder();
        promptText.append("You are an expert technical interviewer evaluating a student candidate's answer.\n");
        promptText.append("QUESTION:\n").append(prompt.questionBody()).append("\n\n");
        promptText.append("MODEL ANSWER / REFERENCE:\n").append(prompt.modelAnswer()).append("\n\n");
        if (prompt.keyPoints() != null && !prompt.keyPoints().isEmpty()) {
            promptText.append("KEY POINTS REQUIRED:\n");
            for (String kp : prompt.keyPoints()) {
                promptText.append("- ").append(kp).append("\n");
            }
            promptText.append("\n");
        }
        promptText.append("CANDIDATE ANSWER:\n").append(prompt.candidateAnswer()).append("\n\n");
        promptText.append("RUBRIC CRITERIA TO GRADE (each from 0 to 100):\n");
        for (CriterionInfo c : prompt.criteria()) {
            promptText.append("- ID: ").append(c.id()).append(" | Name: ").append(c.name())
                    .append(" | Description: ").append(c.description()).append(" | Weight: ").append(c.weight()).append("\n");
        }

        promptText.append("\nRespond STRICTLY with a valid JSON object in this exact schema:\n");
        promptText.append("{\n");
        promptText.append("  \"overallScore\": <number 0-100>,\n");
        promptText.append("  \"feedback\": \"<overall summary feedback>\",\n");
        promptText.append("  \"strengths\": \"<what the candidate answered well>\",\n");
        promptText.append("  \"improvements\": \"<areas candidate should improve>\",\n");
        promptText.append("  \"criteria\": [\n");
        promptText.append("    { \"criterionId\": <id>, \"score\": <0-100>, \"evidenceQuote\": \"<exact words or summary>\", \"feedback\": \"<critique>\" }\n");
        promptText.append("  ]\n");
        promptText.append("}\n");

        Map<String, Object> requestBody = Map.of(
                "model", aiProperties.getModel(),
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a senior technical interviewer providing structured JSON evaluations."),
                        Map.of("role", "user", "content", promptText.toString())
                ),
                "temperature", 0.2
        );

        try {
            String rawJson = restClient.post()
                    .uri(GROQ_API_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode message = root.path("choices").get(0).path("message").path("content");
            JsonNode parsed = objectMapper.readTree(message.asText());

            int promptTokens = root.path("usage").path("prompt_tokens").asInt(0);
            int completionTokens = root.path("usage").path("completion_tokens").asInt(0);

            Map<Long, CriterionEvaluation> critMap = new HashMap<>();
            JsonNode criteriaArray = parsed.path("criteria");
            if (criteriaArray.isArray()) {
                for (JsonNode cNode : criteriaArray) {
                    Long cId = cNode.path("criterionId").asLong();
                    critMap.put(cId, new CriterionEvaluation(
                            cNode.path("score").asDouble(70.0),
                            cNode.path("evidenceQuote").asText(""),
                            cNode.path("feedback").asText("")
                    ));
                }
            }

            return new EvaluationResult(
                    parsed.path("overallScore").asDouble(70.0),
                    parsed.path("feedback").asText("Good effort."),
                    parsed.path("strengths").asText(""),
                    parsed.path("improvements").asText(""),
                    critMap,
                    promptTokens,
                    completionTokens,
                    aiProperties.getModel()
            );

        } catch (Exception e) {
            log.error("Groq API call failed", e);
            throw new RuntimeException("AI Provider evaluation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String generateFollowUp(FollowUpPrompt prompt) {
        String apiKey = resolveApiKey(prompt.apiKey());

        String promptText = "The interviewer asked: " + prompt.questionBody() +
                "\nThe candidate answered: " + prompt.candidateAnswer() +
                "\nReference model answer: " + prompt.modelAnswer() +
                "\n\nWrite ONE concise follow-up question digging deeper into their answer, probing edge cases or implementation details. Output only the question, nothing else.";

        Map<String, Object> requestBody = Map.of(
                "model", aiProperties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a senior technical interviewer asking a single follow-up question."),
                        Map.of("role", "user", "content", promptText)
                ),
                "temperature", 0.5,
                "max_tokens", 100
        );

        try {
            String rawJson = restClient.post()
                    .uri(GROQ_API_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(rawJson);
            return root.path("choices").get(0).path("message").path("content").asText().trim();
        } catch (Exception e) {
            log.error("Groq follow-up generation failed", e);
            return "Could you elaborate further on how you would test or monitor that in production?";
        }
    }

    private String resolveApiKey(String byokKey) {
        if (byokKey != null && !byokKey.isBlank()) {
            return byokKey;
        }
        if (aiProperties.getGroqApiKey() != null && !aiProperties.getGroqApiKey().isBlank()) {
            return aiProperties.getGroqApiKey();
        }
        throw new IllegalStateException("No Groq API key configured and no BYOK provided.");
    }
}
