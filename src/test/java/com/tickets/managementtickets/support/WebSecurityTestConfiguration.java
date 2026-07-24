package com.tickets.managementtickets.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickets.managementtickets.identity.infrastructure.security.AccessTokenAuthenticationFilter;
import com.tickets.managementtickets.identity.infrastructure.security.JsonAccessDeniedHandler;
import com.tickets.managementtickets.identity.infrastructure.security.JsonAuthenticationEntryPoint;
import com.tickets.managementtickets.identity.infrastructure.security.SecurityProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@TestConfiguration
public class WebSecurityTestConfiguration {

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    @Bean
    SecurityProperties securityProperties() {
        SecurityProperties properties = new SecurityProperties();
        properties.setAccessTokenExpirationMinutes(15);
        properties.setRefreshTokenExpirationDays(7);
        properties.setIssuer("management-tickets-test");
        properties.setJwtSecret("test-jwt-secret-key-with-at-least-thirty-two-characters");
        properties.setRefreshCookieName("refresh_token");
        properties.setMaxFailedLoginAttempts(5);
        properties.setAccountLockMinutes(15);
        properties.setLoginRateLimitMaxAttempts(5);
        properties.setLoginRateLimitWindowMinutes(10);
        properties.setAllowedOrigins(java.util.List.of("http://localhost:4200"));
        return properties;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity httpSecurity,
        AccessTokenAuthenticationFilter accessTokenAuthenticationFilter,
        JsonAuthenticationEntryPoint authenticationEntryPoint,
        JsonAccessDeniedHandler accessDeniedHandler
    ) throws Exception {
        return httpSecurity
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh", "/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(accessTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("Direct Spring Security login is disabled in tests.");
        };
    }
}
