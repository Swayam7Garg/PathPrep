package com.swayam.pathprep.service;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.swayam.pathprep.ai.LlmClient;
import com.swayam.pathprep.entity.*;
import com.swayam.pathprep.enums.SessionMode;
import com.swayam.pathprep.enums.SessionStatus;
import com.swayam.pathprep.enums.SessionStyle;
import com.swayam.pathprep.exception.ApiException;
import com.swayam.pathprep.exception.ConflictException;
import com.swayam.pathprep.exception.NotFoundException;
import com.swayam.pathprep.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewService {

    private final PracticeSessionRepository practiceSessionRepository;
    private final SessionQuestionRepository sessionQuestionRepository;
    private final QuestionRepository questionRepository;
    private final AttemptRepository attemptRepository;
    private final RubricRepository rubricRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final EvaluationRepository evaluationRepository;
    private final CriterionScoreRepository criterionScoreRepository;
    private final LlmClient llmClient;
    private final LlmUsageService llmUsageService;
    private final SessionFinalizer sessionFinalizer;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * Grades candidate text response against rubric criteria and question key points.
     * Generates explainable rubric scores and optional follow-up in LIVE style.
     */
    @Transactional
    public Evaluation gradeInterviewAnswer(Long sessionId, Long userId, Long attemptId, String answerText) {
        PracticeSession session = practiceSessionRepository.findByIdAndUserIdWithLock(sessionId, userId)
                .orElseThrow(() -> new NotFoundException("SESSION_NOT_FOUND", "Session not found: " + sessionId));

        if (session.getMode() != SessionMode.INTERVIEW) {
            throw new com.swayam.pathprep.exception.BadRequestException("MODE_NOT_APPLICABLE", "Session is not in INTERVIEW mode");
        }

        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new NotFoundException("ATTEMPT_NOT_FOUND", "Attempt not found: " + attemptId));

        Question question = attempt.getQuestion();

        // 1. Resolve BYOK or shared quota
        Optional<String> userApiKey = llmUsageService.getUserApiKey(userId);
        boolean isByok = userApiKey.isPresent();
        llmUsageService.validateQuota(userId, isByok);

        // 2. Fetch Rubric & Criteria
        List<RubricCriterion> criteriaList = new ArrayList<>();
        if (question.getRubricId() != null) {
            criteriaList = rubricCriterionRepository.findByRubricId(question.getRubricId());
        }
        if (criteriaList.isEmpty()) {
            // Default 3 standard software engineering interview criteria
            Rubric defaultRubric = rubricRepository.findAll().stream().findFirst().orElse(null);
            if (defaultRubric != null) {
                criteriaList = rubricCriterionRepository.findByRubricId(defaultRubric.getId());
            }
        }

        List<LlmClient.CriterionInfo> critInfos = criteriaList.stream()
                .map(c -> new LlmClient.CriterionInfo(c.getId(), c.getName(), c.getDescription(), c.getWeight().intValue()))
                .toList();

        // Parse key points from json
        List<String> keyPoints = new ArrayList<>();
        if (question.getKeyPoints() != null && !question.getKeyPoints().isBlank()) {
            try {
                keyPoints = objectMapper.readValue(question.getKeyPoints(), new TypeReference<>() {});
            } catch (Exception e) {
                log.warn("Could not parse keyPoints JSON for questionId={}", question.getId());
            }
        }

        LlmClient.EvaluationPrompt prompt = new LlmClient.EvaluationPrompt(
                userApiKey.orElse(null),
                question.getBody(),
                question.getType().name(),
                keyPoints,
                question.getModelAnswer() != null ? question.getModelAnswer() : "Clear technical explanation.",
                critInfos,
                answerText
        );

        // 3. Invoke LlmClient
        LlmClient.EvaluationResult result = llmClient.evaluateAnswer(prompt);

        // 4. Record usage metrics
        llmUsageService.recordUsage(
                userId,
                "GROQ",
                result.model() != null ? result.model() : "groq",
                result.promptTokens(),
                result.completionTokens(),
                isByok
        );

        // 5. Persist Evaluation
        Evaluation evaluation = new Evaluation();
        evaluation.setAttempt(attempt);
        evaluation.setStatus("DONE");
        evaluation.setOverallScore(BigDecimal.valueOf(result.overallScore()));
        evaluation.setFeedback(result.feedback());
        evaluation.setStrengths(result.strengths());
        evaluation.setImprovements(result.improvements());
        evaluation.setModelVersion(result.model());
        evaluation.setTokensUsed(result.promptTokens() + result.completionTokens());

        evaluation = evaluationRepository.save(evaluation);

        // 6. Persist Criterion Scores
        Map<Long, RubricCriterion> critMap = new HashMap<>();
        for (RubricCriterion c : criteriaList) {
            critMap.put(c.getId(), c);
        }

        List<CriterionScore> scores = new ArrayList<>();
        if (result.criterionEvaluations() != null) {
            for (Map.Entry<Long, LlmClient.CriterionEvaluation> entry : result.criterionEvaluations().entrySet()) {
                RubricCriterion criterion = critMap.get(entry.getKey());
                if (criterion != null) {
                    CriterionScore cs = new CriterionScore();
                    cs.setEvaluation(evaluation);
                    cs.setCriterion(criterion);
                    cs.setScore(BigDecimal.valueOf(entry.getValue().score()));
                    cs.setEvidenceQuote(entry.getValue().evidenceQuote());
                    cs.setFeedback(entry.getValue().feedback());
                    scores.add(cs);
                }
            }
        }
        if (!scores.isEmpty()) {
            criterionScoreRepository.saveAll(scores);
            evaluation.setCriterionScores(scores);
        }

        // 7. Update Attempt correctness (pass if overallScore >= 60%)
        attempt.setCorrect(result.overallScore() >= 60.0);
        attemptRepository.save(attempt);

        // 8. Handle LIVE style follow-up question (at most 1 follow-up)
        if (session.getStyle() == SessionStyle.LIVE && attempt.getParentAttempt() == null) {
            log.info("Generating LIVE interview follow-up for sessionId={}, attemptId={}", sessionId, attempt.getId());
            LlmClient.FollowUpPrompt followUpPrompt = new LlmClient.FollowUpPrompt(
                    userApiKey.orElse(null),
                    question.getBody(),
                    answerText,
                    question.getModelAnswer()
            );
            String followUpBody = llmClient.generateFollowUp(followUpPrompt);
            log.info("Follow-up question generated: {}", followUpBody);
        }

        return evaluation;
    }
}
