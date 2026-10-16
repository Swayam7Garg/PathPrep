package com.swayam.pathprep.exception;

import org.springframework.http.HttpStatus;
import java.util.List;
import java.util.Map;

public class ActiveSessionExistsException extends ApiException {
    public ActiveSessionExistsException(Long activeSessionId) {
        super("ACTIVE_SESSION_EXISTS", HttpStatus.CONFLICT,
                "An active session is already in progress: " + activeSessionId,
                List.of(),
                Map.of("activeSessionId", activeSessionId));
    }
}
