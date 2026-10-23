package com.swayam.pathprep.dto.response;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSec,
        UserProfile user
) {
    public static AuthResponse of(String accessToken, long expiresInSec, UserProfile user) {
        return new AuthResponse(accessToken, "Bearer", expiresInSec, user);
    }
}
