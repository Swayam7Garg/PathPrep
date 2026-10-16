package com.swayam.pathprep.controller;

import com.swayam.pathprep.filter.TraceIdFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Phase 4: POST /api/v1/sessions/{id}/answers
 *
 * These tests exercise the full Spring context against the real H2/MySQL test database.
 * Each test creates a fresh session first, then submits one or more answers to verify:
 *   - Happy path grading (correct and wrong)
 *   - Session completion after the last answer
 *   - Idempotency (same key = same response, no double-write)
 *   - Validation and error paths (404, 409, 422, 400)
 */
@SpringBootTest
class AnswerSubmissionIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private TraceIdFilter traceIdFilter;
    @Autowired
    private com.swayam.pathprep.repository.PracticeSessionRepository practiceSessionRepository;
    @Autowired
    private com.swayam.pathprep.security.CurrentUser currentUser;

    private MockMvc mockMvc;

    private static final long JAVA_BACKEND_TRACK_ID = 1L;

    @BeforeEach
    void setUp() {
        practiceSessionRepository.findFirstByUserIdAndStatus(currentUser.getUserId(), com.swayam.pathprep.enums.SessionStatus.IN_PROGRESS)
                .ifPresent(s -> {
                    s.setStatus(com.swayam.pathprep.enums.SessionStatus.ABANDONED);
                    practiceSessionRepository.save(s);
                });
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .addFilter(traceIdFilter)
                .build();
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // Helper: create a 1-question QUIZ session and return its JSON response
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Creates a fresh quiz session with exactly {@code questionCount} questions
     * and returns the full MockMvc response so callers can extract sessionId / optionIds.
     */
    private MvcResult createSession(int questionCount) throws Exception {
        String payload = """
                {
                    "mode": "QUIZ",
                    "trackId": %d,
                    "questionCount": %d,
                    "difficulty": "MIXED"
                }
                """.formatted(JAVA_BACKEND_TRACK_ID, questionCount);

        return mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();
    }

    /** Extracts a long value from JSON using Jackson / jsonPath result. */
    private long extractLong(MvcResult result, String jsonPath) throws Exception {
        String body = result.getResponse().getContentAsString();
        com.jayway.jsonpath.DocumentContext ctx = com.jayway.jsonpath.JsonPath.parse(body);
        Number value = ctx.read(jsonPath);
        return value.longValue();
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // T-1  Happy path: submit an answer and get a graded response
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @Test
    void submitAnswer_returnsGradedResponse_withNextQuestion() throws Exception {
        MvcResult sessionResult = createSession(2);
        long sessionId = extractLong(sessionResult, "$.id");
        long optionId  = extractLong(sessionResult, "$.currentQuestion.options[0].id");

        String payload = """
                {
                    "selectedOptionId": %d,
                    "idempotencyKey": "%s"
                }
                """.formatted(optionId, UUID.randomUUID());

        mockMvc.perform(post("/api/v1/sessions/{id}/answers", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correct").isBoolean())
                .andExpect(jsonPath("$.correctOptionId").isNumber())
                .andExpect(jsonPath("$.sessionStatus", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.currentIndex", is(1)))
                .andExpect(jsonPath("$.totalQuestions", is(2)))
                .andExpect(jsonPath("$.nextQuestion").exists())
                .andExpect(jsonPath("$.nextQuestion.id").isNumber())
                // Answer keys must NOT leak through nextQuestion options
                .andExpect(jsonPath("$.nextQuestion.options[0].isCorrect").doesNotExist())
                .andExpect(jsonPath("$.nextQuestion.options[0].correct").doesNotExist())
                .andExpect(jsonPath("$.nextQuestion.options[0].explanation").doesNotExist());
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // T-2  Last answer completes the session; nextQuestion must be null
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @Test
    void submitAnswer_completesSession_whenLastQuestionAnswered() throws Exception {
        MvcResult sessionResult = createSession(1);
        long sessionId = extractLong(sessionResult, "$.id");
        long optionId  = extractLong(sessionResult, "$.currentQuestion.options[0].id");

        String payload = """
                {
                    "selectedOptionId": %d,
                    "idempotencyKey": "%s"
                }
                """.formatted(optionId, UUID.randomUUID());

        mockMvc.perform(post("/api/v1/sessions/{id}/answers", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionStatus", is("COMPLETED")))
                .andExpect(jsonPath("$.currentIndex", is(1)))
                .andExpect(jsonPath("$.nextQuestion").doesNotExist());
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // T-3  Submitting to a COMPLETED session must return 409
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @Test
    void submitAnswer_returns409_whenSessionAlreadyCompleted() throws Exception {
        MvcResult sessionResult = createSession(1);
        long sessionId = extractLong(sessionResult, "$.id");
        long optionId  = extractLong(sessionResult, "$.currentQuestion.options[0].id");

        // First submission â€” completes the session
        mockMvc.perform(post("/api/v1/sessions/{id}/answers", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"selectedOptionId": %d, "idempotencyKey": "%s"}
                                """.formatted(optionId, UUID.randomUUID())))
                .andExpect(status().isCreated());

        // Second submission with a new key â€” must be rejected with 409
        mockMvc.perform(post("/api/v1/sessions/{id}/answers", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"selectedOptionId": %d, "idempotencyKey": "%s"}
                                """.formatted(optionId, UUID.randomUUID())))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("SESSION_ALREADY_COMPLETED")));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // T-4  Idempotency: same key twice must return identical response, no duplicate write
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @Test
    void submitAnswer_isIdempotent_samekeyReturnsSameResponse() throws Exception {
        MvcResult sessionResult = createSession(2);
        long sessionId = extractLong(sessionResult, "$.id");
        long optionId  = extractLong(sessionResult, "$.currentQuestion.options[0].id");
        String key = UUID.randomUUID().toString();

        String payload = """
                {"selectedOptionId": %d, "idempotencyKey": "%s"}
                """.formatted(optionId, key);

        // First call
        String firstBody = mockMvc.perform(post("/api/v1/sessions/{id}/answers", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // Second call â€” same key
        String secondBody = mockMvc.perform(post("/api/v1/sessions/{id}/answers", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // Both responses must be identical (same grading result, same currentIndex)
        org.assertj.core.api.Assertions.assertThat(firstBody).isEqualTo(secondBody);
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // T-5  Non-existent session ID must return 404
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @Test
    void submitAnswer_returns404_whenSessionDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/v1/sessions/{id}/answers", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"selectedOptionId": 1, "idempotencyKey": "%s"}
                                """.formatted(UUID.randomUUID())))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("SESSION_NOT_FOUND")));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // T-6  Non-existent option ID must return 404
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @Test
    void submitAnswer_returns404_whenOptionDoesNotExist() throws Exception {
        MvcResult sessionResult = createSession(1);
        long sessionId = extractLong(sessionResult, "$.id");

        mockMvc.perform(post("/api/v1/sessions/{id}/answers", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"selectedOptionId": 999999, "idempotencyKey": "%s"}
                                """.formatted(UUID.randomUUID())))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("OPTION_NOT_FOUND")));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // T-7  Missing selectedOptionId must return 400 VALIDATION_FAILED
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @Test
    void submitAnswer_returns400_whenSelectedOptionIdMissing() throws Exception {
        MvcResult sessionResult = createSession(1);
        long sessionId = extractLong(sessionResult, "$.id");

        mockMvc.perform(post("/api/v1/sessions/{id}/answers", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotencyKey": "%s"}
                                """.formatted(UUID.randomUUID())))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // T-8  Missing idempotencyKey must return 400 VALIDATION_FAILED
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @Test
    void submitAnswer_returns400_whenIdempotencyKeyMissing() throws Exception {
        MvcResult sessionResult = createSession(1);
        long sessionId = extractLong(sessionResult, "$.id");
        long optionId  = extractLong(sessionResult, "$.currentQuestion.options[0].id");

        mockMvc.perform(post("/api/v1/sessions/{id}/answers", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"selectedOptionId": %d}
                                """.formatted(optionId)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // T-9  Response always contains the correct option ID even when wrong answer chosen
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @Test
    void submitAnswer_alwaysReturnsCorrectOptionId() throws Exception {
        MvcResult sessionResult = createSession(1);
        long sessionId = extractLong(sessionResult, "$.id");
        long optionId  = extractLong(sessionResult, "$.currentQuestion.options[0].id");

        mockMvc.perform(post("/api/v1/sessions/{id}/answers", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"selectedOptionId": %d, "idempotencyKey": "%s"}
                                """.formatted(optionId, UUID.randomUUID())))
                .andExpect(status().isCreated())
                // correctOptionId must always be present, regardless of whether the answer was right
                .andExpect(jsonPath("$.correctOptionId").isNumber())
                .andExpect(jsonPath("$.correctOptionId", greaterThan(0)));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // T-10  currentIndex increments by exactly 1 per submission
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @Test
    void submitAnswer_incrementsCurrentIndexByOne() throws Exception {
        MvcResult sessionResult = createSession(3);
        long sessionId = extractLong(sessionResult, "$.id");
        long optionId  = extractLong(sessionResult, "$.currentQuestion.options[0].id");

        mockMvc.perform(post("/api/v1/sessions/{id}/answers", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"selectedOptionId": %d, "idempotencyKey": "%s"}
                                """.formatted(optionId, UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentIndex", is(1)));  // was 0, now 1
    }
}

