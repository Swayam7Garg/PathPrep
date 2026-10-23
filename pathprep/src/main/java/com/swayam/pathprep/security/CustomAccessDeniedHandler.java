package com.swayam.pathprep.security;

import com.swayam.pathprep.dto.response.ApiErrorResponse;
import com.swayam.pathprep.filter.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        String traceId = MDC.get(TraceIdFilter.MDC_TRACE_ID_KEY);
        if (traceId == null) {
            Object attr = request.getAttribute(TraceIdFilter.MDC_TRACE_ID_KEY);
            traceId = attr != null ? attr.toString() : UUID.randomUUID().toString();
        }

        log.warn("[{}] Access denied (status 403, code=FORBIDDEN) to {} {}: {}",
                traceId, request.getMethod(), request.getRequestURI(), accessDeniedException.getMessage());

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(TraceIdFilter.TRACE_ID_HEADER, traceId);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");

        ApiErrorResponse body = new ApiErrorResponse("FORBIDDEN", "Access is denied", List.of(), null, traceId);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
