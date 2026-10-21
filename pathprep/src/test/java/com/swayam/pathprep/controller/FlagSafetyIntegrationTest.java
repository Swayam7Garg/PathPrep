package com.swayam.pathprep.controller;

import com.jayway.jsonpath.JsonPath;
import com.swayam.pathprep.entity.Question;
import com.swayam.pathprep.entity.ReviewQueueItem;
import com.swayam.pathprep.entity.User;
import com.swayam.pathprep.enums.QuestionStatus;
import com.swayam.pathprep.enums.ReviewStatus;
import com.swayam.pathprep.enums.SessionStatus;
import com.swayam.pathprep.filter.AdminKeyFilter;
import com.swayam.pathprep.filter.TraceIdFilter;
import com.swayam.pathprep.repository.PracticeSessionRepository;
import com.swayam.pathprep.repository.QuestionRepository;
import com.swayam.pathprep.repository.ReviewQueueRepository;
import com.swayam.pathprep.repository.UserRepository;
import com.swayam.pathprep.repository.projection.QuestionCandidateRow;
import com.swayam.pathprep.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.admin.api-key=test-admin-secret"
})
class FlagSafetyIntegrationTest {

    private static final String ADMIN_KEY = "test-admin-secret";
    private static final long JAVA_BACKEND_TRACK_ID = 1L;

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private AdminKeyFilter adminKeyFilter;
    @Autowired
    private TraceIdFilter traceIdFilter;
    @Autowired
    private PracticeSessionRepository practiceSessionRepository;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private ReviewQueueRepository reviewQueueRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private com.swayam.pathprep.repository.QuestionOptionRepository questionOptionRepository;
    @Autowired
    private com.swayam.pathprep.repository.AttemptRepository attemptRepository;
    @Autowired
    private CurrentUser currentUser;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("UPDATE questions SET status = 'PUBLISHED', flag_reason = NULL WHERE id IN (1, 2)");
        practiceSessionRepository.findFirstByUserIdAndStatus(currentUser.getUserId(), SessionStatus.IN_PROGRESS)
                .ifPresent(s -> {
                    s.setStatus(SessionStatus.ABANDONED);
                    practiceSessionRepository.save(s);
                });

        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilter(traceIdFilter)
                .addFilter(adminKeyFilter)
                .build();
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        jdbcTemplate.update("UPDATE questions SET status = 'PUBLISHED', flag_reason = NULL WHERE id IN (1, 2)");
    }

    @Test
    void flagSafety_inFlightSessionAnswersSucceed_newSessionAndReviewDueExcludeFlaggedQuestion() throws Exception {
        // 1. Start a quiz session
        String sessionPayload = """
                {
                    "mode": "QUIZ",
                    "trackId": %d,
                    "questionCount": 1,
                    "difficulty": "MIXED"
                }
                """.formatted(JAVA_BACKEND_TRACK_ID);

        MvcResult sessionResult = mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sessionPayload))
                .andExpect(status().isCreated())
                .andReturn();

        String sessionJson = sessionResult.getResponse().getContentAsString();
        long sessionId = ((Number) JsonPath.parse(sessionJson).read("$.id")).longValue();
        long questionId = ((Number) JsonPath.parse(sessionJson).read("$.currentQuestion.id")).longValue();
        long optionId = ((Number) JsonPath.parse(sessionJson).read("$.currentQuestion.options[0].id")).longValue();

        // Queue this question in review queue for the user due in the past
        User user = userRepository.findById(currentUser.getUserId()).orElseThrow();
        Question question = questionRepository.findById(questionId).orElseThrow();
        com.swayam.pathprep.entity.PracticeSession session = practiceSessionRepository.findById(sessionId).orElseThrow();

        com.swayam.pathprep.entity.Attempt attempt = new com.swayam.pathprep.entity.Attempt();
        attempt.setUser(user);
        attempt.setSession(session);
        attempt.setQuestion(question);
        attempt.setSelectedOption(questionOptionRepository.findById(optionId).orElseThrow());
        attempt.setCorrect(false);
        attempt.setTimeSpentSec(5);
        attempt.setIdempotencyKey(UUID.randomUUID().toString());
        attempt.setAnsweredAt(java.time.Instant.now());
        com.swayam.pathprep.entity.Attempt savedAttempt = attemptRepository.save(attempt);

        ReviewQueueItem reviewItem = reviewQueueRepository.findByUserIdAndQuestionId(user.getId(), questionId)
                .orElseGet(() -> {
                    ReviewQueueItem item = new ReviewQueueItem();
                    item.setUser(user);
                    item.setQuestion(question);
                    item.setLastAttempt(savedAttempt);
                    item.setCreatedAt(java.time.Instant.now());
                    return item;
                });
        reviewItem.setStatus(ReviewStatus.PENDING);
        reviewItem.setDueOn(LocalDate.now().minusDays(1));
        reviewItem.setRepetitionCount(1);
        reviewItem.setIntervalDays(1);
        reviewQueueRepository.save(reviewItem);

        // Verify it is initially due
        long dueBefore = reviewQueueRepository.countDueByUserId(user.getId(), LocalDate.now());
        assertThat(dueBefore).isGreaterThanOrEqualTo(1L);

        try {
            // 2. Admin flags the question
            String flagPayload = """
                    {
                        "reason": "Flagged during test for safety verification"
                    }
                    """;

            mockMvc.perform(post("/api/v1/admin/questions/{id}/flag", questionId)
                            .header("X-Admin-Key", ADMIN_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(flagPayload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("FLAGGED")));

            Question flaggedQ = questionRepository.findById(questionId).orElseThrow();
            assertThat(flaggedQ.getStatus()).isEqualTo(QuestionStatus.FLAGGED);

            // 3. Answer it in the in-flight quiz session -> WORKS! (flag-safety for in-progress sessions)
            String answerPayload = """
                    {
                        "selectedOptionId": %d,
                        "idempotencyKey": "%s"
                    }
                    """.formatted(optionId, UUID.randomUUID());

            mockMvc.perform(post("/api/v1/sessions/{id}/answers", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(answerPayload))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.correct").isBoolean())
                    .andExpect(jsonPath("$.sessionStatus", is("COMPLETED")));

            // 4. Start a new quiz session -> candidate questions MUST exclude the flagged question
            List<QuestionCandidateRow> candidates = questionRepository.findCandidateQuestions(List.of(question.getTopic().getId()), 0);
            List<Long> candidateIds = candidates.stream().map(QuestionCandidateRow::getId).toList();
            assertThat(candidateIds).doesNotContain(questionId);

            // 5. Review due list -> MUST exclude the flagged question
            mockMvc.perform(get("/api/v1/me/review/due")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(result -> {
                        String dueJson = result.getResponse().getContentAsString();
                        List<Number> dueQuestionIds = JsonPath.parse(dueJson).read("$.items[*].question.id");
                        List<Long> longQuestionIds = dueQuestionIds.stream().map(Number::longValue).toList();
                        assertThat(longQuestionIds).doesNotContain(questionId);
                    });

            long dueAfter = reviewQueueRepository.countDueByUserId(user.getId(), LocalDate.now());
            List<ReviewQueueItem> dueItemsAfter = reviewQueueRepository.findDueItems(user.getId(), LocalDate.now(), org.springframework.data.domain.PageRequest.of(0, 10));
            assertThat(dueItemsAfter.stream().map(i -> i.getQuestion().getId()).toList()).doesNotContain(questionId);
        } finally {
            // Restore question status so subsequent test suites aren't broken
            Question restoreQ = questionRepository.findById(questionId).orElse(null);
            if (restoreQ != null) {
                restoreQ.setStatus(QuestionStatus.PUBLISHED);
                restoreQ.setFlagReason(null);
                questionRepository.save(restoreQ);
            }
            reviewQueueRepository.findByUserIdAndQuestionId(user.getId(), questionId)
                    .ifPresent(reviewQueueRepository::delete);
            attemptRepository.delete(savedAttempt);
        }
    }
}
