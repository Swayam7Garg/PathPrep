package com.swayam.pathprep.controller;

import com.swayam.pathprep.config.TestAuthConfiguration;
import com.swayam.pathprep.entity.RefreshToken;
import com.swayam.pathprep.entity.User;
import com.swayam.pathprep.enums.UserRole;
import com.swayam.pathprep.filter.TraceIdFilter;
import com.swayam.pathprep.repository.RefreshTokenRepository;
import com.swayam.pathprep.repository.UserRepository;
import com.swayam.pathprep.security.TokenService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Import(TestAuthConfiguration.class)
class TokenRotationIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private TraceIdFilter traceIdFilter;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private User testUser;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .addFilter(traceIdFilter)
                .build();

        testUser = userRepository.findByEmail("token-test@nie.edu.in").orElseGet(() -> {
            User u = new User();
            u.setEmail("token-test@nie.edu.in");
            u.setName("Token Test User");
            u.setRole(UserRole.STUDENT);
            return userRepository.save(u);
        });
    }

    @Test
    void refreshToken_rotatesToken_revokesOldToken() throws Exception {
        String rawToken = tokenService.createRefreshToken(testUser, "TestAgent/1.0");
        String oldHash = tokenService.hashToken(rawToken);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-Requested-With", "pathprep")
                        .cookie(new Cookie("pp_refresh", rawToken)))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSec").value(900))
                .andExpect(jsonPath("$.user.email").value(testUser.getEmail()))
                .andReturn();

        String newCookieHeader = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(newCookieHeader).contains("pp_refresh=");

        // Verify old token is revoked in DB
        RefreshToken oldToken = refreshTokenRepository.findByTokenHash(oldHash).orElseThrow();
        assertThat(oldToken.getRevokedAt()).isNotNull();
        assertThat(oldToken.getReplacedBy()).isNotNull();
    }

    @Test
    void refreshToken_reuseDetection_revokesEntireFamily() throws Exception {
        String rawToken = tokenService.createRefreshToken(testUser, "TestAgent/1.0");

        // Rotate once (valid)
        TokenService.RefreshResult rotated = tokenService.rotateRefreshToken(rawToken, "TestAgent/1.0");

        // Reuse old revoked token
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-Requested-With", "pathprep")
                        .cookie(new Cookie("pp_refresh", rawToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_REUSED"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        // Verify new token in the same family was also revoked by reuse detection
        entityManager.clear();
        String newHash = tokenService.hashToken(rotated.rawRefreshToken());
        RefreshToken newToken = refreshTokenRepository.findByTokenHash(newHash).orElseThrow();
        assertThat(newToken.getRevokedAt()).isNotNull();
    }

    @Test
    void refreshToken_expiredOrUnknown_returns401() throws Exception {
        // Unknown token
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-Requested-With", "pathprep")
                        .cookie(new Cookie("pp_refresh", "unknown-token-12345")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_INVALID"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        // Expired token
        String rawToken = tokenService.createRefreshToken(testUser, "TestAgent/1.0");
        String hash = tokenService.hashToken(rawToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash).orElseThrow();
        token.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
        refreshTokenRepository.save(token);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-Requested-With", "pathprep")
                        .cookie(new Cookie("pp_refresh", rawToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_INVALID"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
    }

    @Test
    void concurrentRefresh_withSameToken_yieldsExactlyOne200_andOne401() throws Exception {
        String rawToken = tokenService.createRefreshToken(testUser, "Concurrent/1.0");

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                startGate.await();
                try {
                    MvcResult mvcResult = mockMvc.perform(post("/api/v1/auth/refresh")
                                    .header("X-Requested-With", "pathprep")
                                    .cookie(new Cookie("pp_refresh", rawToken)))
                            .andReturn();
                    return mvcResult.getResponse().getStatus();
                } catch (Exception e) {
                    return 500;
                }
            }));
        }

        startGate.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> f : futures) {
            statuses.add(f.get());
        }

        assertThat(statuses).containsExactlyInAnyOrder(200, 401);
    }

    @Test
    void csrfProtection_checksOriginAndXRequestedWith() throws Exception {
        String rawToken = tokenService.createRefreshToken(testUser, "TestAgent/1.0");

        // Missing X-Requested-With -> 403 ORIGIN_NOT_ALLOWED
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("pp_refresh", rawToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ORIGIN_NOT_ALLOWED"));

        // Disallowed Origin -> 403 ORIGIN_NOT_ALLOWED
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-Requested-With", "pathprep")
                        .header("Origin", "http://malicious-attacker.com")
                        .cookie(new Cookie("pp_refresh", rawToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ORIGIN_NOT_ALLOWED"));

        // Allowed Origin and custom header -> 200 OK
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-Requested-With", "pathprep")
                        .header("Origin", "http://localhost:3000")
                        .cookie(new Cookie("pp_refresh", rawToken)))
                .andExpect(status().isOk());
    }

    @Test
    void logout_revokesFamilyAndClearsCookie_idempotent() throws Exception {
        String rawToken = tokenService.createRefreshToken(testUser, "TestAgent/1.0");
        String hash = tokenService.hashToken(rawToken);

        // Logout with cookie -> 204
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("X-Requested-With", "pathprep")
                        .cookie(new Cookie("pp_refresh", rawToken)))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        RefreshToken token = refreshTokenRepository.findByTokenHash(hash).orElseThrow();
        assertThat(token.getRevokedAt()).isNotNull();

        // Idempotent logout without cookie -> 204
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("X-Requested-With", "pathprep"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
    }

    @Test
    void logoutAll_revokesAllUserTokens() throws Exception {
        String token1 = tokenService.createRefreshToken(testUser, "Agent1");
        String token2 = tokenService.createRefreshToken(testUser, "Agent2");
        String accessToken = tokenService.createAccessToken(testUser.getId(), testUser.getRole());

        mockMvc.perform(post("/api/v1/auth/logout-all")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        entityManager.clear();
        RefreshToken r1 = refreshTokenRepository.findByTokenHash(tokenService.hashToken(token1)).orElseThrow();
        RefreshToken r2 = refreshTokenRepository.findByTokenHash(tokenService.hashToken(token2)).orElseThrow();
        assertThat(r1.getRevokedAt()).isNotNull();
        assertThat(r2.getRevokedAt()).isNotNull();
    }
}
