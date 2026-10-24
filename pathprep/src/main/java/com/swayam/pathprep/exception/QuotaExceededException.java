package com.swayam.pathprep.exception;

import org.springframework.http.HttpStatus;

public class QuotaExceededException extends ApiException {
    public QuotaExceededException(String message) {
        super("AI_QUOTA_EXCEEDED", HttpStatus.TOO_MANY_REQUESTS, message);
    }
}
