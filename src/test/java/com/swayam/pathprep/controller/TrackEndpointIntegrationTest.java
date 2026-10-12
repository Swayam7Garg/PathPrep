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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class TrackEndpointIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private static final long JAVA_BACKEND_TRACK_ID = 1L;
    private static final long JAVA_FUNDAMENTALS_TOPIC_ID = 1L;

    @Autowired
    private com.swayam.pathprep.filter.TraceIdFilter traceIdFilter;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilter(traceIdFilter)
                .build();
    }

    @Test
    void listTracks_returnsAllTracksWithCountsAndTraceId() throws Exception {
        mockMvc.perform(get("/api/v1/tracks")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].name", is("General SDE")))
                .andExpect(jsonPath("$[1].name", is("Java Backend")))
                .andExpect(jsonPath("$[1].topicCount", is(4)))
                .andExpect(jsonPath("$[1].questionCount", is(2)))
                .andExpect(jsonPath("$[2].name", is("Web Frontend")));
    }

    @Test
    void getTopics_returnsTopicsSortedByWeightDesc_andIncludesPrereqs() throws Exception {
        mockMvc.perform(get("/api/v1/tracks/{trackId}/topics", JAVA_BACKEND_TRACK_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].name", is("Spring Boot Basics")))
                .andExpect(jsonPath("$[0].weight", is(2.0)))
                .andExpect(jsonPath("$[0].prerequisiteIds[0]", is((int) JAVA_FUNDAMENTALS_TOPIC_ID)));
    }

    @Test
    void getTopics_returns404_whenTrackDoesNotExist() throws Exception {
        long nonExistentId = 999999L;
        mockMvc.perform(get("/api/v1/tracks/{trackId}/topics", nonExistentId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("TRACK_NOT_FOUND")))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void getTopics_returns400_whenTrackIdIsInvalidParameter() throws Exception {
        mockMvc.perform(get("/api/v1/tracks/{trackId}/topics", "invalid-id-string")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_PARAMETER")))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }
}
