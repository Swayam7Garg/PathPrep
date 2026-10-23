package com.swayam.pathprep.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Dev-and-test only fallback CurrentUser bean.
 * Active only in "dev" and "test" profiles; excluded from production.
 */
@Slf4j
@Component
@Profile({"dev", "test"})
public class DevCurrentUser implements CurrentUser {

    @Override
    public Long getUserId() {
        log.trace("DevCurrentUser providing development/test user ID: 1L");
        return 1L;
    }
}
