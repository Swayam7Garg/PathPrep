package com.swayam.pathprep.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

public class RateLimitedException extends ApiException {

    public RateLimitedException(long retryAfterSeconds) {
        super("RATE_LIMITED", HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded. Please try again later.", List.of(), null, retryAfterSeconds);
    }
}
