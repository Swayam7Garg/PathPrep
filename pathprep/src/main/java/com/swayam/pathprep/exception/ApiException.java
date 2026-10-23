package com.swayam.pathprep.exception;

import org.springframework.http.HttpStatus;
import java.util.List;
import java.util.Map;

public abstract class ApiException extends RuntimeException {
    private final String code;
    private final HttpStatus httpStatus;
    private final List<String> details;
    private final Map<String, Object> meta;
    private final Long retryAfterSeconds;

    protected ApiException(String code, HttpStatus httpStatus, String message) {
        this(code, httpStatus, message, List.of(), null, null);
    }

    protected ApiException(String code, HttpStatus httpStatus, String message, List<String> details) {
        this(code, httpStatus, message, details, null, null);
    }

    protected ApiException(String code, HttpStatus httpStatus, String message, List<String> details, Map<String, Object> meta) {
        this(code, httpStatus, message, details, meta, null);
    }

    protected ApiException(String code, HttpStatus httpStatus, String message, List<String> details, Map<String, Object> meta, Long retryAfterSeconds) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
        this.details = details != null ? details : List.of();
        this.meta = meta;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public List<String> getDetails() {
        return details;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
