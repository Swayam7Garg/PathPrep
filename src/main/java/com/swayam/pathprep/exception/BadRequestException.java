package com.swayam.pathprep.exception;

import org.springframework.http.HttpStatus;
import java.util.List;

public class BadRequestException extends ApiException {
    public BadRequestException(String code, String message) {
        super(code, HttpStatus.BAD_REQUEST, message);
    }

    public BadRequestException(String code, String message, List<String> details) {
        super(code, HttpStatus.BAD_REQUEST, message, details);
    }
}
