package com.tickets.managementtickets.identity.infrastructure.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    @Positive
    private int accessTokenExpirationMinutes;

    @Positive
    private int refreshTokenExpirationDays;

    @NotBlank
    private String issuer;

    @NotBlank
    @Size(min = 32)
    private String jwtSecret;

    @NotBlank
    private String refreshCookieName;

    private boolean refreshCookieSecure;

    @Positive
    private int maxFailedLoginAttempts;

    @Positive
    private int accountLockMinutes;

    @Positive
    private int loginRateLimitMaxAttempts;

    @Positive
    private int loginRateLimitWindowMinutes;

    @NotEmpty
    private List<@NotBlank String> allowedOrigins = new ArrayList<>();

    public int getAccessTokenExpirationMinutes() {
        return accessTokenExpirationMinutes;
    }

    public void setAccessTokenExpirationMinutes(int accessTokenExpirationMinutes) {
        this.accessTokenExpirationMinutes = accessTokenExpirationMinutes;
    }

    public int getRefreshTokenExpirationDays() {
        return refreshTokenExpirationDays;
    }

    public void setRefreshTokenExpirationDays(int refreshTokenExpirationDays) {
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public String getRefreshCookieName() {
        return refreshCookieName;
    }

    public void setRefreshCookieName(String refreshCookieName) {
        this.refreshCookieName = refreshCookieName;
    }

    public boolean isRefreshCookieSecure() {
        return refreshCookieSecure;
    }

    public void setRefreshCookieSecure(boolean refreshCookieSecure) {
        this.refreshCookieSecure = refreshCookieSecure;
    }

    public int getMaxFailedLoginAttempts() {
        return maxFailedLoginAttempts;
    }

    public void setMaxFailedLoginAttempts(int maxFailedLoginAttempts) {
        this.maxFailedLoginAttempts = maxFailedLoginAttempts;
    }

    public int getAccountLockMinutes() {
        return accountLockMinutes;
    }

    public void setAccountLockMinutes(int accountLockMinutes) {
        this.accountLockMinutes = accountLockMinutes;
    }

    public int getLoginRateLimitMaxAttempts() {
        return loginRateLimitMaxAttempts;
    }

    public void setLoginRateLimitMaxAttempts(int loginRateLimitMaxAttempts) {
        this.loginRateLimitMaxAttempts = loginRateLimitMaxAttempts;
    }

    public int getLoginRateLimitWindowMinutes() {
        return loginRateLimitWindowMinutes;
    }

    public void setLoginRateLimitWindowMinutes(int loginRateLimitWindowMinutes) {
        this.loginRateLimitWindowMinutes = loginRateLimitWindowMinutes;
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }
}
