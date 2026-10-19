package com.swayam.pathprep.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

/**
 * Standardized API error response payload containing error codes, HTTP status, timestamp, trace ID, and field errors.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
    String code,
    String message,
    List<String> details,
    Long retryAfterSeconds,
    String traceId,
    Map<String, Object> meta
) {
    public ApiErrorResponse(String code, String message, List<String> details, Long retryAfterSeconds, String traceId) {
        this(code, message, details, retryAfterSeconds, traceId, null);
    }
}
