package com.swayam.pathprep.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

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
