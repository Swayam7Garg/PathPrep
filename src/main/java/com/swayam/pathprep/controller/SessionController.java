package com.swayam.pathprep.controller;

import com.swayam.pathprep.dto.AnswerRequest;
import com.swayam.pathprep.dto.AnswerResponse;
import com.swayam.pathprep.dto.SessionCreateRequest;
import com.swayam.pathprep.dto.SessionResponse;
import com.swayam.pathprep.service.SessionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Slf4j
@RestController
@RequestMapping("/api/v1")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * Creates a new practice session.
     * Returns {@code 201 CREATED} with a {@code Location} header pointing to the new session.
     */
    @PostMapping("/sessions")
    public ResponseEntity<SessionResponse> createSession(@Valid @RequestBody SessionCreateRequest request) {
        log.info("Received request to create practice session: trackId={}, mode={}, difficulty={}, questionCount={}",
                request.trackId(), request.mode(), request.difficulty(), request.questionCount());

        SessionResponse response = sessionService.createSession(request);

        log.info("Practice session created successfully: sessionId={}, totalQuestions={}",
                response.id(), response.totalQuestions());

        URI location = URI.create("/api/v1/sessions/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Submits an MCQ answer for the current question in a quiz session.
     *
     * <p>Instantly grades the answer in Java (zero AI). The response includes:
     * <ul>
     *   <li>Whether the chosen option was correct</li>
     *   <li>The correct option ID and its explanation</li>
     *   <li>The next sanitised question (null when session is COMPLETED)</li>
     * </ul>
     *
     * <p>Idempotent: submitting the same {@code idempotencyKey} twice returns the
     * same response without re-grading or double-writing.
     *
     * @param id      the session ID from the URL path
     * @param request the answer payload
     * @return {@code 201 CREATED} with the grading result
     */
    @PostMapping("/sessions/{id}/answers")
    public ResponseEntity<AnswerResponse> submitAnswer(
            @PathVariable Long id,
            @Valid @RequestBody AnswerRequest request) {

        log.info("Received answer submission: sessionId={}, selectedOptionId={}, idempotencyKey={}",
                id, request.selectedOptionId(), request.idempotencyKey());

        AnswerResponse response = sessionService.submitAnswer(id, request);

        log.info("Answer submission processed: sessionId={}, correct={}, sessionStatus={}",
                id, response.correct(), response.sessionStatus());

        URI location = URI.create("/api/v1/sessions/" + id);
        return ResponseEntity.created(location).body(response);
    }
}

