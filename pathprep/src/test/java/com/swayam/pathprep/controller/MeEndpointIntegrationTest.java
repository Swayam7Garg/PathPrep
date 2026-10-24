package com.swayam.pathprep.controller;

import com.swayam.pathprep.TestAuth;
import com.swayam.pathprep.config.TestAuthConfiguration;
import com.swayam.pathprep.dto.request.DeleteUserRequest;
import com.swayam.pathprep.dto.request.SessionCreateRequest;
import com.swayam.pathprep.dto.request.UpdateUserRequest;
import com.swayam.pathprep.entity.PracticeSession;
import com.swayam.pathprep.entity.User;
import com.swayam.pathprep.enums.SessionMode;
import com.swayam.pathprep.enums.SessionStatus;
import com.swayam.pathprep.enums.UserRole;
import com.swayam.pathprep.filter.TraceIdFilter;
import com.swayam.pathprep.repository.PracticeSessionRepository;
import com.swayam.pathprep.repository.UserRepository;
import com.swayam.pathprep.service.MeService;
import com.swayam.pathprep.service.SessionService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Import(TestAuthConfiguration.class)
class MeEndpointIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private TraceIdFilter traceIdFilter;
    @Autowired
    private TestAuth testAuth;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SessionService sessionService;
    @Autowired
    private PracticeSessionRepository sessionRepository;
    @Autowired
    private EntityManager entityManager;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .addFilter(traceIdFilter)
                .build();

        userA = userRepository.findByEmail("usera@nie.edu.in").orElseGet(() -> {
            User u = new User();
            u.setEmail("usera@nie.edu.in");
            u.setName("User A");
            u.setRole(UserRole.STUDENT);
            return userRepository.save(u);
        });
        userA.setBranch(null);
        userA.setGradYear(null);
        userA.setTargetTrackId(null);
        userA = userRepository.save(userA);

        userB = userRepository.findByEmail("userb@nie.edu.in").orElseGet(() -> {
            User u = new User();
            u.setEmail("userb@nie.edu.in");
            u.setName("User B");
            u.setRole(UserRole.STUDENT);
            return userRepository.save(u);
        });

        sessionRepository.findFirstByUserIdAndStatus(userA.getId(), SessionStatus.IN_PROGRESS).ifPresent(s -> {
            s.setStatus(SessionStatus.ABANDONED);
            s.setCompletedAt(java.time.Instant.now());
            sessionRepository.save(s);
        });
        sessionRepository.findFirstByUserIdAndStatus(userB.getId(), SessionStatus.IN_PROGRESS).ifPresent(s -> {
            s.setStatus(SessionStatus.ABANDONED);
            s.setCompletedAt(java.time.Instant.now());
            sessionRepository.save(s);
        });
    }

    @Test
    void getMe_andPatchMe_fieldValidationAndOnboardingCompleted() throws Exception {
        String tokenA = testAuth.tokenFor(userA.getId(), userA.getRole());

        // Initial GET /me
        mockMvc.perform(get("/api/v1/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(userA.getEmail()))
                .andExpect(jsonPath("$.onboardingCompleted").value(false));

        // Invalid gradYear -> 400
        int invalidYear = LocalDate.now().getYear() + 10;
        mockMvc.perform(patch("/api/v1/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gradYear\": " + invalidYear + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_GRAD_YEAR"));

        // Invalid targetTrackId -> 404
        mockMvc.perform(patch("/api/v1/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetTrackId\": 999999}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRACK_NOT_FOUND"));

        // Unknown fields rejected -> 400 MALFORMED_REQUEST
        mockMvc.perform(patch("/api/v1/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unknownField\": \"hack\", \"name\": \"Valid Name\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

        // Valid PATCH /me -> completes onboarding
        int validYear = LocalDate.now().getYear() + 2;
        String patchBody = """
                {
                    "name": "Updated User A",
                    "branch": "Computer Science",
                    "gradYear": %d,
                    "targetTrackId": 1
                }
                """.formatted(validYear);

        mockMvc.perform(patch("/api/v1/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated User A"))
                .andExpect(jsonPath("$.branch").value("Computer Science"))
                .andExpect(jsonPath("$.gradYear").value(validYear))
                .andExpect(jsonPath("$.targetTrackId").value(1))
                .andExpect(jsonPath("$.onboardingCompleted").value(true));
    }

    @Test
    void ownershipMatrix_userBCannotAccessUserAResources() throws Exception {
        String tokenA = testAuth.tokenFor(userA.getId(), userA.getRole());
        String tokenB = testAuth.tokenFor(userB.getId(), userB.getRole());

        // Create a session for User A
        MvcResult sessionResult = mockMvc.perform(post("/api/v1/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trackId\": 1, \"mode\": \"QUIZ\", \"questionCount\": 2}"))
                .andExpect(status().isCreated())
                .andReturn();

        Long sessionAId = objectMapper.readTree(sessionResult.getResponse().getContentAsString())
                .path("id").asLong();

        // User B tries to GET User A's session -> 404 SESSION_NOT_FOUND
        mockMvc.perform(get("/api/v1/sessions/" + sessionAId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"));

        // User B tries to complete User A's session -> 404
        mockMvc.perform(post("/api/v1/sessions/" + sessionAId + "/complete")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"));

        // User B gets /sessions -> User A's session does NOT appear
        MvcResult listResult = mockMvc.perform(get("/api/v1/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andReturn();

        String body = listResult.getResponse().getContentAsString();
        assertThat(body).doesNotContain("\"id\":" + sessionAId);
    }

    @Test
    void deleteMe_cascadesAllTables_andPassesForeignKeyCoverageCheck() throws Exception {
        User userToDelete = new User();
        userToDelete.setEmail("delete-me-test@nie.edu.in");
        userToDelete.setName("Delete Target");
        userToDelete.setRole(UserRole.STUDENT);
        User savedUser = userRepository.save(userToDelete);

        String deleteToken = testAuth.tokenFor(savedUser.getId(), savedUser.getRole());

        // Invalid confirm -> 400 VALIDATION_FAILED
        mockMvc.perform(delete("/api/v1/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + deleteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirm\": \"NO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        // Successful DELETE -> 204
        mockMvc.perform(delete("/api/v1/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + deleteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirm\": \"DELETE\"}"))
                .andExpect(status().isNoContent())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE));

        assertThat(userRepository.findById(savedUser.getId())).isEmpty();

        // Other users remain intact
        assertThat(userRepository.findById(userA.getId())).isPresent();
        assertThat(userRepository.findById(userB.getId())).isPresent();
    }

    @Test
    void informationSchema_foreignKeyCoverageTest() {
        // Queries MySQL information_schema for every table with a foreign key referencing users.id
        // and asserts that MeService.HANDLED_USER_FK_TABLES covers every single one of them.
        @SuppressWarnings("unchecked")
        List<String> fkTables = entityManager.createNativeQuery("""
                SELECT DISTINCT TABLE_NAME
                FROM information_schema.KEY_COLUMN_USAGE
                WHERE REFERENCED_TABLE_NAME = 'users'
                  AND TABLE_SCHEMA = DATABASE()
                """).getResultList();

        for (String fkTable : fkTables) {
            assertThat(MeService.HANDLED_USER_FK_TABLES)
                    .as("Table '%s' has a foreign key to users but is not handled in MeService deletion cascade!", fkTable)
                    .contains(fkTable.toLowerCase());
        }
    }
}
