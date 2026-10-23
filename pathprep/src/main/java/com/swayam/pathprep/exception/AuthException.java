package com.swayam.pathprep.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

public class AuthException extends ApiException {

    public AuthException(String code, HttpStatus httpStatus, String message) {
        super(code, httpStatus, message);
    }

    public AuthException(String code, HttpStatus httpStatus, String message, List<String> details) {
        super(code, httpStatus, message, details);
    }

    public static AuthException googleTokenInvalid() {
        return new AuthException("GOOGLE_TOKEN_INVALID", HttpStatus.UNAUTHORIZED, "Invalid Google ID token");
    }

    public static AuthException tokenExpired() {
        return new AuthException("TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED, "Access token has expired");
    }

    public static AuthException tokenInvalid() {
        return new AuthException("TOKEN_INVALID", HttpStatus.UNAUTHORIZED, "Invalid access token");
    }

    public static AuthException unauthenticated() {
        return new AuthException("UNAUTHENTICATED", HttpStatus.UNAUTHORIZED, "Full authentication is required to access this resource");
    }

    public static AuthException refreshInvalid() {
        return new AuthException("REFRESH_INVALID", HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
    }

    public static AuthException refreshReused() {
        return new AuthException("REFRESH_REUSED", HttpStatus.UNAUTHORIZED, "Refresh token reuse detected");
    }

    public static AuthException adminKeyInvalid() {
        return new AuthException("ADMIN_KEY_INVALID", HttpStatus.UNAUTHORIZED, "Invalid administrator API key");
    }

    public static AuthException forbidden() {
        return new AuthException("FORBIDDEN", HttpStatus.FORBIDDEN, "Access is denied");
    }

    public static AuthException emailDomainNotAllowed() {
        return new AuthException("EMAIL_DOMAIN_NOT_ALLOWED", HttpStatus.FORBIDDEN, "Please sign in with your college email");
    }

    public static AuthException originNotAllowed() {
        return new AuthException("ORIGIN_NOT_ALLOWED", HttpStatus.FORBIDDEN, "Cross-site origin or security header verification failed");
    }

    public static AuthException accountConflict() {
        return new AuthException("ACCOUNT_CONFLICT", HttpStatus.CONFLICT, "Account linking conflict");
    }
}
