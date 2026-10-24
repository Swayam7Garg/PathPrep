package com.swayam.pathprep.controller;

import com.swayam.pathprep.dto.plan.request.CreateDailyPlanItemRequest;
import com.swayam.pathprep.entity.*;
import com.swayam.pathprep.enums.DailyPlanItemType;
import com.swayam.pathprep.enums.QuizDifficulty;
import com.swayam.pathprep.enums.SessionMode;
import com.swayam.pathprep.enums.SessionStatus;
import com.swayam.pathprep.event.SessionCompletedEvent;
import com.swayam.pathprep.filter.TraceIdFilter;
import com.swayam.pathprep.repository.*;
import com.swayam.pathprep.security.CurrentUser;
import com.swayam.pathprep.util.AppDay;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class DailyPlanIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private TraceIdFilter traceIdFilter;

    @Autowired
    private DailyPlanItemRepository dailyPlanItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private RoleTrackRepository roleTrackRepository;

    @Autowired
    private PracticeSessionRepository practiceSessionRepository;

    @Autowired
    private CurrentUser currentUser;

    @Autowired
    private AppDay appDay;

    @Autowired
    private Clock clock;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        dailyPlanItemRepository.deleteAll();
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilter(traceIdFilter)
                .build();
    }

    @Test
    void getPlan_whenEmpty_returnsEmptyChecklistWithZeroStats() throws Exception {
        mockMvc.perform(get("/api/v1/me/plan")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(appDay.today().toString()))
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.completedItems").value(0))
                .andExpect(jsonPath("$.progressPercent").value(0))
                .andExpect(jsonPath("$.hasUnfinishedYesterdayTasks").value(false))
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void addCustomTask_succeedsAndCalculatesProgress() throws Exception {
        CreateDailyPlanItemRequest customReq = new CreateDailyPlanItemRequest(
                DailyPlanItemType.CUSTOM,
                "Solve 3 Tree questions on LeetCode",
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/me/plan/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Solve 3 Tree questions on LeetCode"))
                .andExpect(jsonPath("$.itemType").value("CUSTOM"))
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.sortOrder").value(0));

        mockMvc.perform(get("/api/v1/me/plan")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.completedItems").value(0))
                .andExpect(jsonPath("$.progressPercent").value(0))
                .andExpect(jsonPath("$.items", hasSize(1)));
    }

    @Test
    void addQuizTask_succeedsWithDefaultTitleAndAutoPopulatedTrack() throws Exception {
        Topic topic = topicRepository.findAll().get(0);

        CreateDailyPlanItemRequest quizReq = new CreateDailyPlanItemRequest(
                DailyPlanItemType.QUIZ,
                null,
                null,
                topic.getId(),
                8
        );

        mockMvc.perform(post("/api/v1/me/plan/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(quizReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title", containsString(topic.getName())))
                .andExpect(jsonPath("$.itemType").value("QUIZ"))
                .andExpect(jsonPath("$.topicId").value(topic.getId()))
                .andExpect(jsonPath("$.topicName").value(topic.getName()))
                .andExpect(jsonPath("$.questionCount").value(8))
                .andExpect(jsonPath("$.completed").value(false));
    }

    @Test
    void addItem_validationFailures() throws Exception {
        // 1. CUSTOM without title
        CreateDailyPlanItemRequest blankCustom = new CreateDailyPlanItemRequest(
                DailyPlanItemType.CUSTOM,
                "   ",
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/me/plan/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankCustom)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        // 2. QUIZ without topicId
        CreateDailyPlanItemRequest quizNoTopic = new CreateDailyPlanItemRequest(
                DailyPlanItemType.QUIZ,
                "Quiz",
                null,
                null,
                5
        );

        mockMvc.perform(post("/api/v1/me/plan/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(quizNoTopic)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        // 3. QUIZ with invalid topicId
        CreateDailyPlanItemRequest quizInvalidTopic = new CreateDailyPlanItemRequest(
                DailyPlanItemType.QUIZ,
                "Quiz",
                null,
                999999L,
                5
        );

        mockMvc.perform(post("/api/v1/me/plan/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(quizInvalidTopic)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TOPIC_NOT_FOUND"));
    }

    @Test
    void toggleItem_updatesStatusAndCompletedAt() throws Exception {
        CreateDailyPlanItemRequest req = new CreateDailyPlanItemRequest(
                DailyPlanItemType.CUSTOM,
                "Review System Design notes",
                null,
                null,
                null
        );

        String createRes = mockMvc.perform(post("/api/v1/me/plan/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long itemId = objectMapper.readTree(createRes).get("id").asLong();

        // 1. Toggle to completed
        mockMvc.perform(patch("/api/v1/me/plan/items/{id}/toggle", itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(itemId))
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());

        // Verify summary statistics
        mockMvc.perform(get("/api/v1/me/plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.completedItems").value(1))
                .andExpect(jsonPath("$.progressPercent").value(100));

        // 2. Toggle back to uncompleted
        mockMvc.perform(patch("/api/v1/me/plan/items/{id}/toggle", itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.completedAt").doesNotExist());

        // 3. Toggle non-existent item returns 404
        mockMvc.perform(patch("/api/v1/me/plan/items/{id}/toggle", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLAN_ITEM_NOT_FOUND"));
    }

    @Test
    void deleteItem_removesFromChecklist() throws Exception {
        CreateDailyPlanItemRequest req = new CreateDailyPlanItemRequest(
                DailyPlanItemType.CUSTOM,
                "Item to be deleted",
                null,
                null,
                null
        );

        String createRes = mockMvc.perform(post("/api/v1/me/plan/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long itemId = objectMapper.readTree(createRes).get("id").asLong();

        // Delete item
        mockMvc.perform(delete("/api/v1/me/plan/items/{id}", itemId))
                .andExpect(status().isNoContent());

        // Verify item removed from plan
        mockMvc.perform(get("/api/v1/me/plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.items", hasSize(0)));

        // Deleting non-existent item returns 404
        mockMvc.perform(delete("/api/v1/me/plan/items/{id}", itemId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLAN_ITEM_NOT_FOUND"));
    }

    @Test
    void rolloverYesterdayTasks_importsUnfinishedTasks() throws Exception {
        Long userId = currentUser.getUserId();
        User user = userRepository.getReferenceById(userId);
        LocalDate yesterday = appDay.today().minusDays(1);

        // Seed unfinished task yesterday
        DailyPlanItem yesterdayItem = DailyPlanItem.builder()
                .user(user)
                .planDate(yesterday)
                .title("Unfinished task from yesterday")
                .itemType(DailyPlanItemType.CUSTOM)
                .completed(false)
                .sortOrder(0)
                .createdAt(clock.instant())
                .build();
        dailyPlanItemRepository.save(yesterdayItem);

        // Seed finished task yesterday (should NOT be rolled over)
        DailyPlanItem yesterdayDoneItem = DailyPlanItem.builder()
                .user(user)
                .planDate(yesterday)
                .title("Finished task from yesterday")
                .itemType(DailyPlanItemType.CUSTOM)
                .completed(true)
                .completedAt(clock.instant())
                .sortOrder(1)
                .createdAt(clock.instant())
                .build();
        dailyPlanItemRepository.save(yesterdayDoneItem);

        // Today's plan shows hasUnfinishedYesterdayTasks = true
        mockMvc.perform(get("/api/v1/me/plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasUnfinishedYesterdayTasks").value(true))
                .andExpect(jsonPath("$.totalItems").value(0));

        // Perform rollover
        mockMvc.perform(post("/api/v1/me/plan/rollover"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Unfinished task from yesterday"))
                .andExpect(jsonPath("$.items[0].completed").value(false));
    }

    @Test
    void sessionCompletedEvent_autoCompletesMatchingQuizTask() throws Exception {
        Long userId = currentUser.getUserId();
        User user = userRepository.getReferenceById(userId);
        Topic topic = topicRepository.findAll().get(0);
        RoleTrack track = roleTrackRepository.findAll().get(0);

        // Add a QUIZ plan item for today
        DailyPlanItem quizItem = DailyPlanItem.builder()
                .user(user)
                .planDate(appDay.today())
                .title("Quiz: " + topic.getName())
                .itemType(DailyPlanItemType.QUIZ)
                .track(track)
                .topic(topic)
                .questionCount(5)
                .completed(false)
                .sortOrder(0)
                .createdAt(clock.instant())
                .build();
        quizItem = dailyPlanItemRepository.save(quizItem);

        // Create practice session in DB to bind
        PracticeSession session = new PracticeSession();
        session.setUser(user);
        session.setTrack(track);
        session.setMode(SessionMode.QUIZ);
        session.setDifficulty(QuizDifficulty.MIXED);
        session.setStatus(SessionStatus.COMPLETED);
        session.setTotalQuestions(5);
        session.setCurrentIndex(5);
        session.setStartedAt(clock.instant().minusSeconds(300));
        session.setCompletedAt(clock.instant());
        session = practiceSessionRepository.save(session);

        // Publish SessionCompletedEvent with matching topic
        SessionCompletedEvent event = new SessionCompletedEvent(
                userId,
                session.getId(),
                track.getId(),
                clock.instant(),
                List.of(new SessionCompletedEvent.TopicResult(topic.getId(), 5, 4))
        );
        eventPublisher.publishEvent(event);

        // Verify that the QUIZ task was auto-completed!
        mockMvc.perform(get("/api/v1/me/plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.completedItems").value(1))
                .andExpect(jsonPath("$.progressPercent").value(100))
                .andExpect(jsonPath("$.items[0].id").value(quizItem.getId()))
                .andExpect(jsonPath("$.items[0].completed").value(true))
                .andExpect(jsonPath("$.items[0].sessionId").value(session.getId()));
    }

    @Test
    void getTodayAlgorithmicPlan_backwardCompatibilityVerified() throws Exception {
        mockMvc.perform(get("/api/v1/me/plan/today")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").exists())
                .andExpect(jsonPath("$.streak").exists())
                .andExpect(jsonPath("$.items").isArray());
    }
}
