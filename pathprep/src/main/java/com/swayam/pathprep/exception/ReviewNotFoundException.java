package com.swayam.pathprep.exception;

import org.springframework.http.HttpStatus;

public class ReviewNotFoundException extends ApiException {
    public ReviewNotFoundException(String message) {
        super("REVIEW_NOT_FOUND", HttpStatus.NOT_FOUND, message);
    }
}
