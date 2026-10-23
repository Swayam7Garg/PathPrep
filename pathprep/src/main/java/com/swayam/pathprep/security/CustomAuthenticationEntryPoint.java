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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        String traceId = MDC.get(TraceIdFilter.MDC_TRACE_ID_KEY);
        if (traceId == null) {
            Object attr = request.getAttribute(TraceIdFilter.MDC_TRACE_ID_KEY);
            traceId = attr != null ? attr.toString() : UUID.randomUUID().toString();
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String code = "UNAUTHENTICATED";
        String message = "Full authentication is required to access this resource";

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String errorDesc = "";
            if (authException instanceof InvalidBearerTokenException ibte) {
                errorDesc = ibte.getMessage();
            } else if (authException instanceof OAuth2AuthenticationException oae) {
                OAuth2Error error = oae.getError();
                errorDesc = error != null ? error.getDescription() + " " + error.getErrorCode() : oae.getMessage();
            } else if (authException.getMessage() != null) {
                errorDesc = authException.getMessage();
            }

            if (errorDesc != null && (errorDesc.toLowerCase().contains("expired") || errorDesc.toLowerCase().contains("jwt has expired"))) {
                code = "TOKEN_EXPIRED";
                message = "Access token has expired";
            } else {
                code = "TOKEN_INVALID";
                message = "Invalid access token";
            }
        }

        log.warn("[{}] Authentication failure (status 401, code={}): {}", traceId, code, authException.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(TraceIdFilter.TRACE_ID_HEADER, traceId);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");

        ApiErrorResponse body = new ApiErrorResponse(code, message, List.of(), null, traceId);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
