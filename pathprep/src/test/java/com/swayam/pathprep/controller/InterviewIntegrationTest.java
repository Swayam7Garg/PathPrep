package com.swayam.pathprep.controller;

import tools.jackson.databind.ObjectMapper;
import com.swayam.pathprep.entity.*;
import com.swayam.pathprep.enums.*;
import com.swayam.pathprep.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class InterviewIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleTrackRepository roleTrackRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private TrackTopicRepository trackTopicRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private RubricRepository rubricRepository;

    @Autowired
    private RubricCriterionRepository rubricCriterionRepository;

    @Autowired
    private PracticeSessionRepository practiceSessionRepository;

    private User devUser;
    private RoleTrack backendTrack;
    private Topic javaTopic;
    private Rubric testRubric;
    private Question conceptQuestion;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        practiceSessionRepository.findFirstByUserIdAndStatus(1L, SessionStatus.IN_PROGRESS)
                .ifPresent(active -> {
                    active.setStatus(SessionStatus.ABANDONED);
                    practiceSessionRepository.saveAndFlush(active);
                });
        devUser = userRepository.findById(1L).orElseGet(() -> {
            User u = new User();
            u.setEmail("dev@test.edu.in");
            u.setName("Dev Student");
            u.setRole(com.swayam.pathprep.enums.UserRole.STUDENT);
            return userRepository.save(u);
        });

        backendTrack = roleTrackRepository.findAll().stream()
                .filter(t -> "Java Backend".equalsIgnoreCase(t.getName()))
                .findFirst()
                .orElseGet(() -> {
                    RoleTrack t = new RoleTrack();
                    t.setName("Java Backend");
                    t.setDescription("Backend engineering track");
                    return roleTrackRepository.save(t);
                });

        javaTopic = topicRepository.findAll().stream()
                .filter(top -> "Spring Core".equalsIgnoreCase(top.getName()))
                .findFirst()
                .orElseGet(() -> {
                    Topic top = new Topic();
                    top.setName("Spring Core");
                    top.setDomain("Java");
                    return topicRepository.save(top);
                });

        if (!trackTopicRepository.existsById(new TrackTopicId(backendTrack.getId(), javaTopic.getId()))) {
            TrackTopic tt = new TrackTopic();
            tt.setId(new TrackTopicId(backendTrack.getId(), javaTopic.getId()));
            tt.setTrack(backendTrack);
            tt.setTopic(javaTopic);
            tt.setWeight(BigDecimal.valueOf(1.0));
            trackTopicRepository.save(tt);
        }

        testRubric = rubricRepository.findAll().stream().findFirst().orElseGet(() -> {
            Rubric r = new Rubric();
            r.setName("Test Rubric");
            r.setVersion(1);
            r = rubricRepository.save(r);

            RubricCriterion c = new RubricCriterion();
            c.setRubric(r);
            c.setName("Technical Accuracy");
            c.setDescription("Correctness of core concepts");
            c.setWeight(BigDecimal.valueOf(1.0));
            rubricCriterionRepository.save(c);

            return r;
        });

        conceptQuestion = new Question();
        conceptQuestion.setTopic(javaTopic);
        conceptQuestion.setRubricId(testRubric.getId());
        conceptQuestion.setType(QuestionType.CONCEPT);
        conceptQuestion.setDifficulty((byte) 1);
        conceptQuestion.setBody("Explain Inversion of Control (IoC) and Dependency Injection in Spring Boot.");
        conceptQuestion.setModelAnswer("IoC transfers control of object lifecycle and dependencies to a container. Dependency Injection is the mechanism where the container injects dependencies into components.");
        conceptQuestion.setKeyPoints("[\"IoC Container manages beans\", \"Loose coupling\", \"DI via constructor or field\"]");
        conceptQuestion.setStatus(QuestionStatus.PUBLISHED);
        conceptQuestion = questionRepository.save(conceptQuestion);
    }

    @Test
    @DisplayName("Create interview session and submit free-text answer with mock evaluation")
    void interviewSession_createsAndGradesTextAnswer() throws Exception {
        // 1. Create interview session
        String createJson = """
                {
                  "mode": "INTERVIEW",
                  "style": "LITE",
                  "trackId": %d,
                  "topicIds": [%d],
                  "questionCount": 1
                }
                """.formatted(backendTrack.getId(), javaTopic.getId());

        String sessionResp = mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.totalQuestions").value(1))
                .andReturn().getResponse().getContentAsString();

        tools.jackson.databind.JsonNode rootNode = new tools.jackson.databind.ObjectMapper().readTree(sessionResp);
        long sessionId = rootNode.path("id").asLong();

        // 2. Submit text answer
        String idempotencyKey = UUID.randomUUID().toString();
        String answerJson = """
                {
                  "answerText": "Inversion of control means the framework controls flow of program and instantiates beans instead of the programmer doing it directly. Dependency injection injects objects via constructors.",
                  "idempotencyKey": "%s",
                  "timeSpentSec": 45
                }
                """.formatted(idempotencyKey);

        mockMvc.perform(post("/api/v1/sessions/" + sessionId + "/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(answerJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correct").value(true))
                .andExpect(jsonPath("$.sessionStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.explanation").isNotEmpty());

        // 3. Poll evaluation
        mockMvc.perform(get("/api/v1/sessions/" + sessionId + "/evaluations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("DONE"))
                .andExpect(jsonPath("$[0].overallScore").value(85.0))
                .andExpect(jsonPath("$[0].criteria", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Usage quota endpoint returns correct initial quota usage and BYOK save")
    void usageEndpoints_verifyBYOKFlow() throws Exception {
        mockMvc.perform(get("/api/v1/me/usage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyFreeCallLimit").value(15))
                .andExpect(jsonPath("$.hasByok").value(false));

        String keyJson = """
                {
                  "apiKey": "gsk_test_mock_groq_api_key_12345678",
                  "provider": "GROQ"
                }
                """;

        mockMvc.perform(post("/api/v1/me/llm-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(keyJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("API key saved successfully"));

        mockMvc.perform(get("/api/v1/me/usage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasByok").value(true))
                .andExpect(jsonPath("$.keyHint").value("...5678"));

        mockMvc.perform(delete("/api/v1/me/llm-key"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/me/usage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasByok").value(false));
    }
}
