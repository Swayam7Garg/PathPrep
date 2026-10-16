package com.swayam.pathprep.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a client tries to perform an action that conflicts with the current resource state.
 *
 * <p>Maps to HTTP {@code 409 CONFLICT}. Used in Phase 4 when a student submits an answer
 * to a session that has already been {@code COMPLETED} or {@code FAILED}.
 */
public class ConflictException extends ApiException {
    public ConflictException(String code, String message) {
        super(code, HttpStatus.CONFLICT, message);
    }
}
