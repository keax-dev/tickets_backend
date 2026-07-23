package com.tickets.managementtickets.identity.application.result;

import com.tickets.managementtickets.identity.application.model.AuthCookie;

public record AuthResult(AuthResponse response, AuthCookie refreshCookie) {
}
