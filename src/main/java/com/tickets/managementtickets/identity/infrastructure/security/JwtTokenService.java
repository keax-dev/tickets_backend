package com.tickets.managementtickets.identity.infrastructure.security;

import com.tickets.managementtickets.identity.application.model.AuthenticatedUser;
import com.tickets.managementtickets.identity.application.port.AccessTokenService;
import com.tickets.managementtickets.identity.application.port.AuthSecuritySettings;
import com.tickets.managementtickets.identity.application.service.RolePermissionService;
import com.tickets.managementtickets.identity.domain.model.Role;
import com.tickets.managementtickets.shared.application.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class JwtTokenService implements AccessTokenService {

    private final AuthSecuritySettings securityProperties;
    private final Clock clock;
    private final RolePermissionService rolePermissionService;

    public JwtTokenService(
        AuthSecuritySettings securityProperties,
        Clock clock,
        RolePermissionService rolePermissionService
    ) {
        this.securityProperties = securityProperties;
        this.clock = clock;
        this.rolePermissionService = rolePermissionService;
    }

    public String generateAccessToken(AuthenticatedUser user) {
        Instant issuedAt = clock.instant();
        Instant expiration = issuedAt.plus(securityProperties.getAccessTokenExpirationMinutes(), ChronoUnit.MINUTES);

        return Jwts.builder()
            .issuer(securityProperties.getIssuer())
            .subject(user.id())
            .issuedAt(java.util.Date.from(issuedAt))
            .expiration(java.util.Date.from(expiration))
            .claim("email", user.email())
            .claim("firstName", user.firstName())
            .claim("lastName", user.lastName())
            .claim("role", user.role().name())
            .signWith(secretKey())
            .compact();
    }

    public Instant resolveAccessTokenExpiration() {
        return clock.instant().plus(securityProperties.getAccessTokenExpirationMinutes(), ChronoUnit.MINUTES);
    }

    public AuthenticatedUser parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(secretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

            Role role = Role.valueOf(claims.get("role", String.class));
            return new AuthenticatedUser(
                claims.getSubject(),
                claims.get("email", String.class),
                claims.get("firstName", String.class),
                claims.get("lastName", String.class),
                role,
                rolePermissionService.resolvePermissions(role)
            );
        } catch (Exception exception) {
            throw new UnauthorizedException("INVALID_ACCESS_TOKEN", "The access token is invalid or expired.");
        }
    }

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(securityProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }
}
