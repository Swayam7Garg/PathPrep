package com.swayam.pathprep.security;

import com.swayam.pathprep.config.AuthProperties;
import com.swayam.pathprep.config.AuthStartupValidator;
import com.swayam.pathprep.exception.RateLimitedException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthStartupAndRateLimitTest {

    @org.springframework.context.annotation.Configuration
    @org.springframework.boot.context.properties.EnableConfigurationProperties(AuthProperties.class)
    static class TestConfig {}

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class, AuthStartupValidator.class);

    @Test
    void startupValidation_failsInProd_whenJwtSecretTooShort() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "app.auth.jwt.secret=short-key",
                        "app.auth.google.client-id=my-client-id",
                        "app.auth.refresh-cookie.secure=true",
                        "app.auth.dev-login-enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining("app.auth.jwt.secret must be at least 32 bytes");
                });
    }

    @Test
    void startupValidation_failsInProd_whenGoogleClientIdBlank() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "app.auth.jwt.secret=super-secret-key-that-is-at-least-32-bytes-long!",
                        "app.auth.google.client-id=",
                        "app.auth.refresh-cookie.secure=true",
                        "app.auth.dev-login-enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining("app.auth.google.client-id must not be blank");
                });
    }

    @Test
    void startupValidation_failsInProd_whenCookieNotSecure() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "app.auth.jwt.secret=super-secret-key-that-is-at-least-32-bytes-long!",
                        "app.auth.google.client-id=valid-client-id",
                        "app.auth.refresh-cookie.secure=false",
                        "app.auth.dev-login-enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining("app.auth.refresh-cookie.secure must be true");
                });
    }

    @Test
    void startupValidation_failsInProd_whenDevLoginEnabledIsTrue() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "app.auth.jwt.secret=super-secret-key-that-is-at-least-32-bytes-long!",
                        "app.auth.google.client-id=valid-client-id",
                        "app.auth.refresh-cookie.secure=true",
                        "app.auth.dev-login-enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining("app.auth.dev-login-enabled must be false");
                });
    }

    @Test
    void startupValidation_succeedsInProd_whenAllConstraintsValid() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "app.auth.jwt.secret=super-secret-key-that-is-at-least-32-bytes-long!",
                        "app.auth.google.client-id=valid-client-id",
                        "app.auth.refresh-cookie.secure=true",
                        "app.auth.dev-login-enabled=false"
                )
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void rateLimiter_21stRequestInMinute_throws429WithRetryAfterSeconds() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-10-23T12:00:00Z"), ZoneId.of("UTC"));
        AuthRateLimiter rateLimiter = new AuthRateLimiter(fixedClock);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");

        // First 20 requests within 60 seconds are permitted
        for (int i = 0; i < 20; i++) {
            rateLimiter.checkRateLimit(request);
        }

        // 21st request must trigger RateLimitedException with retryAfterSeconds
        assertThatThrownBy(() -> rateLimiter.checkRateLimit(request))
                .isInstanceOf(RateLimitedException.class)
                .satisfies(ex -> {
                    RateLimitedException rle = (RateLimitedException) ex;
                    assertThat(rle.getCode()).isEqualTo("RATE_LIMITED");
                    assertThat(rle.getRetryAfterSeconds()).isGreaterThan(0L);
                });
    }
}
