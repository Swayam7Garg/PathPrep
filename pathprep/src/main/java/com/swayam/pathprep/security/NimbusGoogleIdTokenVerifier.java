package com.swayam.pathprep.security;

import com.swayam.pathprep.config.AuthProperties;
import com.swayam.pathprep.exception.AuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@ConditionalOnMissingBean(value = GoogleIdTokenVerifier.class, ignored = NimbusGoogleIdTokenVerifier.class)
public class NimbusGoogleIdTokenVerifier implements GoogleIdTokenVerifier {

    private final AuthProperties authProperties;
    private final JwtDecoder jwtDecoder;

    @org.springframework.beans.factory.annotation.Autowired
    public NimbusGoogleIdTokenVerifier(AuthProperties authProperties) {
        this.authProperties = authProperties;
        this.jwtDecoder = NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs").build();
    }

    // Constructor for testing or custom decoder injection
    public NimbusGoogleIdTokenVerifier(AuthProperties authProperties, JwtDecoder jwtDecoder) {
        this.authProperties = authProperties;
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public VerifiedGoogleUser verify(String credential) {
        if (credential == null || credential.isBlank() || credential.length() > 4096) {
            throw AuthException.googleTokenInvalid();
        }

        try {
            Jwt jwt = jwtDecoder.decode(credential);

            // 1. Verify issuer
            String issuer = jwt.getIssuer() != null ? jwt.getIssuer().toString() : "";
            if (!"accounts.google.com".equals(issuer) && !"https://accounts.google.com".equals(issuer)) {
                log.warn("Google ID token issuer rejected: {}", issuer);
                throw AuthException.googleTokenInvalid();
            }

            // 2. Verify audience
            List<String> audience = jwt.getAudience();
            String configuredClientId = authProperties.getGoogle().getClientId();
            if (audience == null || !audience.contains(configuredClientId)) {
                log.warn("Google ID token audience rejected: expected clientId={} in audience={}", configuredClientId, audience);
                throw AuthException.googleTokenInvalid();
            }

            // 3. Verify email
            String email = jwt.getClaimAsString("email");
            if (email == null || email.isBlank()) {
                log.warn("Google ID token has missing or blank email claim");
                throw AuthException.googleTokenInvalid();
            }

            // 4. Verify email_verified
            Object emailVerifiedClaim = jwt.getClaims().get("email_verified");
            boolean emailVerified = false;
            if (emailVerifiedClaim instanceof Boolean b) {
                emailVerified = b;
            } else if (emailVerifiedClaim instanceof String s) {
                emailVerified = Boolean.parseBoolean(s);
            }
            if (!emailVerified) {
                log.warn("Google ID token rejected: email is not verified by Google");
                throw AuthException.googleTokenInvalid();
            }

            String sub = jwt.getSubject();
            if (sub == null || sub.isBlank()) {
                throw AuthException.googleTokenInvalid();
            }

            String name = jwt.getClaimAsString("name");
            String pictureUrl = jwt.getClaimAsString("picture");

            return new VerifiedGoogleUser(sub, email.toLowerCase().trim(), true, name, pictureUrl);
        } catch (Exception e) {
            log.warn("Google ID token verification failed: {}", e.getMessage());
            throw AuthException.googleTokenInvalid();
        }
    }
}
