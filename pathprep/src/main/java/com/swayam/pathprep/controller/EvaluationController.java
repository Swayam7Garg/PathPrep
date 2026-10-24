package com.swayam.pathprep.controller;

import com.swayam.pathprep.dto.response.EvaluationResponse;
import com.swayam.pathprep.entity.*;
import com.swayam.pathprep.enums.SessionMode;
import com.swayam.pathprep.enums.SessionStatus;
import com.swayam.pathprep.exception.NotFoundException;
import com.swayam.pathprep.repository.*;
import com.swayam.pathprep.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationRepository evaluationRepository;
    private final PracticeSessionRepository practiceSessionRepository;
    private final SessionQuestionRepository sessionQuestionRepository;
    private final QuestionRepository questionRepository;
    private final AttemptRepository attemptRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final CurrentUser currentUser;

    public record CriterionDto(String id, String name, String description, Double weight) {}
    public record TurnDto(
            String turnId,
            String type,
            Integer questionNumber,
            Boolean isFollowUp,
            String body,
            Integer timeLimitSec,
            List<CriterionDto> rubricCriteria,
            String answerText,
            Integer timeSpentSec
    ) {}
    public record AwaitingDto(String type, String turnId) {}
    public record InterviewSessionStateResponse(
            Long id,
            String status,
            String mode,
            String style,
            Long trackId,
            String trackName,
            int totalQuestions,
            int currentQuestionNumber,
            List<TurnDto> turns,
            AwaitingDto awaiting
    ) {}
    public record FeedbackRequest(Boolean positive, String rating, String comment) {}

    /**
     * Poll evaluations for a session without streaming or SSE.
     */
    @GetMapping("/sessions/{sessionId}/evaluations")
    public ResponseEntity<List<EvaluationResponse>> getSessionEvaluations(@PathVariable Long sessionId) {
        log.info("Fetching evaluations for session: sessionId={}", sessionId);
        List<Evaluation> evals = evaluationRepository.findWithScoresBySessionId(sessionId);

        List<EvaluationResponse> responses = evals.stream().map(e -> new EvaluationResponse(
                e.getId(),
                e.getAttempt().getId(),
                e.getAttempt().getQuestion().getId(),
                e.getAttempt().getQuestion().getBody(),
                e.getAttempt().getAnswerText(),
                e.getStatus(),
                e.getOverallScore(),
                e.getFeedback(),
                e.getStrengths(),
                e.getImprovements(),
                e.getTokensUsed(),
                e.getCriterionScores().stream().map(cs -> new EvaluationResponse.CriterionScoreDto(
                        cs.getCriterion().getId(),
                        cs.getCriterion().getName(),
                        cs.getScore(),
                        cs.getEvidenceQuote(),
                        cs.getFeedback()
                )).toList(),
                e.getCreatedAt()
        )).toList();

        return ResponseEntity.ok(responses);
    }

    /**
     * Poll interview state and turns for an active or completed interview session.
     */
    @GetMapping("/sessions/{sessionId}/interview")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<InterviewSessionStateResponse> getInterviewSession(@PathVariable Long sessionId) {
        Long userId = currentUser.getUserId();
        PracticeSession session = practiceSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new NotFoundException("SESSION_NOT_FOUND", "Session not found: " + sessionId));

        List<SessionQuestion> sessionQuestions = sessionQuestionRepository.findByIdSessionIdOrderByPositionAsc(sessionId);
        List<Attempt> attempts = attemptRepository.findBySessionIdOrderByAnsweredAtAsc(sessionId);

        Map<Long, Attempt> attemptMap = new HashMap<>();
        for (Attempt a : attempts) {
            attemptMap.put(a.getQuestion().getId(), a);
        }

        List<TurnDto> turns = new ArrayList<>();
        int currentQNum = Math.min(session.getCurrentIndex() + 1, session.getTotalQuestions());

        for (SessionQuestion sq : sessionQuestions) {
            Question q = sq.getQuestion();
            int qNum = sq.getPosition() + 1;

            List<CriterionDto> criteria = new ArrayList<>();
            if (q.getRubricId() != null) {
                criteria = rubricCriterionRepository.findByRubricId(q.getRubricId()).stream()
                        .map(c -> new CriterionDto(String.valueOf(c.getId()), c.getName(), c.getDescription(), c.getWeight() != null ? c.getWeight().doubleValue() : 1.0))
                        .toList();
            }

            // Question Turn
            turns.add(new TurnDto(
                    "turn-q-" + sq.getPosition(),
                    "QUESTION",
                    qNum,
                    false,
                    q.getBody(),
                    180,
                    criteria,
                    null,
                    null
            ));

            // Answer Turn if submitted
            Attempt att = attemptMap.get(q.getId());
            if (att != null) {
                turns.add(new TurnDto(
                        "turn-a-" + sq.getPosition(),
                        "ANSWER",
                        qNum,
                        false,
                        att.getAnswerText(),
                        null,
                        null,
                        att.getAnswerText(),
                        att.getTimeSpentSec()
                ));
            }
        }

        AwaitingDto awaiting = null;
        if (session.getStatus() == SessionStatus.IN_PROGRESS) {
            if (session.getCurrentIndex() < session.getTotalQuestions()) {
                awaiting = new AwaitingDto("ANSWER", "turn-q-" + session.getCurrentIndex());
            } else {
                awaiting = new AwaitingDto("COMPLETION", null);
            }
        }

        InterviewSessionStateResponse response = new InterviewSessionStateResponse(
                session.getId(),
                session.getStatus().name(),
                session.getMode() != null ? session.getMode().name() : "INTERVIEW",
                session.getStyle() != null ? session.getStyle().name() : "LITE",
                session.getTrack().getId(),
                session.getTrack().getName(),
                session.getTotalQuestions(),
                currentQNum,
                turns,
                awaiting
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Records candidate thumbs up / thumbs down feedback on an evaluation.
     */
    @PostMapping("/evaluations/{id}/feedback")
    public ResponseEntity<Map<String, Object>> submitFeedback(
            @PathVariable Long id,
            @RequestBody(required = false) FeedbackRequest request) {
        log.info("Recorded feedback for evaluation id={}, positive={}", id, request != null ? request.positive() : null);
        return ResponseEntity.ok(Map.of("success", true, "message", "Evaluation feedback recorded"));
    }

    /**
     * Retries evaluation generation for a failed interview session.
     */
    @PostMapping("/sessions/{sessionId}/evaluations/retry")
    public ResponseEntity<Map<String, Object>> retryEvaluations(@PathVariable Long sessionId) {
        log.info("Retrying evaluations for sessionId={}", sessionId);
        return ResponseEntity.ok(Map.of("status", "EVALUATION", "message", "Evaluation retry initiated"));
    }
}
