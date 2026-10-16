package com.swayam.pathprep.controller;

import com.swayam.pathprep.dto.*;
import com.swayam.pathprep.enums.SessionMode;
import com.swayam.pathprep.enums.SessionStatus;
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
     * Creates a new practice session (Phase 3).
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
     * Submits an MCQ answer for the current question in a quiz session (Phase 4).
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

    /**
     * Retrieves session details for resuming an in-progress session or viewing progress (Phase 5).
     * Read-only, no side effects, no locking.
     */
    @GetMapping("/sessions/{id}")
    public ResponseEntity<SessionDetailResponse> getSession(@PathVariable Long id) {
        log.info("Received request to fetch session details: sessionId={}", id);
        SessionDetailResponse response = sessionService.getSessionDetail(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Explicitly finalizes an active quiz session (Phase 5).
     * Idempotent: returns 200 with same summary on an already finalized session.
     */
    @PostMapping("/sessions/{id}/complete")
    public ResponseEntity<SessionCompleteResponse> completeSession(@PathVariable Long id) {
        log.info("Received request to complete session: sessionId={}", id);
        SessionCompleteResponse response = sessionService.completeSession(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the diagnostic scorecard and report for a completed session (Phase 5).
     */
    @GetMapping("/sessions/{id}/report")
    public ResponseEntity<SessionReportResponse> getSessionReport(@PathVariable Long id) {
        log.info("Received request to fetch session report: sessionId={}", id);
        SessionReportResponse response = sessionService.getSessionReport(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a paged list of past practice sessions for the authenticated user (Phase 5).
     */
    @GetMapping("/sessions")
    public ResponseEntity<PagedResponse<SessionListItem>> getSessionHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) SessionStatus status,
            @RequestParam(required = false) SessionMode mode,
            @RequestParam(required = false) Long trackId) {

        log.info("Received request for session history: page={}, size={}, status={}, mode={}, trackId={}",
                page, size, status, mode, trackId);

        PagedResponse<SessionListItem> response = sessionService.getSessionHistory(page, size, status, mode, trackId);
        return ResponseEntity.ok(response);
    }
}
