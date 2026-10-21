package com.swayam.pathprep.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Admin configuration properties bound from {@code app.admin.*}.
 *
 * <p>{@code apiKey} is the value of the {@code X-Admin-Key} header that all
 * {@code /api/v1/admin/**} endpoints require. Compared in constant time.
 * If blank, admin endpoints are fully disabled (fail-closed).
 *
 * <p>TODO: Replace with {@code ROLE_ADMIN} from Spring Security JWT in Phase 7.
 */
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(
        /** Value of the ADMIN_API_KEY environment variable. Blank means admin endpoints are disabled. */
        String apiKey
) {}
