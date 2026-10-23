package com.swayam.pathprep.config;

import com.swayam.pathprep.filter.AdminKeyFilter;
import com.swayam.pathprep.filter.TraceIdFilter;
import com.swayam.pathprep.security.CustomAccessDeniedHandler;
import com.swayam.pathprep.security.CustomAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import com.swayam.pathprep.dto.response.ApiErrorResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.config.Customizer;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthProperties authProperties;
    private final CorsProperties corsProperties;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final AdminKeyFilter adminKeyFilter;
    private final TraceIdFilter traceIdFilter;
    private final Environment environment;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        boolean isDevOrTest = activeProfiles.contains("dev") || activeProfiles.contains("test");

        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .contentTypeOptions(HeadersConfigurer.ContentTypeOptionsConfig::disable) // spring default already adds nosniff
                        .addHeaderWriter((request, response) -> {
                            response.setHeader("X-Content-Type-Options", "nosniff");
                            String uri = request.getRequestURI();
                            if (uri.startsWith("/api/v1/auth/") || uri.equals("/api/v1/me")) {
                                response.setHeader("Cache-Control", "no-store");
                            }
                        })
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                )
                .authorizeHttpRequests(auth -> {
                    // Public authentication endpoints
                    auth.requestMatchers(
                            "/api/v1/auth/google",
                            "/api/v1/auth/refresh",
                            "/api/v1/auth/logout"
                    ).permitAll();

                    // Dev login is publicly accessible only if dev/test profile
                    if (isDevOrTest) {
                        auth.requestMatchers("/api/v1/auth/dev-login").permitAll();
                        auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll();
                    } else {
                        auth.requestMatchers("/api/v1/auth/dev-login").denyAll();
                        auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").denyAll();
                    }

                    // Public catalog and health
                    auth.requestMatchers(
                            "/api/v1/tracks",
                            "/api/v1/tracks/*/topics",
                            "/actuator/health",
                            "/api/v1/health",
                            "/error"
                    ).permitAll();

                    // Admin endpoints require ROLE_ADMIN
                    auth.requestMatchers("/api/v1/admin/**").hasRole("ADMIN");

                    // Everything else requires authentication
                    auth.anyRequest().authenticated();
                })
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .addFilterBefore(traceIdFilter, BearerTokenAuthenticationFilter.class)
                .addFilterBefore(adminKeyFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key", "X-Trace-Id", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("X-Trace-Id", "Location"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public CorsFilter corsFilter(CorsConfigurationSource corsConfigurationSource, ObjectMapper objectMapper) {
        CorsFilter filter = new CorsFilter(corsConfigurationSource);
        filter.setCorsProcessor(new DefaultCorsProcessor() {
            @Override
            protected void rejectRequest(ServerHttpResponse response) throws java.io.IOException {
                response.setStatusCode(HttpStatus.FORBIDDEN);
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

                String traceId = MDC.get(TraceIdFilter.MDC_TRACE_ID_KEY);
                if (traceId == null) {
                    traceId = java.util.UUID.randomUUID().toString();
                }
                response.getHeaders().add(TraceIdFilter.TRACE_ID_HEADER, traceId);

                ApiErrorResponse body = new ApiErrorResponse(
                        "ORIGIN_NOT_ALLOWED",
                        "Cross-site origin or security header verification failed",
                        List.of(),
                        null,
                        traceId
                );
                byte[] bytes = objectMapper.writeValueAsBytes(body);
                response.getBody().write(bytes);
                response.flush();
            }
        });
        return filter;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        authoritiesConverter.setAuthoritiesClaimName("role");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        converter.setPrincipalClaimName("sub");
        return converter;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        byte[] secretBytes = authProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        SecretKey secretKey = new SecretKeySpec(secretBytes, "HmacSHA256");

        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(authProperties.getJwt().getIssuer());
        jwtDecoder.setJwtValidator(withIssuer);
        return jwtDecoder;
    }
}
