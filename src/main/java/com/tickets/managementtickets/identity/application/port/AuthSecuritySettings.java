package com.tickets.managementtickets.identity.application.port;

import java.util.List;

public interface AuthSecuritySettings {

    int getAccessTokenExpirationMinutes();

    int getRefreshTokenExpirationDays();

    String getIssuer();

    String getJwtSecret();

    String getRefreshCookieName();

    List<String> getAllowedOrigins();
}
