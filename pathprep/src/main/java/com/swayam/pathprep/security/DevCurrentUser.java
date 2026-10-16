package com.swayam.pathprep.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Development-only implementation returning fixed test user ID.
 * To be replaced with Spring Security SecurityContextHolder in Phase 7.
 */
@Slf4j
@Component
public class DevCurrentUser implements CurrentUser {

    @Override
    public Long getUserId() {
        log.trace("DevCurrentUser providing mock development user ID: 1L");
        return 1L;
    }
}
