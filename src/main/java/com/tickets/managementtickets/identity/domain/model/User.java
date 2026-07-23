package com.tickets.managementtickets.identity.domain.model;

import java.time.Instant;

public record User(
    String id,
    String firstName,
    String lastName,
    String email,
    String passwordHash,
    Role role,
    boolean active,
    int failedLoginAttempts,
    Instant lastLoginAt,
    Instant lockedUntil,
    long version
) {

    public static User create(String firstName, String lastName, String email, String passwordHash, Role role) {
        return new User(null, firstName, lastName, email, passwordHash, role, true, 0, null, null, 0);
    }

    public User updateProfile(String firstName, String lastName, String email, Role role) {
        return new User(id, firstName, lastName, email, passwordHash, role, active, failedLoginAttempts, lastLoginAt, lockedUntil, version);
    }

    public User withActive(boolean active) {
        return new User(id, firstName, lastName, email, passwordHash, role, active, active ? failedLoginAttempts : 0, active ? lastLoginAt : null, active ? lockedUntil : null, version);
    }

    public User recordFailedLogin(Instant failedAt, int maxFailedAttempts, int lockMinutes) {
        int nextAttempts = failedLoginAttempts + 1;
        Instant nextLockedUntil = nextAttempts >= maxFailedAttempts ? failedAt.plusSeconds(lockMinutes * 60L) : lockedUntil;
        return new User(id, firstName, lastName, email, passwordHash, role, active, nextAttempts, lastLoginAt, nextLockedUntil, version);
    }

    public User recordSuccessfulLogin(Instant loginAt) {
        return new User(id, firstName, lastName, email, passwordHash, role, active, 0, loginAt, null, version);
    }

    public User clearExpiredLoginLock(Instant now) {
        if (lockedUntil == null || lockedUntil.isAfter(now)) {
            return this;
        }
        return new User(id, firstName, lastName, email, passwordHash, role, active, 0, lastLoginAt, null, version);
    }

    public boolean isLoginLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public String displayName() {
        return firstName + " " + lastName;
    }
}
