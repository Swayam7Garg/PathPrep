package com.swayam.pathprep.exception;

import org.springframework.http.HttpStatus;

public class FeatureNotAvailableException extends ApiException {
    public FeatureNotAvailableException(String code, String message) {
        super(code, HttpStatus.BAD_REQUEST, message);
    }
}
