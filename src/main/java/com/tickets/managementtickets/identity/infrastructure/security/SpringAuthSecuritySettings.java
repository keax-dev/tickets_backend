package com.tickets.managementtickets.identity.infrastructure.security;

import com.tickets.managementtickets.identity.application.port.AuthSecuritySettings;
import org.springframework.stereotype.Component;

@Component
public class SpringAuthSecuritySettings implements AuthSecuritySettings {

    private final SecurityProperties properties;

    public SpringAuthSecuritySettings(SecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public int getAccessTokenExpirationMinutes() {
        return properties.getAccessTokenExpirationMinutes();
    }

    @Override
    public int getRefreshTokenExpirationDays() {
        return properties.getRefreshTokenExpirationDays();
    }

    @Override
    public String getIssuer() {
        return properties.getIssuer();
    }

    @Override
    public String getJwtSecret() {
        return properties.getJwtSecret();
    }

    @Override
    public String getRefreshCookieName() {
        return properties.getRefreshCookieName();
    }

    @Override
    public boolean isRefreshCookieSecure() {
        return properties.isRefreshCookieSecure();
    }

    @Override
    public int getMaxFailedLoginAttempts() {
        return properties.getMaxFailedLoginAttempts();
    }

    @Override
    public int getAccountLockMinutes() {
        return properties.getAccountLockMinutes();
    }
}
