package com.swayam.pathprep.exception;

import org.springframework.http.HttpStatus;

public class ReviewNotDueException extends ApiException {
    public ReviewNotDueException(String message) {
        super("REVIEW_NOT_DUE", HttpStatus.CONFLICT, message);
    }
}
