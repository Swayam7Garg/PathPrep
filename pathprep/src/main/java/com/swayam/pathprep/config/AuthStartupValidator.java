package com.swayam.pathprep.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthStartupValidator {

    private final AuthProperties authProperties;
    private final Environment environment;

    @PostConstruct
    public void validate() {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        boolean isDevOrTest = activeProfiles.contains("dev") || activeProfiles.contains("test");

        if (isDevOrTest) {
            log.info("Active profile in dev/test mode ({}). Relaxing production authentication startup constraints.", activeProfiles);
            return;
        }

        log.info("Validating production authentication startup configuration...");

        String secret = authProperties.getJwt().getSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("Production startup failed: app.auth.jwt.secret must be at least 32 bytes (256 bits).");
        }

        String clientId = authProperties.getGoogle().getClientId();
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException("Production startup failed: app.auth.google.client-id must not be blank.");
        }

        if (!authProperties.getRefreshCookie().isSecure()) {
            throw new IllegalStateException("Production startup failed: app.auth.refresh-cookie.secure must be true.");
        }

        if (authProperties.isDevLoginEnabled()) {
            throw new IllegalStateException("Production startup failed: app.auth.dev-login-enabled must be false.");
        }

        log.info("Production authentication configuration validated successfully.");
    }
}
