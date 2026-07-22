package com.tickets.managementtickets.shared.application.port;

public interface PasswordHashingService {

    String encode(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}
