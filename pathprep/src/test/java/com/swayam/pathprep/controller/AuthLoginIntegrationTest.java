package com.swayam.pathprep.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.swayam.pathprep.config.AuthProperties;
import com.swayam.pathprep.config.TestAuthConfiguration;
import com.swayam.pathprep.dto.request.GoogleLoginRequest;
import com.swayam.pathprep.entity.User;
import com.swayam.pathprep.enums.UserRole;
import com.swayam.pathprep.exception.AuthException;
import com.swayam.pathprep.filter.TraceIdFilter;
import com.swayam.pathprep.repository.UserRepository;
import com.swayam.pathprep.security.FakeGoogleIdTokenVerifier;
import com.swayam.pathprep.security.GoogleIdTokenVerifier;
import com.swayam.pathprep.security.VerifiedGoogleUser;
import com.swayam.pathprep.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Import(TestAuthConfiguration.class)
class AuthLoginIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private TraceIdFilter traceIdFilter;
    @Autowired
    private GoogleIdTokenVerifier googleIdTokenVerifier;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthService authService;
    @Autowired
    private AuthProperties authProperties;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private FakeGoogleIdTokenVerifier fakeVerifier;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .addFilter(traceIdFilter)
                .build();
        this.fakeVerifier = (FakeGoogleIdTokenVerifier) googleIdTokenVerifier;
        this.fakeVerifier.reset();
    }

    @Test
    void loginHappyPath_createsStudent_setsSecureHttpOnlyCookie_noTokenInJson() throws Exception {
        String credential = "valid-google-credential-student1";
        fakeVerifier.registerToken(credential, new VerifiedGoogleUser(
                "google-sub-student-1",
                "freshstudent@nie.edu.in",
                true,
                "Fresh Student",
                "https://lh3.googleusercontent.com/photo.jpg"
        ));

        GoogleLoginRequest request = new GoogleLoginRequest(credential);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSec").value(900))
                .andExpect(jsonPath("$.user.email").value("freshstudent@nie.edu.in"))
                .andExpect(jsonPath("$.user.name").value("Fresh Student"))
                .andExpect(jsonPath("$.user.role").value("STUDENT"))
                .andExpect(jsonPath("$.user.institution.name").value("National Institute of Engineering"))
                .andExpect(jsonPath("$.user.onboardingCompleted").value(false))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andReturn();

        String setCookieHeader = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookieHeader).isNotNull();
        assertThat(setCookieHeader).contains("pp_refresh=");
        assertThat(setCookieHeader).contains("HttpOnly");
        assertThat(setCookieHeader).contains("Path=/api/v1/auth");
        assertThat(setCookieHeader).contains("SameSite=Lax");
    }

    @Test
    void secondLogin_reusesSameUser() throws Exception {
        String credential = "valid-google-credential-student2";
        fakeVerifier.registerToken(credential, new VerifiedGoogleUser(
                "google-sub-student-2",
                "reuse@nie.edu.in",
                true,
                "Reuse Student",
                null
        ));

        GoogleLoginRequest request = new GoogleLoginRequest(credential);

        MvcResult firstLogin = mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        Long userId1 = objectMapper.readTree(firstLogin.getResponse().getContentAsString())
                .path("user").path("id").asLong();

        MvcResult secondLogin = mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        Long userId2 = objectMapper.readTree(secondLogin.getResponse().getContentAsString())
                .path("user").path("id").asLong();

        assertThat(userId1).isEqualTo(userId2);
    }

    @Test
    void googleSub_linkedToExistingEmailUser() throws Exception {
        // User with email student@nie.edu.in exists from seed
        String credential = "valid-google-link";
        fakeVerifier.registerToken(credential, new VerifiedGoogleUser(
                "sub-link-12345",
                "student@nie.edu.in",
                true,
                "Dev Student",
                null
        ));

        GoogleLoginRequest request = new GoogleLoginRequest(credential);

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("student@nie.edu.in"));

        User updatedUser = userRepository.findByEmail("student@nie.edu.in").orElseThrow();
        assertThat(updatedUser.getGoogleSub()).isEqualTo("sub-link-12345");
    }

    @Test
    void concurrentFirstLogins_createsExactlyOneUser() throws Exception {
        String email = "concurrent-login@nie.edu.in";
        VerifiedGoogleUser googleUser = new VerifiedGoogleUser(
                "sub-concurrent-1",
                email,
                true,
                "Concurrent User",
                null
        );

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<AuthService.AuthLoginResult>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                startGate.await();
                fakeVerifier.registerToken("cred-" + Thread.currentThread().getName(), googleUser);
                return authService.loginWithGoogle("cred-" + Thread.currentThread().getName(), "Mozilla/5.0");
            }));
        }

        startGate.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        int successCount = 0;
        for (Future<AuthService.AuthLoginResult> future : futures) {
            try {
                AuthService.AuthLoginResult res = future.get();
                if (res != null) successCount++;
            } catch (Exception ignored) {}
        }

        assertThat(successCount).isGreaterThanOrEqualTo(1);
        long count = userRepository.findAll().stream().filter(u -> email.equalsIgnoreCase(u.getEmail())).count();
        assertThat(count).isEqualTo(1L);
    }

    @Test
    void domainRules_validation() throws Exception {
        // 1. Allowed institution domain -> 200
        fakeVerifier.registerToken("cred-inst", new VerifiedGoogleUser("sub-1", "user@nie.edu.in", true, "User", null));
        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GoogleLoginRequest("cred-inst"))))
                .andExpect(status().isOk());

        // 2. Disallowed domain -> 403 EMAIL_DOMAIN_NOT_ALLOWED
        fakeVerifier.registerToken("cred-disallowed", new VerifiedGoogleUser("sub-2", "stranger@gmail.com", true, "Stranger", null));
        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GoogleLoginRequest("cred-disallowed"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_DOMAIN_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").value("Please sign in with your college email"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());

        // 3. Allow-any flag set to true -> 200
        authProperties.setAllowAnyGoogleAccount(true);
        try {
            mockMvc.perform(post("/api/v1/auth/google")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new GoogleLoginRequest("cred-disallowed"))))
                    .andExpect(status().isOk());
        } finally {
            authProperties.setAllowAnyGoogleAccount(false);
        }

        // 4. Unverified email -> 401 GOOGLE_TOKEN_INVALID
        fakeVerifier.registerFailure("cred-unverified", AuthException.googleTokenInvalid());
        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GoogleLoginRequest("cred-unverified"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("GOOGLE_TOKEN_INVALID"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void adminPromotion_andOfficerPreservation() throws Exception {
        // Admin email in ADMIN_EMAILS -> promoted to ADMIN
        fakeVerifier.registerToken("cred-admin", new VerifiedGoogleUser("sub-admin-1", "admin@nie.edu.in", true, "Admin User", null));
        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GoogleLoginRequest("cred-admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("ADMIN"));

        // Existing OFFICER user logging in is NOT silently demoted to STUDENT
        User officer = userRepository.findByEmail("placement-officer@nie.edu.in").orElseGet(() -> {
            User u = new User();
            u.setEmail("placement-officer@nie.edu.in");
            u.setName("Officer Test");
            return u;
        });
        officer.setRole(UserRole.OFFICER);
        userRepository.save(officer);

        fakeVerifier.registerToken("cred-officer", new VerifiedGoogleUser("sub-off-1", "placement-officer@nie.edu.in", true, "Officer Test", null));
        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GoogleLoginRequest("cred-officer"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("OFFICER"));
    }

    @Test
    void secrets_areNeverLogged() throws Exception {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        rootLogger.addAppender(listAppender);

        String secretCredential = "super-secret-google-id-token-credential-xyz999";
        fakeVerifier.registerToken(secretCredential, new VerifiedGoogleUser("sub-sec-1", "secretuser@nie.edu.in", true, "Secret User", null));

        MvcResult result = mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GoogleLoginRequest(secretCredential))))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseBody).path("accessToken").asText();
        String cookieHeader = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);

        for (ILoggingEvent event : listAppender.list) {
            String logMsg = event.getFormattedMessage();
            assertThat(logMsg).doesNotContain(secretCredential);
            assertThat(logMsg).doesNotContain(accessToken);
            if (cookieHeader != null) {
                String tokenVal = cookieHeader.substring(cookieHeader.indexOf("=") + 1, cookieHeader.indexOf(";"));
                assertThat(logMsg).doesNotContain(tokenVal);
            }
        }
        rootLogger.detachAppender(listAppender);
    }
}
