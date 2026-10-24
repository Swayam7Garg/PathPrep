package com.swayam.pathprep;

import com.swayam.pathprep.enums.UserRole;
import com.swayam.pathprep.security.TokenService;
import org.springframework.stereotype.Component;

/**
 * Shared test authentication helper for minting real access tokens in integration tests.
 */
@Component
public class TestAuth {

    private final TokenService tokenService;

    public TestAuth(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    public String devToken() {
        return tokenFor(1L, UserRole.STUDENT);
    }

    public String adminToken() {
        return tokenFor(1L, UserRole.ADMIN);
    }

    public String tokenFor(Long userId, UserRole role) {
        return tokenService.createAccessToken(userId, role);
    }

    public String devBearer() {
        return "Bearer " + devToken();
    }

    public String adminBearer() {
        return "Bearer " + adminToken();
    }

    public String bearer(Long userId, UserRole role) {
        return "Bearer " + tokenFor(userId, role);
    }
}
