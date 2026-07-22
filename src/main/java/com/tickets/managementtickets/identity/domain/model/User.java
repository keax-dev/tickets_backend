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
    long version
) {

    public static User create(String firstName, String lastName, String email, String passwordHash, Role role) {
        return new User(null, firstName, lastName, email, passwordHash, role, true, 0, null, 0);
    }

    public User updateProfile(String firstName, String lastName, String email, Role role) {
        return new User(id, firstName, lastName, email, passwordHash, role, active, failedLoginAttempts, lastLoginAt, version);
    }

    public User withActive(boolean active) {
        return new User(id, firstName, lastName, email, passwordHash, role, active, failedLoginAttempts, active ? lastLoginAt : null, version);
    }

    public User recordFailedLogin() {
        return new User(id, firstName, lastName, email, passwordHash, role, active, failedLoginAttempts + 1, lastLoginAt, version);
    }

    public User recordSuccessfulLogin(Instant loginAt) {
        return new User(id, firstName, lastName, email, passwordHash, role, active, 0, loginAt, version);
    }

    public String displayName() {
        return firstName + " " + lastName;
    }
}
