package com.swayam.pathprep.security;

public interface GoogleIdTokenVerifier {
    VerifiedGoogleUser verify(String credential);
}
