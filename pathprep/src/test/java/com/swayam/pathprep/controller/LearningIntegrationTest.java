package com.swayam.pathprep.controller;

import com.swayam.pathprep.entity.*;
import com.swayam.pathprep.enums.QuizDifficulty;
import com.swayam.pathprep.enums.ReviewStatus;
import com.swayam.pathprep.enums.SessionMode;
import com.swayam.pathprep.enums.SessionStatus;
import com.swayam.pathprep.filter.TraceIdFilter;
import com.swayam.pathprep.repository.*;
import com.swayam.pathprep.security.CurrentUser;
import com.swayam.pathprep.util.AppDay;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class LearningIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private TraceIdFilter traceIdFilter;

    @Autowired
    private CurrentUser currentUser;

    @Autowired
    private ReviewQueueRepository reviewQueueRepository;

    @Autowired
    private ReviewAttemptRepository reviewAttemptRepository;

    @Autowired
    private SkillMasteryRepository skillMasteryRepository;

    @Autowired
    private DailyActivityRepository dailyActivityRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuestionOptionRepository questionOptionRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AttemptRepository attemptRepository;

    @Autowired
    private PracticeSessionRepository practiceSessionRepository;

    @Autowired
    private RoleTrackRepository roleTrackRepository;

    @Autowired
    private AppDay appDay;

    @Autowired
    private Clock clock;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilter(traceIdFilter)
                .build();
    }

    @Test
    void getSkills_returnsTopicSkills_andStrictBooleanNaming() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/me/skills?trackId=1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackId").value(1))
                .andExpect(jsonPath("$.topics").isArray())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        assertFalse(json.contains("\"isPrerequisitesMet\""), "Response must not contain 'isPrerequisitesMet'");
    }

    @Test
    void getSkillGaps_returnsRankedGaps_andValidatesLimit() throws Exception {
        mockMvc.perform(get("/api/v1/me/skills/gaps?trackId=1&limit=5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackId").value(1))
                .andExpect(jsonPath("$.gaps").isArray());

        // Limit validation: limit = 0 -> 400
        mockMvc.perform(get("/api/v1/me/skills/gaps?trackId=1&limit=0"))
                .andExpect(status().isBadRequest());

        // Limit validation: limit = 11 -> 400
        mockMvc.perform(get("/api/v1/me/skills/gaps?trackId=1&limit=11"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTodayPlan_returnsStreakReadinessAndItems() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/me/plan/today")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.streak").exists())
                .andExpect(jsonPath("$.streak.current").isNumber())
                .andExpect(jsonPath("$.streak.longest").isNumber())
                .andExpect(jsonPath("$.streak.activeToday").isBoolean())
                .andExpect(jsonPath("$.streak.atRisk").isBoolean())
                .andExpect(jsonPath("$.readiness").exists())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()", lessThanOrEqualTo(3)))
                .andReturn();

        String json = result.getResponse().getContentAsString();
        assertFalse(json.contains("\"isActiveToday\""), "Response must not contain 'isActiveToday'");
        assertFalse(json.contains("\"isAtRisk\""), "Response must not contain 'isAtRisk'");
    }

    @Test
    void getProgress_returnsExactRangeDaysArray() throws Exception {
        // range = 7
        mockMvc.perform(get("/api/v1/me/progress?range=7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.range").value(7))
                .andExpect(jsonPath("$.days").isArray())
                .andExpect(jsonPath("$.days.length()").value(7))
                .andExpect(jsonPath("$.totals").exists())
                .andExpect(jsonPath("$.recentSessions").isArray());

        // range = 30
        mockMvc.perform(get("/api/v1/me/progress?range=30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.range").value(30))
                .andExpect(jsonPath("$.days.length()").value(30));

        // invalid range -> 400
        mockMvc.perform(get("/api/v1/me/progress?range=15"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reviewQueue_dueEndpoint_andAnswerProgression() throws Exception {
        Long userId = currentUser.getUserId();
        LocalDate today = appDay.today();
        Instant now = clock.instant();

        User user = userRepository.findById(userId).orElseThrow();
        QuestionOption correctOption = questionOptionRepository.findAll().stream()
                .filter(QuestionOption::isCorrect)
                .findFirst()
                .orElseThrow();
        Question question = correctOption.getQuestion();
        QuestionOption option = correctOption;

        Attempt attempt = attemptRepository.findAll().stream().findFirst().orElseGet(() -> {
            RoleTrack track = roleTrackRepository.findAll().get(0);
            PracticeSession session = new PracticeSession();
            session.setUser(user);
            session.setTrack(track);
            session.setMode(SessionMode.QUIZ);
            session.setStatus(SessionStatus.IN_PROGRESS);
            session.setDifficulty(QuizDifficulty.MIXED);
            session.setTotalQuestions(1);
            session.setStartedAt(now);
            session = practiceSessionRepository.save(session);

            Attempt a = new Attempt();
            a.setSession(session);
            a.setUser(user);
            a.setQuestion(question);
            a.setSelectedOption(option);
            a.setCorrect(false);
            a.setTimeSpentSec(10);
            a.setAnsweredAt(now);
            a.setIdempotencyKey(UUID.randomUUID().toString());
            return attemptRepository.save(a);
        });

        // Create or reset a review item due today
        ReviewQueueItem item = reviewQueueRepository.findByUserIdAndQuestionId(userId, question.getId())
                .orElseGet(() -> {
                    ReviewQueueItem qi = new ReviewQueueItem();
                    qi.setUser(user);
                    qi.setQuestion(question);
                    qi.setCreatedAt(now);
                    return qi;
                });
        item.setLastAttempt(attempt);
        item.setStatus(ReviewStatus.PENDING);
        item.setRepetitionCount(0);
        item.setIntervalDays(1);
        item.setDueOn(today);
        item = reviewQueueRepository.save(item);

        // 1. GET /api/v1/me/review/due
        MvcResult dueResult = mockMvc.perform(get("/api/v1/me/review/due?limit=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dueCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.items").isArray())
                .andReturn();

        String dueJson = dueResult.getResponse().getContentAsString();
        // Ensure question sanitization: no correctAnswerId, no explanation, no isCorrect in options
        assertFalse(dueJson.contains("correctAnswerId"), "Must not expose correctAnswerId");
        assertFalse(dueJson.contains("explanation"), "Must not expose explanation");
        assertFalse(dueJson.contains("isCorrect"), "Must not expose isCorrect");

        // 2. Answer correctly

        String answerPayload = "{\"selectedOptionId\": " + correctOption.getId() + ", \"timeSpentSec\": 15}";
        MvcResult answerResult = mockMvc.perform(post("/api/v1/me/review/" + item.getId() + "/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(answerPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.schedule.step").value(1))
                .andExpect(jsonPath("$.schedule.nextDueOn").value(today.plusDays(3).toString()))
                .andExpect(jsonPath("$.schedule.graduated").value(false))
                .andReturn();

        String answerJson = answerResult.getResponse().getContentAsString();
        assertFalse(answerJson.contains("\"isCorrect\""), "Must not contain 'isCorrect'");
        assertFalse(answerJson.contains("\"isGraduated\""), "Must not contain 'isGraduated'");

        // 3. Trying to answer again when not due -> 409 Conflict
        mockMvc.perform(post("/api/v1/me/review/" + item.getId() + "/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(answerPayload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REVIEW_NOT_DUE"));

        // 4. Non-existent review -> 404 Not Found
        mockMvc.perform(post("/api/v1/me/review/9999999/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(answerPayload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REVIEW_NOT_FOUND"));
    }
}
