package com.swayam.pathprep.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private Google google = new Google();
    private Jwt jwt = new Jwt();
    private RefreshCookie refreshCookie = new RefreshCookie();
    private List<String> adminEmails = new ArrayList<>();
    private List<String> allowedEmailDomains = new ArrayList<>();
    private boolean allowAnyGoogleAccount = false;
    private boolean devLoginEnabled = false;

    @Getter
    @Setter
    public static class Google {
        private String clientId = "";
    }

    @Getter
    @Setter
    public static class Jwt {
        private String secret = "";
        private String issuer = "pathprep";
        private Duration accessTtl = Duration.ofMinutes(15);
        private Duration refreshTtl = Duration.ofDays(30);
    }

    @Getter
    @Setter
    public static class RefreshCookie {
        private String name = "pp_refresh";
        private boolean secure = true;
        private String sameSite = "Lax";
        private String path = "/api/v1/auth";
        private String domain;
    }
}
