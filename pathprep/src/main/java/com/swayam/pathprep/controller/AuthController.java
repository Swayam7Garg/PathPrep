package com.swayam.pathprep.controller;

import com.swayam.pathprep.config.AuthProperties;
import com.swayam.pathprep.dto.request.DevLoginRequest;
import com.swayam.pathprep.dto.request.GoogleLoginRequest;
import com.swayam.pathprep.dto.response.AuthResponse;
import com.swayam.pathprep.exception.AuthException;
import com.swayam.pathprep.exception.NotFoundException;
import com.swayam.pathprep.security.AuthRateLimiter;
import com.swayam.pathprep.security.CookieEndpointProtection;
import com.swayam.pathprep.security.CookieHelper;
import com.swayam.pathprep.security.CurrentUser;
import com.swayam.pathprep.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthProperties authProperties;
    private final CookieHelper cookieHelper;
    private final CookieEndpointProtection cookieEndpointProtection;
    private final AuthRateLimiter authRateLimiter;
    private final CurrentUser currentUser;
    private final Environment environment;

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginWithGoogle(
            @Valid @RequestBody GoogleLoginRequest body,
            HttpServletRequest request) {

        authRateLimiter.checkRateLimit(request);

        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        AuthService.AuthLoginResult result = authService.loginWithGoogle(body.credential(), userAgent);

        ResponseCookie refreshCookie = cookieHelper.createRefreshCookie(result.rawRefreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(AuthResponse.of(result.accessToken(), result.expiresInSec(), result.userProfile()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {

        authRateLimiter.checkRateLimit(request);
        cookieEndpointProtection.validate(request);

        String rawRefreshToken = extractCookie(request, authProperties.getRefreshCookie().getName());
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw AuthException.refreshInvalid();
        }

        try {
            String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
            AuthService.AuthLoginResult result = authService.refreshToken(rawRefreshToken, userAgent);

            ResponseCookie newCookie = cookieHelper.createRefreshCookie(result.rawRefreshToken());

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, newCookie.toString())
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .body(AuthResponse.of(result.accessToken(), result.expiresInSec(), result.userProfile()));
        } catch (AuthException ae) {
            // Clear the cookie on invalid or reused refresh tokens
            ResponseCookie clearCookie = cookieHelper.createClearRefreshCookie();
            response.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());
            throw ae;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        cookieEndpointProtection.validate(request);

        String rawRefreshToken = extractCookie(request, authProperties.getRefreshCookie().getName());
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            authService.logout(rawRefreshToken);
        }

        ResponseCookie clearCookie = cookieHelper.createClearRefreshCookie();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll() {
        Long userId = currentUser.getUserId();
        authService.logoutAll(userId);

        ResponseCookie clearCookie = cookieHelper.createClearRefreshCookie();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .build();
    }

    @PostMapping("/dev-login")
    public ResponseEntity<AuthResponse> devLogin(
            @RequestBody(required = false) DevLoginRequest body,
            HttpServletRequest request) {

        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        boolean isDev = activeProfiles.contains("dev") || activeProfiles.contains("test");

        if (!isDev || !authProperties.isDevLoginEnabled()) {
            throw new NotFoundException("ENDPOINT_NOT_FOUND", "Dev login endpoint is not enabled or available in this environment");
        }

        authRateLimiter.checkRateLimit(request);

        String email = body != null ? body.email() : null;
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        AuthService.AuthLoginResult result = authService.devLogin(email, userAgent);

        ResponseCookie refreshCookie = cookieHelper.createRefreshCookie(result.rawRefreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(AuthResponse.of(result.accessToken(), result.expiresInSec(), result.userProfile()));
    }

    private String extractCookie(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
