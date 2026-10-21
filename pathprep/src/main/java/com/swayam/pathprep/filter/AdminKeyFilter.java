package com.swayam.pathprep.filter;

import tools.jackson.databind.ObjectMapper;
import com.swayam.pathprep.config.AdminProperties;
import com.swayam.pathprep.dto.response.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;

/**
 * Interim admin authentication filter for {@code /api/v1/admin/**}.
 *
 * <p>Compares the {@code X-Admin-Key} request header against the configured
 * {@code app.admin.api-key} property using {@link MessageDigest#isEqual} (constant-time) to
 * prevent timing attacks. The key value is NEVER logged.
 *
 * <p>Fail-closed behaviour:
 * <ul>
 *   <li>If {@code app.admin.api-key} is blank → 403 ADMIN_DISABLED on every admin request.</li>
 *   <li>If header is missing or wrong → 401 ADMIN_KEY_INVALID.</li>
 * </ul>
 *
 * <p>TODO: Remove this filter and replace with {@code @PreAuthorize("hasRole('ADMIN')")}
 *          once Phase 7 (Spring Security JWT) is implemented.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminKeyFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Admin-Key";
    private static final String ADMIN_PATH_PREFIX = "/api/v1/admin/";

    private final AdminProperties adminProperties;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only intercept admin paths; all other paths pass straight through
        return !request.getRequestURI().startsWith(ADMIN_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String configuredKey = adminProperties.apiKey();

        // Fail-closed: if no key is configured, all admin endpoints are disabled
        if (configuredKey == null || configuredKey.isBlank()) {
            log.warn("Admin endpoint requested but ADMIN_API_KEY is not configured; rejecting");
            reject(response, 403, "ADMIN_DISABLED",
                    "Admin endpoints are disabled on this server (no API key configured)");
            return;
        }

        String providedKey = request.getHeader(HEADER);
        if (providedKey == null) {
            log.warn("Admin endpoint requested without {} header; path={}", HEADER, request.getRequestURI());
            reject(response, 401, "ADMIN_KEY_INVALID", "Missing " + HEADER + " header");
            return;
        }

        // Constant-time comparison to prevent timing-based key enumeration
        boolean valid = MessageDigest.isEqual(
                configuredKey.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                providedKey.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        if (!valid) {
            log.warn("Admin endpoint rejected: wrong {} value; path={}", HEADER, request.getRequestURI());
            reject(response, 401, "ADMIN_KEY_INVALID", "Invalid " + HEADER);
            return;
        }

        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        // Derive a trace ID from MDC if the TraceIdFilter has already run
        String traceId = org.slf4j.MDC.get(TraceIdFilter.MDC_TRACE_ID_KEY);
        if (traceId == null) traceId = UUID.randomUUID().toString();

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(),
                new ApiErrorResponse(code, message, List.of(), null, traceId));
    }
}
