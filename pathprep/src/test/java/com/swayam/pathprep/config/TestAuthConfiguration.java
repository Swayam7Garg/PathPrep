package com.swayam.pathprep.config;

import com.swayam.pathprep.security.FakeGoogleIdTokenVerifier;
import com.swayam.pathprep.security.GoogleIdTokenVerifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestAuthConfiguration {

    @Bean
    @Primary
    public GoogleIdTokenVerifier fakeGoogleIdTokenVerifier() {
        return new FakeGoogleIdTokenVerifier();
    }
}
