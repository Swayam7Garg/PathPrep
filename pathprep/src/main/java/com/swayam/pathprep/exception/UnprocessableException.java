package com.swayam.pathprep.exception;

import org.springframework.http.HttpStatus;

public class UnprocessableException extends ApiException {
    public UnprocessableException(String code, String message) {
        super(code, HttpStatus.valueOf(422), message);
    }
}
