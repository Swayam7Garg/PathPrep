package com.swayam.pathprep.exception;

import org.springframework.http.HttpStatus;

public class SessionNotCompletedException extends ApiException {
    public SessionNotCompletedException(Long sessionId) {
        super("SESSION_NOT_COMPLETED", HttpStatus.CONFLICT, "Session is still in progress: " + sessionId);
    }
}
