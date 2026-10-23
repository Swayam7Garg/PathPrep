package com.swayam.pathprep.security;

import com.swayam.pathprep.config.AuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class CookieHelper {

    private final AuthProperties authProperties;

    public ResponseCookie createRefreshCookie(String rawRefreshToken) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(authProperties.getRefreshCookie().getName(), rawRefreshToken)
                .httpOnly(true)
                .secure(authProperties.getRefreshCookie().isSecure())
                .path(authProperties.getRefreshCookie().getPath())
                .sameSite(authProperties.getRefreshCookie().getSameSite())
                .maxAge(authProperties.getJwt().getRefreshTtl());

        String domain = authProperties.getRefreshCookie().getDomain();
        if (domain != null && !domain.isBlank()) {
            builder.domain(domain);
        }

        return builder.build();
    }

    public ResponseCookie createClearRefreshCookie() {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(authProperties.getRefreshCookie().getName(), "")
                .httpOnly(true)
                .secure(authProperties.getRefreshCookie().isSecure())
                .path(authProperties.getRefreshCookie().getPath())
                .sameSite(authProperties.getRefreshCookie().getSameSite())
                .maxAge(Duration.ZERO);

        String domain = authProperties.getRefreshCookie().getDomain();
        if (domain != null && !domain.isBlank()) {
            builder.domain(domain);
        }

        return builder.build();
    }
}
