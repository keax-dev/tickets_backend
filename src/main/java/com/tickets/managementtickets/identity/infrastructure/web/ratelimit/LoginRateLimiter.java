package com.tickets.managementtickets.identity.infrastructure.web.ratelimit;

import com.tickets.managementtickets.identity.application.port.AuthMetricsPort;
import com.tickets.managementtickets.identity.infrastructure.security.SecurityProperties;
import com.tickets.managementtickets.shared.application.exception.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class LoginRateLimiter {

    private final ConcurrentMap<String, Deque<Instant>> attemptsByKey = new ConcurrentHashMap<>();
    private final SecurityProperties securityProperties;
    private final Clock clock;
    private final AuthMetricsPort authMetricsPort;

    public LoginRateLimiter(SecurityProperties securityProperties, Clock clock) {
        this(securityProperties, clock, AuthMetricsPort.NO_OP);
    }

    @Autowired
    public LoginRateLimiter(SecurityProperties securityProperties, Clock clock, AuthMetricsPort authMetricsPort) {
        this.securityProperties = securityProperties;
        this.clock = clock;
        this.authMetricsPort = authMetricsPort;
    }

    public String keyFor(String remoteAddress, String email) {
        String normalizedAddress = remoteAddress == null || remoteAddress.isBlank() ? "unknown" : remoteAddress.trim();
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        return normalizedAddress + "|" + normalizedEmail;
    }

    public void checkAllowed(String key) {
        Deque<Instant> attempts = attemptsByKey.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (attempts) {
            evictExpired(attempts);
            if (attempts.size() >= securityProperties.getLoginRateLimitMaxAttempts()) {
                authMetricsPort.recordRateLimitBlocked();
                throw new TooManyRequestsException("LOGIN_RATE_LIMITED", "Too many login attempts. Please try again later.");
            }
        }
    }

    public void recordFailure(String key) {
        Deque<Instant> attempts = attemptsByKey.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (attempts) {
            evictExpired(attempts);
            attempts.addLast(clock.instant());
        }
    }

    public void reset(String key) {
        attemptsByKey.remove(key);
    }

    private void evictExpired(Deque<Instant> attempts) {
        Instant threshold = clock.instant().minus(securityProperties.getLoginRateLimitWindowMinutes(), ChronoUnit.MINUTES);
        while (!attempts.isEmpty() && attempts.peekFirst().isBefore(threshold)) {
            attempts.removeFirst();
        }
    }
}
