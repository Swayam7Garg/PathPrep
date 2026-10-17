package com.swayam.pathprep.exception;

import org.springframework.http.HttpStatus;

public class SessionNotCompletedException extends ApiException {
    public SessionNotCompletedException(Long sessionId) {
        super("SESSION_NOT_COMPLETED", HttpStatus.BAD_REQUEST, "Session is still in progress: " + sessionId);
    }
}
