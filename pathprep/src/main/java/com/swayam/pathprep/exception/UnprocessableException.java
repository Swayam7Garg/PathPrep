package com.swayam.pathprep.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

public class UnprocessableException extends ApiException {
    public UnprocessableException(String code, String message) {
        super(code, HttpStatus.valueOf(422), message);
    }

    public UnprocessableException(String code, String message, List<String> details) {
        super(code, HttpStatus.valueOf(422), message, details);
    }
}
