package com.swayam.pathprep.controller.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.admin.api-key=test-admin-secret"
})
class AdminQuestionControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private com.swayam.pathprep.filter.AdminKeyFilter adminKeyFilter;

    @Autowired
    private com.swayam.pathprep.filter.TraceIdFilter traceIdFilter;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilter(traceIdFilter)
                .addFilter(adminKeyFilter)
                .build();
    }

    @Test
    void createQuestion_withSubtopic_createsDraftQuestion() throws Exception {
        String suffix = java.util.UUID.randomUUID().toString();
        String json = """
                {
                    "topicId": 1,
                    "type": "MCQ",
                    "difficulty": 2,
                    "body": "What does volatile keyword do in Java? %s",
                    "subtopic": "Memory Model",
                    "options": [
                        {"position": 0, "text": "Guarantees visibility of changes across threads", "isCorrect": true},
                        {"position": 1, "text": "Provides atomicity for compound operations", "isCorrect": false}
                    ]
                }
                """.formatted(suffix);

        mockMvc.perform(post("/api/v1/admin/questions")
                        .header("X-Admin-Key", "test-admin-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.subtopic").value("Memory Model"))
                .andExpect(jsonPath("$.contentHash").isNotEmpty())
                .andExpect(jsonPath("$.isQuizEligible").value(false)); // status is DRAFT
    }

    @Test
    void createDuplicateQuestion_returns409Conflict() throws Exception {
        String suffix = java.util.UUID.randomUUID().toString();
        String json = """
                {
                    "topicId": 1,
                    "type": "MCQ",
                    "difficulty": 1,
                    "body": "Duplicate Question Test Body %s",
                    "options": [
                        {"position": 0, "text": "Option One", "isCorrect": true},
                        {"position": 1, "text": "Option Two", "isCorrect": false}
                    ]
                }
                """.formatted(suffix);

        mockMvc.perform(post("/api/v1/admin/questions")
                        .header("X-Admin-Key", "test-admin-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        // Submitting same content again returns 409 DUPLICATE_QUESTION
        mockMvc.perform(post("/api/v1/admin/questions")
                        .header("X-Admin-Key", "test-admin-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_QUESTION"));
    }

    @Test
    void publishQuestion_returns422_whenQuestionInvalid() throws Exception {
        String suffix = java.util.UUID.randomUUID().toString();
        // Create question with NO correct option
        String json = """
                {
                    "topicId": 1,
                    "type": "MCQ",
                    "difficulty": 1,
                    "body": "Question with zero correct answers %s",
                    "options": [
                        {"position": 0, "text": "Wrong 1", "isCorrect": false},
                        {"position": 1, "text": "Wrong 2", "isCorrect": false}
                    ]
                }
                """.formatted(suffix);

        String response = mockMvc.perform(post("/api/v1/admin/questions")
                        .header("X-Admin-Key", "test-admin-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // Extract ID
        int idStart = response.indexOf("\"id\":") + 5;
        int idEnd = response.indexOf(",", idStart);
        String questionId = response.substring(idStart, idEnd).trim();

        // Attempt to publish
        mockMvc.perform(post("/api/v1/admin/questions/" + questionId + "/publish")
                        .header("X-Admin-Key", "test-admin-secret"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("QUESTION_INVALID"))
                .andExpect(jsonPath("$.details", hasItem(containsString("requires exactly 1 correct option"))));
    }

    @Test
    void flagQuestion_updatesStatusAndReason() throws Exception {
        String suffix = java.util.UUID.randomUUID().toString();
        String json = """
                {
                    "topicId": 1,
                    "type": "MCQ",
                    "difficulty": 1,
                    "body": "Question to be flagged %s",
                    "options": [
                        {"position": 0, "text": "Choice A", "isCorrect": true},
                        {"position": 1, "text": "Choice B", "isCorrect": false}
                    ]
                }
                """.formatted(suffix);

        String response = mockMvc.perform(post("/api/v1/admin/questions")
                        .header("X-Admin-Key", "test-admin-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        int idStart = response.indexOf("\"id\":") + 5;
        int idEnd = response.indexOf(",", idStart);
        String questionId = response.substring(idStart, idEnd).trim();

        // Flag it
        mockMvc.perform(post("/api/v1/admin/questions/" + questionId + "/flag")
                        .header("X-Admin-Key", "test-admin-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"Outdated API usage\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FLAGGED"))
                .andExpect(jsonPath("$.flagReason").value("Outdated API usage"));
    }
}
