package com.swayam.pathprep.security;

public record VerifiedGoogleUser(
        String sub,
        String email,
        boolean emailVerified,
        String name,
        String pictureUrl
) {}
