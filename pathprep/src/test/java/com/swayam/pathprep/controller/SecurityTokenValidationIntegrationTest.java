package com.swayam.pathprep.controller;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.swayam.pathprep.TestAuth;
import com.swayam.pathprep.config.AuthProperties;
import com.swayam.pathprep.config.TestAuthConfiguration;
import com.swayam.pathprep.enums.UserRole;
import com.swayam.pathprep.filter.TraceIdFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Import(TestAuthConfiguration.class)
class SecurityTokenValidationIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private TraceIdFilter traceIdFilter;
    @Autowired
    private TestAuth testAuth;
    @Autowired
    private AuthProperties authProperties;
    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .addFilter(traceIdFilter)
                .build();
    }

    @Test
    void accessToken_missing_returns401Unauthenticated_withTraceId() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void accessToken_expired_returns401TokenExpired_withTraceId() throws Exception {
        // Mint an expired JWT
        Instant now = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant exp = now.plus(15, ChronoUnit.MINUTES);
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("1")
                .claim("role", "STUDENT")
                .issuer(authProperties.getJwt().getIssuer())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(exp))
                .jwtID(UUID.randomUUID().toString())
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(new SecretKeySpec(authProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        String expiredToken = jwt.serialize();

        mockMvc.perform(get("/api/v1/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void accessToken_tamperedOrWrongSecret_returns401TokenInvalid() throws Exception {
        // Sign with a different secret
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("1")
                .claim("role", "STUDENT")
                .issuer(authProperties.getJwt().getIssuer())
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plus(15, ChronoUnit.MINUTES)))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(new SecretKeySpec("wrong-secret-key-that-is-at-least-32-bytes-long!".getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        String tamperedToken = jwt.serialize();

        mockMvc.perform(get("/api/v1/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void genericEndpointSecurity_assertsAllNonWhitelistedEndpointsRequireAuth() throws Exception {
        // Explicit whitelist of public paths with documented reasons:
        Set<String> publicWhitelist = Set.of(
                "/api/v1/auth/google",    // Public Google sign-in
                "/api/v1/auth/refresh",   // Public token refresh (authenticated via refresh cookie)
                "/api/v1/auth/logout",    // Public logout (authenticated via refresh cookie)
                "/api/v1/auth/dev-login", // Dev login in dev/test profiles
                "/api/v1/tracks",         // Public catalog tracks
                "/api/v1/tracks/{trackId}/topics", // Public catalog topics
                "/api/v1/health",         // Public health check
                "/error"                  // Spring error handling controller
        );

        Map<RequestMappingInfo, ?> map = handlerMapping.getHandlerMethods();
        for (RequestMappingInfo info : map.keySet()) {
            Set<String> patterns = info.getDirectPaths();
            if (patterns.isEmpty() && info.getPathPatternsCondition() != null) {
                patterns = info.getPathPatternsCondition().getPatternValues();
            }

            for (String pattern : patterns) {
                if (publicWhitelist.contains(pattern)) {
                    continue;
                }

                // Any non-whitelisted path must return 401 UNAUTHENTICATED when called without a token
                Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
                RequestMethod method = methods.isEmpty() ? RequestMethod.GET : methods.iterator().next();

                String testPath = pattern.replaceAll("\\{[^}]+\\}", "1");

                MvcResult result = mockMvc.perform(request(org.springframework.http.HttpMethod.valueOf(method.name()), testPath))
                        .andReturn();

                int status = result.getResponse().getStatus();
                assertThat(status)
                        .as("Non-whitelisted path '%s' [%s] must return 401 without token but returned %d", testPath, method, status)
                        .isEqualTo(401);
            }
        }
    }

    @Test
    void roleAuthorization_studentGets403OnAdmin_adminGets200() throws Exception {
        String studentToken = testAuth.tokenFor(1L, UserRole.STUDENT);
        String adminToken = testAuth.tokenFor(1L, UserRole.ADMIN);

        // STUDENT -> 403 FORBIDDEN on /admin/**
        mockMvc.perform(get("/api/v1/admin/questions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        // ADMIN JWT -> 200 OK
        mockMvc.perform(get("/api/v1/admin/questions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Valid X-Admin-Key -> 200 OK
        mockMvc.perform(get("/api/v1/admin/questions")
                        .header("X-Admin-Key", "test-admin-key"))
                .andExpect(status().isOk());

        // Wrong X-Admin-Key -> 401 ADMIN_KEY_INVALID
        mockMvc.perform(get("/api/v1/admin/questions")
                        .header("X-Admin-Key", "wrong-key"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ADMIN_KEY_INVALID"));
    }

    @Test
    void cors_allowedOriginGetsCredentialsHeaders_disallowedDoesNot() throws Exception {
        // Allowed origin: http://localhost:3000
        mockMvc.perform(options("/api/v1/tracks")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));

        // Disallowed origin: http://evil-site.com
        mockMvc.perform(options("/api/v1/tracks")
                        .header("Origin", "http://evil-site.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
