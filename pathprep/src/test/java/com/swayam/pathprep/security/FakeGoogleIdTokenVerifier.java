package com.swayam.pathprep.security;

import com.swayam.pathprep.exception.AuthException;

import java.util.HashMap;
import java.util.Map;

/**
 * In-memory test double for GoogleIdTokenVerifier.
 * No test makes any external network calls to Google JWKS.
 */
public class FakeGoogleIdTokenVerifier implements GoogleIdTokenVerifier {

    private final Map<String, VerifiedGoogleUser> tokenMap = new HashMap<>();
    private final Map<String, RuntimeException> exceptionMap = new HashMap<>();
    private RuntimeException defaultException = null;

    public void registerToken(String credential, VerifiedGoogleUser user) {
        tokenMap.put(credential, user);
    }

    public void registerFailure(String credential, RuntimeException exception) {
        exceptionMap.put(credential, exception);
    }

    public void setDefaultException(RuntimeException exception) {
        this.defaultException = exception;
    }

    public void reset() {
        tokenMap.clear();
        exceptionMap.clear();
        defaultException = null;
    }

    @Override
    public VerifiedGoogleUser verify(String credential) {
        if (credential == null || credential.isBlank() || credential.length() > 4096) {
            throw AuthException.googleTokenInvalid();
        }
        if (exceptionMap.containsKey(credential)) {
            throw exceptionMap.get(credential);
        }
        if (tokenMap.containsKey(credential)) {
            return tokenMap.get(credential);
        }
        if (defaultException != null) {
            throw defaultException;
        }
        throw AuthException.googleTokenInvalid();
    }
}
