package com.swayam.pathprep.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.swayam.pathprep.config.AuthProperties;
import com.swayam.pathprep.entity.RefreshToken;
import com.swayam.pathprep.entity.User;
import com.swayam.pathprep.enums.UserRole;
import com.swayam.pathprep.exception.AuthException;
import com.swayam.pathprep.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final AuthProperties authProperties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Mints a signed HS256 access token with claims: sub = userId, role, iss, iat, exp, jti.
     * Contains NO email or name.
     */
    public String createAccessToken(Long userId, UserRole role) {
        Instant now = clock.instant();
        Instant expiry = now.plus(authProperties.getJwt().getAccessTtl());

        try {
            byte[] secretBytes = authProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
            SecretKey key = new SecretKeySpec(secretBytes, "HmacSHA256");

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(userId.toString())
                    .claim("role", role.name())
                    .issuer(authProperties.getJwt().getIssuer())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expiry))
                    .jwtID(UUID.randomUUID().toString())
                    .build();

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
            signedJWT.sign(new MACSigner(key));
            return signedJWT.serialize();
        } catch (Exception e) {
            log.error("Failed to sign access token for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Error signing access token", e);
        }
    }

    /**
     * Generates a new 32-byte opaque refresh token and creates a new family.
     */
    @Transactional
    public String createRefreshToken(User user, String userAgent) {
        String rawToken = generateSecureToken();
        String hash = hashToken(rawToken);
        String familyId = UUID.randomUUID().toString();
        Instant now = clock.instant();
        Instant expiry = now.plus(authProperties.getJwt().getRefreshTtl());

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hash)
                .familyId(familyId)
                .expiresAt(expiry)
                .createdAt(now)
                .userAgent(userAgent)
                .build();

        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    /**
     * Rotates a refresh token:
     * 1. Looks up token by hash with pessimistic write lock (concurrency control).
     * 2. If token is revoked, revokes whole family and throws REFRESH_REUSED.
     * 3. If token is expired or not found, throws REFRESH_INVALID.
     * 4. Mints new token in the same family, marks old token revoked and sets replaced_by.
     */
    @Transactional(noRollbackFor = AuthException.class)
    public RefreshResult rotateRefreshToken(String rawOldToken, String userAgent) {
        if (rawOldToken == null || rawOldToken.isBlank()) {
            throw AuthException.refreshInvalid();
        }

        String hash = hashToken(rawOldToken);
        Instant now = clock.instant();

        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByTokenHashForUpdate(hash);
        if (tokenOpt.isEmpty()) {
            log.warn("Refresh token not found in database");
            throw AuthException.refreshInvalid();
        }

        RefreshToken oldToken = tokenOpt.get();

        // Check if token was already revoked (Reuse Detection)
        if (oldToken.getRevokedAt() != null) {
            log.warn("Revoked refresh token presented for family {}. Revoking entire token family (Reuse Attack detected).", oldToken.getFamilyId());
            int count = refreshTokenRepository.revokeFamily(oldToken.getFamilyId(), now);
            log.warn("revokeFamily affected rows: {}", count);
            throw AuthException.refreshReused();
        }

        // Check if token is expired
        if (oldToken.getExpiresAt().isBefore(now)) {
            log.warn("Expired refresh token presented for family {}", oldToken.getFamilyId());
            throw AuthException.refreshInvalid();
        }

        User user = oldToken.getUser();
        String rawNewToken = generateSecureToken();
        String newHash = hashToken(rawNewToken);
        Instant newExpiry = now.plus(authProperties.getJwt().getRefreshTtl());

        RefreshToken newToken = RefreshToken.builder()
                .user(user)
                .tokenHash(newHash)
                .familyId(oldToken.getFamilyId())
                .expiresAt(newExpiry)
                .createdAt(now)
                .userAgent(userAgent)
                .build();

        RefreshToken savedNewToken = refreshTokenRepository.save(newToken);

        oldToken.setRevokedAt(now);
        oldToken.setReplacedBy(savedNewToken);
        refreshTokenRepository.save(oldToken);

        String newAccessToken = createAccessToken(user.getId(), user.getRole());
        return new RefreshResult(newAccessToken, rawNewToken, user);
    }

    /**
     * Revokes the whole family of the given refresh token.
     */
    @Transactional
    public void revokeFamilyByToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        String hash = hashToken(rawToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            refreshTokenRepository.revokeFamily(token.getFamilyId(), clock.instant());
        });
    }

    /**
     * Revokes all tokens belonging to the specified user.
     */
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.revokeAllForUser(userId, clock.instant());
    }

    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record RefreshResult(String accessToken, String rawRefreshToken, User user) {}
}
