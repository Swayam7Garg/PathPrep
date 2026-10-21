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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.admin.api-key=test-admin-secret"
})
class CoverageIntegrationTest {

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
    void getCoverage_returnsTracksWithTargetAndSubtopicBreakdown() throws Exception {
        mockMvc.perform(get("/api/v1/admin/content/coverage")
                        .param("trackId", "1")
                        .param("target", "5")
                        .param("includeSubtopics", "true")
                        .header("X-Admin-Key", "test-admin-secret")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].trackName").value("Java Backend"))
                .andExpect(jsonPath("$[0].targetPerTopic").value(5))
                .andExpect(jsonPath("$[0].meetsTarget").value(false)) // not enough questions yet
                .andExpect(jsonPath("$[0].topics", hasSize(10)));
    }
}
