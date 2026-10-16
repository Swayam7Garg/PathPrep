package com.swayam.pathprep.exception;

import org.springframework.http.HttpStatus;

public class ReportNotAvailableException extends ApiException {
    public ReportNotAvailableException(Long sessionId) {
        super("REPORT_NOT_AVAILABLE", HttpStatus.CONFLICT, "Report is not available for abandoned session: " + sessionId);
    }
}
