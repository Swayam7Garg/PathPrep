package com.swayam.pathprep.filter;

import com.swayam.pathprep.config.AdminProperties;
import com.swayam.pathprep.dto.response.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;

/**
 * Admin authentication filter for {@code /api/v1/admin/**}.
 *
 * <p>Supports dual authentication: either X-Admin-Key (if configured and provided)
 * or standard Bearer JWT with ROLE_ADMIN.
 * <ul>
 *   <li>If key mode is disabled (blank key) -> ignore header and pass to next filter.</li>
 *   <li>If header is absent -> pass through to JWT filter.</li>
 *   <li>If header is present and wrong -> 401 ADMIN_KEY_INVALID.</li>
 *   <li>If valid -> authenticate as principal "admin-key" with ROLE_ADMIN.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Admin-Key";
    private static final String ADMIN_PATH_PREFIX = "/api/v1/admin";

    private final AdminProperties adminProperties;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(ADMIN_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String configuredKey = adminProperties.apiKey();

        // If admin key mode is disabled (blank), ignore header and continue down the chain
        if (configuredKey == null || configuredKey.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        String providedKey = request.getHeader(HEADER);
        // If header is absent, allow request to proceed to JWT authentication
        if (providedKey == null || providedKey.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        // Header is present: validate using constant-time comparison
        boolean valid = MessageDigest.isEqual(
                configuredKey.getBytes(StandardCharsets.UTF_8),
                providedKey.getBytes(StandardCharsets.UTF_8)
        );

        if (!valid) {
            log.warn("Admin endpoint rejected: wrong {} value for path={}", HEADER, request.getRequestURI());
            reject(response, 401, "ADMIN_KEY_INVALID", "Invalid " + HEADER);
            return;
        }

        // Authenticate principal as "admin-key" with ROLE_ADMIN
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin-key",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        String traceId = org.slf4j.MDC.get(TraceIdFilter.MDC_TRACE_ID_KEY);
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
        }

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(),
                new ApiErrorResponse(code, message, List.of(), null, traceId));
    }
}
