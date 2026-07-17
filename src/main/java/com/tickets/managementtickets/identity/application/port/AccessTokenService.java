package com.tickets.managementtickets.identity.application.port;

import com.tickets.managementtickets.identity.application.model.AuthenticatedUser;

import java.time.Instant;

public interface AccessTokenService {

    String generateAccessToken(AuthenticatedUser user);

    Instant resolveAccessTokenExpiration();
}
