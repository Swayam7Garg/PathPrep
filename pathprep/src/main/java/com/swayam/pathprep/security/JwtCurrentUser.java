package com.swayam.pathprep.security;

import com.swayam.pathprep.exception.AuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Primary
@Component
public class JwtCurrentUser implements CurrentUser {

    private final ObjectProvider<DevCurrentUser> devCurrentUserProvider;
    private final boolean isDevOrTest;

    public JwtCurrentUser(ObjectProvider<DevCurrentUser> devCurrentUserProvider, Environment environment) {
        this.devCurrentUserProvider = devCurrentUserProvider;
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        this.isDevOrTest = activeProfiles.contains("dev") || activeProfiles.contains("test");
    }

    @Override
    public Long getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            String principalName = authentication.getName();
            try {
                return Long.parseLong(principalName);
            } catch (NumberFormatException e) {
                if (isDevOrTest) {
                    DevCurrentUser devUser = devCurrentUserProvider.getIfAvailable();
                    if (devUser != null) {
                        return devUser.getUserId();
                    }
                }
                log.error("Principal name '{}' is not a valid numeric user ID", principalName);
                throw AuthException.tokenInvalid();
            }
        }

        // In dev or test profile, fallback to DevCurrentUser if present (e.g. for test setup or unauthenticated dev calls)
        if (isDevOrTest) {
            DevCurrentUser devUser = devCurrentUserProvider.getIfAvailable();
            if (devUser != null) {
                return devUser.getUserId();
            }
        }

        log.warn("Attempted to access current user without an authenticated principal in non-dev profile");
        throw AuthException.unauthenticated();
    }
}
