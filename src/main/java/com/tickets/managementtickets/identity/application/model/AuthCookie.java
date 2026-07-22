package com.tickets.managementtickets.identity.application.model;

public record AuthCookie(
    String name,
    String value,
    boolean httpOnly,
    boolean secure,
    String sameSite,
    String path,
    long maxAgeSeconds
) {
}
