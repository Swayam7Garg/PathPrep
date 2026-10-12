package com.swayam.pathprep.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class SessionEndpointIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private static final long JAVA_BACKEND_TRACK_ID = 1L;
    private static final long WEB_FRONTEND_TRACK_ID = 2L;

    @Autowired
    private com.swayam.pathprep.filter.TraceIdFilter traceIdFilter;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilter(traceIdFilter)
                .build();
    }

    @Test
    void createSession_createsQuizSession_andDoesNotLeakAnswers() throws Exception {
        String payload = """
                {
                    "mode": "QUIZ",
                    "trackId": %d,
                    "questionCount": 2,
                    "difficulty": "MIXED"
                }
                """.formatted(JAVA_BACKEND_TRACK_ID);

        mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/v1/sessions/")))
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.mode", is("QUIZ")))
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.totalQuestions", is(2)))
                .andExpect(jsonPath("$.currentIndex", is(0)))
                .andExpect(jsonPath("$.currentQuestion.id").isNotEmpty())
                .andExpect(jsonPath("$.currentQuestion.body").isNotEmpty())
                .andExpect(jsonPath("$.currentQuestion.options", hasSize(4)))
                // Verify no answer leakage in option view
                .andExpect(jsonPath("$.currentQuestion.options[0].isCorrect").doesNotExist())
                .andExpect(jsonPath("$.currentQuestion.options[0].correct").doesNotExist())
                .andExpect(jsonPath("$.currentQuestion.options[0].explanation").doesNotExist())
                .andExpect(jsonPath("$.currentQuestion.options[0].text").isNotEmpty());
    }

    @Test
    void createSession_returns400ModeNotAvailable_whenModeIsInterview() throws Exception {
        String payload = """
                {
                    "mode": "INTERVIEW",
                    "trackId": %d
                }
                """.formatted(JAVA_BACKEND_TRACK_ID);

        mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("MODE_NOT_AVAILABLE")))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void createSession_returns404_whenTrackDoesNotExist() throws Exception {
        String payload = """
                {
                    "mode": "QUIZ",
                    "trackId": 999999
                }
                """;

        mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("TRACK_NOT_FOUND")))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void createSession_returns400TopicNotInTrack_whenTopicBelongsToOtherTrack() throws Exception {
        // Topic 999999 does not belong to Java Backend track
        String payload = """
                {
                    "mode": "QUIZ",
                    "trackId": %d,
                    "topicIds": [999999]
                }
                """.formatted(JAVA_BACKEND_TRACK_ID);

        mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("TOPIC_NOT_IN_TRACK")))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void createSession_returns400ValidationFailed_whenMissingRequiredTrackId() throws Exception {
        String payload = """
                {
                    "mode": "QUIZ"
                }
                """;

        mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.details", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void createSession_returns400InvalidQuestionCount_whenCountExceedsMax() throws Exception {
        String payload = """
                {
                    "mode": "QUIZ",
                    "trackId": %d,
                    "questionCount": 50
                }
                """.formatted(JAVA_BACKEND_TRACK_ID);

        mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_QUESTION_COUNT")));
    }

    @Test
    void createSession_returns422NotEnoughQuestions_whenTrackHasNoPublishedQuestions() throws Exception {
        String payload = """
                {
                    "mode": "QUIZ",
                    "trackId": %d
                }
                """.formatted(WEB_FRONTEND_TRACK_ID);

        mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("NOT_ENOUGH_QUESTIONS")));
    }
}
