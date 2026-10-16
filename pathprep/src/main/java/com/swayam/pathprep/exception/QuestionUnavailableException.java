package com.swayam.pathprep.exception;

import org.springframework.http.HttpStatus;

public class QuestionUnavailableException extends ApiException {
    public QuestionUnavailableException(String message) {
        super("QUESTION_UNAVAILABLE", HttpStatus.CONFLICT, message);
    }
}
