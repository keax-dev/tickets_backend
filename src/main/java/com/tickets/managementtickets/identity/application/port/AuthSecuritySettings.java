package com.tickets.managementtickets.identity.application.port;

public interface AuthSecuritySettings {

    int getAccessTokenExpirationMinutes();

    int getRefreshTokenExpirationDays();

    String getIssuer();

    String getJwtSecret();

    String getRefreshCookieName();

    boolean isRefreshCookieSecure();
}
