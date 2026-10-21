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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.admin.api-key=test-admin-secret"
})
class QuestionReportIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private com.swayam.pathprep.filter.AdminKeyFilter adminKeyFilter;

    @Autowired
    private com.swayam.pathprep.filter.TraceIdFilter traceIdFilter;

    @Autowired
    private com.swayam.pathprep.repository.QuestionReportRepository questionReportRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        questionReportRepository.deleteAll();
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilter(traceIdFilter)
                .addFilter(adminKeyFilter)
                .build();
    }

    @Test
    void studentReportQuestion_andAdminListReports() throws Exception {
        long questionId = 1L; // dev seed Question 1

        String reportJson = """
                {
                    "reason": "Option explanations could be clearer about boolean primitives"
                }
                """;

        // Student reports question
        mockMvc.perform(post("/api/v1/questions/{id}/report", questionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.questionId").value(questionId))
                .andExpect(jsonPath("$.reason").value("Option explanations could be clearer about boolean primitives"));

        // Duplicate report by same student returns 409 Conflict
        mockMvc.perform(post("/api/v1/questions/{id}/report", questionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reportJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_REPORTED"));

        // Admin lists reports
        mockMvc.perform(get("/api/v1/admin/reports")
                        .header("X-Admin-Key", "test-admin-secret")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }
}
