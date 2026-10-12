package com.swayam.pathprep.exception;

import org.springframework.http.HttpStatus;
import java.util.List;

public abstract class ApiException extends RuntimeException {
    private final String code;
    private final HttpStatus httpStatus;
    private final List<String> details;

    protected ApiException(String code, HttpStatus httpStatus, String message) {
        this(code, httpStatus, message, List.of());
    }

    protected ApiException(String code, HttpStatus httpStatus, String message, List<String> details) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
        this.details = details != null ? details : List.of();
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
}
