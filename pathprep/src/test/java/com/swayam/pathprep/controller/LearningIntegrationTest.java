package com.swayam.pathprep.controller;

import com.swayam.pathprep.entity.*;
import com.swayam.pathprep.enums.QuizDifficulty;
import com.swayam.pathprep.enums.ReviewStatus;
import com.swayam.pathprep.enums.SessionMode;
import com.swayam.pathprep.enums.SessionStatus;
import com.swayam.pathprep.event.AttemptGradedEvent;
import com.swayam.pathprep.filter.TraceIdFilter;
import com.swayam.pathprep.repository.*;
import com.swayam.pathprep.security.CurrentUser;
import com.swayam.pathprep.util.AppDay;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class LearningIntegrationTest {

    public static volatile boolean failListenerDeliberately = false;

    @TestConfiguration
    static class FailingListenerConfig {
        @EventListener
        public void onGraded(AttemptGradedEvent event) {
            if (failListenerDeliberately) {
                throw new RuntimeException("Deliberate failure in test listener to test atomicity");
            }
        }
    }

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        failListenerDeliberately = false;
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilter(traceIdFilter)
                .build();

        jdbcTemplate.update("UPDATE sessions SET status = 'ABANDONED', completion_reason = 'ENDED_EARLY' WHERE status = 'IN_PROGRESS' AND user_id = ?",
                currentUser.getUserId());
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
                .andExpect(jsonPath("$.readiness.minAnswersRequired").isNumber())
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

    @Test
    void atomicity_whenEventListenerFails_answerFailsAndNoAttemptRowExists() throws Exception {
        Long userId = currentUser.getUserId();
        User user = userRepository.findById(userId).orElseThrow();
        RoleTrack track = roleTrackRepository.findAll().get(0);
        Question question = questionRepository.findAll().get(0);
        QuestionOption option = questionOptionRepository.findAll().stream()
                .filter(o -> o.getQuestion().getId().equals(question.getId()))
                .findFirst()
                .orElseThrow();

        PracticeSession session = new PracticeSession();
        session.setUser(user);
        session.setTrack(track);
        session.setMode(SessionMode.QUIZ);
        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setDifficulty(QuizDifficulty.MIXED);
        session.setTotalQuestions(3);
        session.setCurrentIndex(0);
        session.setStartedAt(clock.instant());
        session = practiceSessionRepository.save(session);

        SessionQuestion sq = new SessionQuestion(new SessionQuestionId(session.getId(), 0), session, question);
        session.getSessionQuestions().add(sq);
        practiceSessionRepository.save(session);

        String testKey = "atomicity-test-key-" + UUID.randomUUID();
        failListenerDeliberately = true;

        try {
            mockMvc.perform(post("/api/v1/sessions/" + session.getId() + "/answers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"selectedOptionId": %d, "idempotencyKey": "%s"}
                                    """.formatted(option.getId(), testKey)))
                    .andExpect(status().is5xxServerError());

            // Verify attempt was rolled back
            assertThat(attemptRepository.findByIdempotencyKey(testKey)).isEmpty();

            // Verify session did not advance
            PracticeSession reloaded = practiceSessionRepository.findById(session.getId()).orElseThrow();
            assertThat(reloaded.getCurrentIndex()).isEqualTo(0);
        } finally {
            failListenerDeliberately = false;
            try {
                practiceSessionRepository.delete(session);
            } catch (Exception ignored) {}
        }
    }

    @Test
    void replayWithSameIdempotencyKey_doesNotModifyMasteryOrActivityOrReviewQueue() throws Exception {
        Long userId = currentUser.getUserId();
        User user = userRepository.findById(userId).orElseThrow();
        RoleTrack track = roleTrackRepository.findAll().get(0);
        Question question = questionRepository.findAll().get(0);
        QuestionOption option = questionOptionRepository.findAll().stream()
                .filter(o -> o.getQuestion().getId().equals(question.getId()))
                .findFirst()
                .orElseThrow();

        PracticeSession session = new PracticeSession();
        session.setUser(user);
        session.setTrack(track);
        session.setMode(SessionMode.QUIZ);
        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setDifficulty(QuizDifficulty.MIXED);
        session.setTotalQuestions(3);
        session.setCurrentIndex(0);
        session.setStartedAt(clock.instant());
        session = practiceSessionRepository.save(session);

        SessionQuestion sq = new SessionQuestion(new SessionQuestionId(session.getId(), 0), session, question);
        session.getSessionQuestions().add(sq);
        practiceSessionRepository.save(session);

        String replayKey = "replay-key-" + UUID.randomUUID();

        // 1st request -> 201 Created
        mockMvc.perform(post("/api/v1/sessions/" + session.getId() + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"selectedOptionId": %d, "idempotencyKey": "%s"}
                                """.formatted(option.getId(), replayKey)))
                .andExpect(status().isCreated());

        LocalDate today = appDay.today();
        DailyActivity activity1 = dailyActivityRepository.findByUserIdAndActivityDate(userId, today).orElseThrow();
        int answeredCount1 = activity1.getQuestionsAnswered();

        SkillMastery mastery1 = skillMasteryRepository.findByUserIdAndTopicId(userId, question.getTopic().getId()).orElseThrow();
        int attemptsCount1 = mastery1.getAttemptsCount();

        // 2nd request with same idempotency key -> 201 Created (replay identical response)
        mockMvc.perform(post("/api/v1/sessions/" + session.getId() + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"selectedOptionId": %d, "idempotencyKey": "%s"}
                                """.formatted(option.getId(), replayKey)))
                .andExpect(status().isCreated());

        DailyActivity activity2 = dailyActivityRepository.findByUserIdAndActivityDate(userId, today).orElseThrow();
        assertThat(activity2.getQuestionsAnswered()).isEqualTo(answeredCount1);

        SkillMastery mastery2 = skillMasteryRepository.findByUserIdAndTopicId(userId, question.getTopic().getId()).orElseThrow();
        assertThat(mastery2.getAttemptsCount()).isEqualTo(attemptsCount1);

        try {
            practiceSessionRepository.delete(session);
        } catch (Exception ignored) {}
    }

    @Test
    void fullReviewLadderProgression_resetsGraduationAndReopensOnQuizWrongAnswer() throws Exception {
        Long userId = currentUser.getUserId();
        LocalDate today = appDay.today();
        User user = userRepository.findById(userId).orElseThrow();
        RoleTrack track = roleTrackRepository.findAll().get(0);

        QuestionOption correctOption = questionOptionRepository.findAll().stream()
                .filter(QuestionOption::isCorrect)
                .findFirst()
                .orElseThrow();
        Question question = correctOption.getQuestion();
        QuestionOption wrongOption = questionOptionRepository.findAll().stream()
                .filter(o -> o.getQuestion().getId().equals(question.getId()) && !o.isCorrect())
                .findFirst()
                .orElseThrow();
        Attempt initialAttempt = attemptRepository.findAll().stream().findFirst().orElse(null);

        ReviewQueueItem item = reviewQueueRepository.findByUserIdAndQuestionId(userId, question.getId())
                .orElseGet(() -> {
                    ReviewQueueItem q = new ReviewQueueItem();
                    q.setUser(user);
                    q.setQuestion(question);
                    q.setLastAttempt(initialAttempt);
                    q.setCreatedAt(clock.instant());
                    return q;
                });

        item.setStatus(ReviewStatus.PENDING);
        item.setRepetitionCount(0);
        item.setIntervalDays(1);
        item.setDueOn(today);
        item = reviewQueueRepository.save(item);

        String correctPayload = """
                {"selectedOptionId": %d, "timeSpentSec": 10}
                """.formatted(correctOption.getId());

        // Step 0 -> 1 (+3 days)
        mockMvc.perform(post("/api/v1/me/review/" + item.getId() + "/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(correctPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schedule.step").value(1))
                .andExpect(jsonPath("$.schedule.graduated").value(false));

        // Advance to today -> Step 1 -> 2 (+7 days)
        item = reviewQueueRepository.findById(item.getId()).orElseThrow();
        item.setDueOn(today);
        reviewQueueRepository.save(item);
        mockMvc.perform(post("/api/v1/me/review/" + item.getId() + "/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(correctPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schedule.step").value(2))
                .andExpect(jsonPath("$.schedule.graduated").value(false));

        // Advance to today -> Step 2 -> 3 (+14 days)
        item = reviewQueueRepository.findById(item.getId()).orElseThrow();
        item.setDueOn(today);
        reviewQueueRepository.save(item);
        mockMvc.perform(post("/api/v1/me/review/" + item.getId() + "/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(correctPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schedule.step").value(3))
                .andExpect(jsonPath("$.schedule.graduated").value(false));

        // Advance to today -> Step 3 -> 4 (graduated!)
        item = reviewQueueRepository.findById(item.getId()).orElseThrow();
        item.setDueOn(today);
        reviewQueueRepository.save(item);
        mockMvc.perform(post("/api/v1/me/review/" + item.getId() + "/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(correctPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schedule.step").value(4))
                .andExpect(jsonPath("$.schedule.graduated").value(true));

        ReviewQueueItem graduatedItem = reviewQueueRepository.findById(item.getId()).orElseThrow();
        assertThat(graduatedItem.getStatus()).isEqualTo(ReviewStatus.COMPLETED);
        assertThat(graduatedItem.getRepetitionCount()).isEqualTo(4);

        // Now simulate a subsequent wrong answer for this question in a QUIZ session
        PracticeSession quizSession = new PracticeSession();
        quizSession.setUser(user);
        quizSession.setTrack(track);
        quizSession.setMode(SessionMode.QUIZ);
        quizSession.setStatus(SessionStatus.IN_PROGRESS);
        quizSession.setDifficulty(QuizDifficulty.MIXED);
        quizSession.setTotalQuestions(1);
        quizSession.setCurrentIndex(0);
        quizSession.setStartedAt(clock.instant());
        quizSession = practiceSessionRepository.save(quizSession);

        SessionQuestion sq = new SessionQuestion(new SessionQuestionId(quizSession.getId(), 0), quizSession, question);
        quizSession.getSessionQuestions().add(sq);
        practiceSessionRepository.save(quizSession);

        mockMvc.perform(post("/api/v1/sessions/" + quizSession.getId() + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"selectedOptionId": %d, "idempotencyKey": "%s"}
                                """.formatted(wrongOption.getId(), UUID.randomUUID())))
                .andExpect(status().isCreated());

        // Verify that the graduated item has reopened: PENDING, step 0, due tomorrow (+1)
        ReviewQueueItem reopenedItem = reviewQueueRepository.findById(item.getId()).orElseThrow();
        assertThat(reopenedItem.getStatus()).isEqualTo(ReviewStatus.PENDING);
        assertThat(reopenedItem.getRepetitionCount()).isEqualTo(0);
        assertThat(reopenedItem.getDueOn()).isEqualTo(today.plusDays(1));

        try {
            practiceSessionRepository.delete(quizSession);
        } catch (Exception ignored) {}
    }

    @Test
    void concurrentReviewSubmissions_oneSucceedsOneFailsConflict() throws Exception {
        Long userId = currentUser.getUserId();
        LocalDate today = appDay.today();
        User user = userRepository.findById(userId).orElseThrow();

        QuestionOption correctOption = questionOptionRepository.findAll().stream()
                .filter(QuestionOption::isCorrect)
                .findFirst()
                .orElseThrow();
        Question question = correctOption.getQuestion();
        Attempt attempt = attemptRepository.findAll().stream().findFirst().orElse(null);

        ReviewQueueItem item = reviewQueueRepository.findByUserIdAndQuestionId(userId, question.getId())
                .orElseGet(() -> {
                    ReviewQueueItem q = new ReviewQueueItem();
                    q.setUser(user);
                    q.setQuestion(question);
                    q.setLastAttempt(attempt);
                    q.setCreatedAt(clock.instant());
                    return q;
                });

        item.setStatus(ReviewStatus.PENDING);
        item.setRepetitionCount(0);
        item.setIntervalDays(1);
        item.setDueOn(today);
        item = reviewQueueRepository.save(item);

        Long queueId = item.getId();
        Long optId = correctOption.getId();

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);
        List<Integer> statusCodes = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    MvcResult res = mockMvc.perform(post("/api/v1/me/review/" + queueId + "/answer")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            {"selectedOptionId": %d, "timeSpentSec": 10}
                                            """.formatted(optId)))
                            .andReturn();
                    statusCodes.add(res.getResponse().getStatus());
                } catch (Exception e) {
                    statusCodes.add(500);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(statusCodes).contains(200);
        assertThat(statusCodes).contains(409);
    }

    @Test
    void sessionsCompleted_incrementsOnce_neverForAbandoned_neverOnSecondComplete() throws Exception {
        Long userId = currentUser.getUserId();
        LocalDate today = appDay.today();
        User user = userRepository.findById(userId).orElseThrow();
        RoleTrack track = roleTrackRepository.findAll().get(0);

        int initialSessionsCompleted = dailyActivityRepository.findByUserIdAndActivityDate(userId, today)
                .map(DailyActivity::getSessionsCompleted)
                .orElse(0);

        // 1. Abandoned session (0 answers)
        PracticeSession s1 = new PracticeSession();
        s1.setUser(user);
        s1.setTrack(track);
        s1.setMode(SessionMode.QUIZ);
        s1.setStatus(SessionStatus.IN_PROGRESS);
        s1.setDifficulty(QuizDifficulty.MIXED);
        s1.setTotalQuestions(2);
        s1.setStartedAt(clock.instant());
        s1 = practiceSessionRepository.save(s1);

        mockMvc.perform(post("/api/v1/sessions/" + s1.getId() + "/complete"))
                .andExpect(status().isOk());

        int sessionsAfterAbandon = dailyActivityRepository.findByUserIdAndActivityDate(userId, today)
                .map(DailyActivity::getSessionsCompleted)
                .orElse(0);
        assertThat(sessionsAfterAbandon).isEqualTo(initialSessionsCompleted);

        // 2. Completed session (answered questions)
        PracticeSession s2 = new PracticeSession();
        s2.setUser(user);
        s2.setTrack(track);
        s2.setMode(SessionMode.QUIZ);
        s2.setStatus(SessionStatus.IN_PROGRESS);
        s2.setDifficulty(QuizDifficulty.MIXED);
        s2.setTotalQuestions(1);
        s2.setCurrentIndex(0);
        s2.setStartedAt(clock.instant());
        s2 = practiceSessionRepository.save(s2);

        Question q = questionRepository.findAll().get(0);
        QuestionOption qOpt = questionOptionRepository.findAll().stream()
                .filter(o -> o.getQuestion().getId().equals(q.getId()))
                .findFirst()
                .orElseThrow();
        SessionQuestion sq = new SessionQuestion(new SessionQuestionId(s2.getId(), 0), s2, q);
        s2.getSessionQuestions().add(sq);
        practiceSessionRepository.save(s2);

        mockMvc.perform(post("/api/v1/sessions/" + s2.getId() + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"selectedOptionId": %d, "idempotencyKey": "%s"}
                                """.formatted(qOpt.getId(), UUID.randomUUID())))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/sessions/" + s2.getId() + "/complete"))
                .andExpect(status().isOk());

        int sessionsAfterComplete = dailyActivityRepository.findByUserIdAndActivityDate(userId, today)
                .map(DailyActivity::getSessionsCompleted)
                .orElse(0);
        assertThat(sessionsAfterComplete).isEqualTo(initialSessionsCompleted + 1);

        // 3. Repeat complete on same session
        mockMvc.perform(post("/api/v1/sessions/" + s2.getId() + "/complete"))
                .andExpect(status().isOk());

        int sessionsAfterSecondComplete = dailyActivityRepository.findByUserIdAndActivityDate(userId, today)
                .map(DailyActivity::getSessionsCompleted)
                .orElse(0);
        assertThat(sessionsAfterSecondComplete).isEqualTo(initialSessionsCompleted + 1);

        try {
            practiceSessionRepository.delete(s2);
            practiceSessionRepository.delete(s1);
        } catch (Exception ignored) {}
    }
}
