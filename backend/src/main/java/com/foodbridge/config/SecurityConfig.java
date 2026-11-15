package com.foodbridge.config;

import com.foodbridge.security.JwtAuthenticationFilter;
import com.foodbridge.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Central HTTP security policy.
 *
 * <ul>
 *   <li>Stateless: no HTTP session, no CSRF token (the refresh token is a
 *       SameSite cookie and access tokens are sent explicitly).</li>
 *   <li>Authorization is enforced here at the URL level and again with
 *       {@code @PreAuthorize} on service/controller methods for defence in
 *       depth.</li>
 *   <li>CORS is restricted to explicitly configured origins.</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final FoodBridgeProperties properties;

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Cost factor 10 is the Spring default and a sensible CPU/UX trade-off.
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAuthenticationEntryPoint))
                .headers(h -> h
                        .frameOptions(frame -> {
                            String mode = properties.getSecurity().getFrameOptions().toUpperCase();
                            switch (mode) {
                                case "DISABLE" -> frame.disable();
                                case "DENY" -> frame.deny();
                                default -> frame.sameOrigin();
                            }
                        })
                        .contentTypeOptions(Customizer.withDefaults())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)))
                .authorizeHttpRequests(auth -> auth
                        // --- public ---
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()
                        .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        // --- role scoped ---
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/donor/**").hasRole("DONOR")
                        .requestMatchers("/api/ngo/**").hasRole("NGO")
                        // --- authenticated, any role ---
                        // The SSE endpoint authenticates with an `access_token` query
                        // parameter (EventSource cannot send headers) and validates that
                        // token inside the controller, so the filter chain cannot
                        // pre-authorise it.
                        .requestMatchers(HttpMethod.GET, "/api/notifications/stream").permitAll()
                        .requestMatchers("/api/notifications/**").authenticated()
                        .requestMatchers("/api/me/**").authenticated()
                        .requestMatchers("/api/donations/**").authenticated()
                        .requestMatchers("/api/claims/**").authenticated()
                        .requestMatchers("/api/pickups/**").authenticated()
                        .requestMatchers("/api/categories/**").authenticated()
                        .requestMatchers("/api/requests/**").authenticated()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        // --- the bundled React SPA (public shell; every byte of data
                        // it renders still comes from an authenticated /api call) ---
                        .requestMatchers(HttpMethod.GET, "/", "/index.html", "/favicon.svg",
                                "/assets/**", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/{path:^(?!api$|actuator$|uploads$|swagger-ui$|api-docs$)[^\\.]*}/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(properties.getCors().origins());
        if (!properties.getCors().originPatterns().isEmpty()) {
            config.setAllowedOriginPatterns(properties.getCors().originPatterns());
        }
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-Id", "Accept"));
        config.setExposedHeaders(List.of("X-Request-Id"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
