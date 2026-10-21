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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.admin.api-key=test-admin-secret"
})
class BulkImportIntegrationTest {

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
    void bulkImport_processesValid_andDetectsInFileAndDbDuplicates() throws Exception {
        String suffix = java.util.UUID.randomUUID().toString();
        String json = """
                {
                    "batch": "test-batch-%s",
                    "questions": [
                        {
                            "topicName": "Java Fundamentals",
                            "difficulty": 1,
                            "body": "Unique import question body alpha %s",
                            "subtopic": "Variables",
                            "options": [
                                {"position": 0, "text": "Correct alpha", "isCorrect": true},
                                {"position": 1, "text": "Wrong alpha", "isCorrect": false}
                            ]
                        },
                        {
                            "topicName": "Java Fundamentals",
                            "difficulty": 1,
                            "body": "Unique import question body alpha %s",
                            "subtopic": "Variables",
                            "options": [
                                {"position": 0, "text": "Correct alpha", "isCorrect": true},
                                {"position": 1, "text": "Wrong alpha", "isCorrect": false}
                            ]
                        },
                        {
                            "topicName": "NonExistentTopic",
                            "difficulty": 1,
                            "body": "Question for non-existent topic",
                            "options": [
                                {"position": 0, "text": "Opt 1", "isCorrect": true},
                                {"position": 1, "text": "Opt 2", "isCorrect": false}
                            ]
                        }
                    ]
                }
                """.formatted(suffix, suffix, suffix);

        mockMvc.perform(post("/api/v1/admin/questions/import")
                        .header("X-Admin-Key", "test-admin-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batch").value("test-batch-" + suffix))
                .andExpect(jsonPath("$.totalProcessed").value(3))
                .andExpect(jsonPath("$.importedCount").value(1))
                .andExpect(jsonPath("$.skippedDuplicatesCount").value(1))
                .andExpect(jsonPath("$.items[0].status").value("WOULD_CREATE"))
                .andExpect(jsonPath("$.items[1].status").value("DUPLICATE"))
                .andExpect(jsonPath("$.items[2].status").value("INVALID"));

        // Now run with dryRun: false
        String jsonReal = """
                {
                    "batch": "test-batch-real-%s",
                    "dryRun": false,
                    "questions": [
                        {
                            "topicName": "Java Fundamentals",
                            "difficulty": 1,
                            "body": "Unique import question body real %s",
                            "options": [
                                {"position": 0, "text": "Correct alpha", "isCorrect": true},
                                {"position": 1, "text": "Wrong alpha", "isCorrect": false}
                            ]
                        }
                    ]
                }
                """.formatted(suffix, suffix);

        mockMvc.perform(post("/api/v1/admin/questions/import")
                        .header("X-Admin-Key", "test-admin-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonReal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].status").value("CREATED"));
    }
}
