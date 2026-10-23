package com.swayam.pathprep.security;

import com.swayam.pathprep.config.CorsProperties;
import com.swayam.pathprep.exception.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CookieEndpointProtection {

    private final CorsProperties corsProperties;

    public void validate(HttpServletRequest request) {
        String xRequestedWith = request.getHeader("X-Requested-With");
        if (xRequestedWith == null || !"pathprep".equalsIgnoreCase(xRequestedWith.trim())) {
            log.warn("Cookie endpoint rejected: missing or invalid X-Requested-With header: {}", xRequestedWith);
            throw AuthException.originNotAllowed();
        }

        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isBlank()) {
            boolean allowed = corsProperties.getAllowedOrigins().stream()
                    .anyMatch(allowedOrigin -> allowedOrigin.equalsIgnoreCase(origin.trim()));
            if (!allowed) {
                log.warn("Cookie endpoint rejected: origin {} not in allowed origins {}", origin, corsProperties.getAllowedOrigins());
                throw AuthException.originNotAllowed();
            }
        }
    }
}
