package com.swayam.pathprep.service;

import com.swayam.pathprep.config.AuthProperties;
import com.swayam.pathprep.dto.response.UserProfile;
import com.swayam.pathprep.entity.Institution;
import com.swayam.pathprep.entity.User;
import com.swayam.pathprep.enums.UserRole;
import com.swayam.pathprep.exception.AuthException;
import com.swayam.pathprep.repository.InstitutionRepository;
import com.swayam.pathprep.repository.UserRepository;
import com.swayam.pathprep.security.GoogleIdTokenVerifier;
import com.swayam.pathprep.security.TokenService;
import com.swayam.pathprep.security.VerifiedGoogleUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthProperties authProperties;
    private final UserRepository userRepository;
    private final InstitutionRepository institutionRepository;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final TokenService tokenService;
    private final UserProfileAssembler userProfileAssembler;
    private final Clock clock;

    public record AuthLoginResult(String accessToken, String rawRefreshToken, long expiresInSec, UserProfile userProfile) {}

    @Transactional
    public AuthLoginResult loginWithGoogle(String credential, String userAgent) {
        VerifiedGoogleUser googleUser = googleIdTokenVerifier.verify(credential);
        String email = googleUser.email().toLowerCase().trim();
        String domain = extractDomain(email);

        if (!isAllowed(email, domain)) {
            log.warn("Login rejected for email {} with domain {}", email, domain);
            throw AuthException.emailDomainNotAllowed();
        }

        User user = provisionOrUpdateUser(googleUser, email, domain);

        String accessToken = tokenService.createAccessToken(user.getId(), user.getRole());
        String rawRefreshToken = tokenService.createRefreshToken(user, userAgent);
        UserProfile profile = userProfileAssembler.toProfile(user);

        long expiresInSec = authProperties.getJwt().getAccessTtl().toSeconds();
        return new AuthLoginResult(accessToken, rawRefreshToken, expiresInSec, profile);
    }

    @Transactional(noRollbackFor = AuthException.class)
    public AuthLoginResult refreshToken(String rawRefreshToken, String userAgent) {
        TokenService.RefreshResult result = tokenService.rotateRefreshToken(rawRefreshToken, userAgent);
        UserProfile profile = userProfileAssembler.toProfile(result.user());
        long expiresInSec = authProperties.getJwt().getAccessTtl().toSeconds();
        return new AuthLoginResult(result.accessToken(), result.rawRefreshToken(), expiresInSec, profile);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        tokenService.revokeFamilyByToken(rawRefreshToken);
    }

    @Transactional
    public void logoutAll(Long userId) {
        tokenService.revokeAllUserTokens(userId);
    }

    @Transactional
    public AuthLoginResult devLogin(String emailParam, String userAgent) {
        String email = (emailParam != null && !emailParam.isBlank())
                ? emailParam.toLowerCase().trim()
                : "student@nie.edu.in";
        String domain = extractDomain(email);

        Optional<User> userOpt = userRepository.findByEmail(email);
        User user;
        if (userOpt.isPresent()) {
            user = userOpt.get();
            user.setLastLoginAt(clock.instant());
            userRepository.save(user);
        } else {
            Long institutionId = institutionRepository.findByEmailDomainIgnoreCase(domain)
                    .map(Institution::getId)
                    .orElse(null);

            boolean isAdminEmail = authProperties.getAdminEmails().stream()
                    .anyMatch(ae -> ae.equalsIgnoreCase(email));

            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(email.substring(0, email.indexOf('@')));
            newUser.setRole(isAdminEmail ? UserRole.ADMIN : UserRole.STUDENT);
            newUser.setInstitutionId(institutionId);
            newUser.setLastLoginAt(clock.instant());
            newUser.setCreatedAt(clock.instant());
            newUser.setUpdatedAt(clock.instant());
            user = userRepository.save(newUser);
        }

        String accessToken = tokenService.createAccessToken(user.getId(), user.getRole());
        String rawRefreshToken = tokenService.createRefreshToken(user, userAgent);
        UserProfile profile = userProfileAssembler.toProfile(user);
        long expiresInSec = authProperties.getJwt().getAccessTtl().toSeconds();
        return new AuthLoginResult(accessToken, rawRefreshToken, expiresInSec, profile);
    }

    public boolean isAllowed(String email, String domain) {
        // 1. Admin emails always allowed
        boolean isAdminEmail = authProperties.getAdminEmails().stream()
                .anyMatch(adminEmail -> adminEmail.equalsIgnoreCase(email.trim()));
        if (isAdminEmail) {
            return true;
        }

        // 2. allow-any-google-account flag
        if (authProperties.isAllowAnyGoogleAccount()) {
            return true;
        }

        // 3. allowed-email-domains list
        boolean inAllowedDomains = authProperties.getAllowedEmailDomains().stream()
                .anyMatch(allowedDomain -> allowedDomain.equalsIgnoreCase(domain));
        if (inAllowedDomains) {
            return true;
        }

        // 4. matches institutions.email_domain table
        return institutionRepository.findByEmailDomainIgnoreCase(domain).isPresent();
    }

    public String extractDomain(String email) {
        int atIndex = email.lastIndexOf('@');
        if (atIndex == -1 || atIndex == email.length() - 1) {
            return "";
        }
        return email.substring(atIndex + 1).toLowerCase().trim();
    }

    private User provisionOrUpdateUser(VerifiedGoogleUser googleUser, String email, String domain) {
        Optional<User> bySub = userRepository.findByGoogleSub(googleUser.sub());
        User user;
        if (bySub.isPresent()) {
            user = bySub.get();
        } else {
            Optional<User> byEmail = userRepository.findByEmail(email);
            if (byEmail.isPresent()) {
                user = byEmail.get();
                if (user.getGoogleSub() != null && !user.getGoogleSub().equals(googleUser.sub())) {
                    log.warn("Account conflict: user {} already linked to google_sub {}", email, user.getGoogleSub());
                    throw AuthException.accountConflict();
                }
                user.setGoogleSub(googleUser.sub());
            } else {
                UserRole initialRole = UserRole.STUDENT;
                boolean isAdminEmail = authProperties.getAdminEmails().stream()
                        .anyMatch(adminEmail -> adminEmail.equalsIgnoreCase(email));
                if (isAdminEmail) {
                    initialRole = UserRole.ADMIN;
                }

                String name = (googleUser.name() != null && !googleUser.name().isBlank())
                        ? googleUser.name().trim()
                        : email.substring(0, email.indexOf('@'));

                Long institutionId = institutionRepository.findByEmailDomainIgnoreCase(domain)
                        .map(Institution::getId)
                        .orElse(null);

                User newUser = new User();
                newUser.setEmail(email);
                newUser.setGoogleSub(googleUser.sub());
                newUser.setName(name);
                newUser.setRole(initialRole);
                newUser.setInstitutionId(institutionId);
                newUser.setPictureUrl(googleUser.pictureUrl());
                newUser.setLastLoginAt(clock.instant());
                newUser.setCreatedAt(clock.instant());
                newUser.setUpdatedAt(clock.instant());

                try {
                    return userRepository.saveAndFlush(newUser);
                } catch (DataIntegrityViolationException dive) {
                    log.warn("Concurrent first login race condition detected for email {}. Re-fetching user.", email);
                    return userRepository.findByEmail(email)
                            .orElseThrow(() -> dive);
                }
            }
        }

        user.setLastLoginAt(clock.instant());
        if (googleUser.pictureUrl() != null && !googleUser.pictureUrl().isBlank()) {
            user.setPictureUrl(googleUser.pictureUrl());
        }
        if (user.getName() == null || user.getName().isBlank()) {
            if (googleUser.name() != null && !googleUser.name().isBlank()) {
                user.setName(googleUser.name().trim());
            }
        }

        boolean isAdminEmail = authProperties.getAdminEmails().stream()
                .anyMatch(adminEmail -> adminEmail.equalsIgnoreCase(email));
        if (isAdminEmail && user.getRole() != UserRole.ADMIN) {
            log.info("Promoting user {} to ADMIN based on admin-emails configuration", email);
            user.setRole(UserRole.ADMIN);
        }

        return userRepository.save(user);
    }
}
