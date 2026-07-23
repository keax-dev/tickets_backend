package com.tickets.managementtickets.shared.application.service;

import com.tickets.managementtickets.shared.application.exception.ConflictException;

public final class ResourceVersionPolicy {

    private ResourceVersionPolicy() {
    }

    public static void ensureCurrent(long currentVersion, long requestedVersion, String message) {
        if (currentVersion != requestedVersion) {
            throw new ConflictException("RESOURCE_VERSION_CONFLICT", message);
        }
    }
}
