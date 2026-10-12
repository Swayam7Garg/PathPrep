package com.swayam.pathprep.dto;

import java.util.List;

public record ApiErrorResponse(
    String code,
    String message,
    List<String> details,
    Long retryAfterSeconds,
    String traceId
) {}
