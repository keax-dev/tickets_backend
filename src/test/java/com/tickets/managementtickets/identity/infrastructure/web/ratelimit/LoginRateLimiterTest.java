package com.tickets.managementtickets.identity.infrastructure.web.ratelimit;

import com.tickets.managementtickets.identity.infrastructure.security.SecurityProperties;
import com.tickets.managementtickets.shared.application.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// This test suite verifies login rate-limiting behavior without running the web layer.
class LoginRateLimiterTest {

    // Freeze the initial instant so the time-based rate-limiting window is deterministic.
    private static final Instant NOW = Instant.parse("2026-07-16T00:00:00Z");

    @Test
    void shouldBuildStableKeyFromRemoteAddressAndEmail() {
        // Arrange: create the limiter with fixed settings and time.
        LoginRateLimiter rateLimiter = new LoginRateLimiter(properties(), new MutableClock(NOW));

        // Act: build keys with whitespace and uppercase email.
        String normalizedKey = rateLimiter.keyFor(" 127.0.0.1 ", " User@Test.COM ");
        String fallbackKey = rateLimiter.keyFor(" ", null);

        // Assert: verify the normalized key format used for rate limit buckets.
        assertEquals("127.0.0.1|user@test.com", normalizedKey);
        assertEquals("unknown|", fallbackKey);
    }

    @Test
    void shouldRejectRequestsAfterConfiguredFailures() {
        // Arrange: create a limiter that allows two failed attempts in the active window.
        LoginRateLimiter rateLimiter = new LoginRateLimiter(properties(), new MutableClock(NOW));
        String key = "127.0.0.1|user@test.com";

        // Act: record the maximum configured failures.
        rateLimiter.recordFailure(key);
        rateLimiter.recordFailure(key);

        // Assert: verify the next request is blocked.
        TooManyRequestsException exception = assertThrows(
            TooManyRequestsException.class,
            () -> rateLimiter.checkAllowed(key)
        );
        assertEquals("LOGIN_RATE_LIMITED", exception.getCode());
    }

    @Test
    void shouldAllowRequestsAfterReset() {
        // Arrange: create a blocked key.
        LoginRateLimiter rateLimiter = new LoginRateLimiter(properties(), new MutableClock(NOW));
        String key = "127.0.0.1|user@test.com";
        rateLimiter.recordFailure(key);
        rateLimiter.recordFailure(key);

        // Act: reset the key after a successful login.
        rateLimiter.reset(key);

        // Assert: verify the same key is allowed again.
        assertDoesNotThrow(() -> rateLimiter.checkAllowed(key));
    }

    @Test
    void shouldEvictExpiredFailuresFromWindow() {
        // Arrange: record failures at the beginning of the window.
        MutableClock clock = new MutableClock(NOW);
        LoginRateLimiter rateLimiter = new LoginRateLimiter(properties(), clock);
        String key = "127.0.0.1|user@test.com";
        rateLimiter.recordFailure(key);
        rateLimiter.recordFailure(key);

        // Act: move time past the configured window.
        clock.setInstant(NOW.plusSeconds(6 * 60L));

        // Assert: verify old failures no longer block a new request.
        assertDoesNotThrow(() -> rateLimiter.checkAllowed(key));
    }

    private SecurityProperties properties() {
        // Provide a tiny configuration object with small limits so tests stay compact and easy to read.
        SecurityProperties properties = new SecurityProperties();
        // Allow only two failures before blocking the next attempt.
        properties.setLoginRateLimitMaxAttempts(2);
        // Keep the lock window short so time-window scenarios are easy to simulate.
        properties.setLoginRateLimitWindowMinutes(5);
        return properties;
    }

    private static final class MutableClock extends Clock {

        // Hold the current instant that the rate limiter will read.
        private Instant instant;

        private MutableClock(Instant instant) {
            // Start the clock at the instant chosen by the test.
            this.instant = instant;
        }

        private void setInstant(Instant instant) {
            // Let the test manually move time forward without waiting in real life.
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            // Always behave like a UTC clock so timestamps are stable.
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            // Zone changes are irrelevant for this fake clock, so return the same instance.
            return this;
        }

        @Override
        public Instant instant() {
            // Expose the mutable instant to the production component under test.
            return instant;
        }
    }
}
