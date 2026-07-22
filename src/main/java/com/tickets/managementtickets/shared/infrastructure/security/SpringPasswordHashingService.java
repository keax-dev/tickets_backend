package com.tickets.managementtickets.shared.infrastructure.security;

import com.tickets.managementtickets.shared.application.port.PasswordHashingService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SpringPasswordHashingService implements PasswordHashingService {

    private final PasswordEncoder passwordEncoder;

    public SpringPasswordHashingService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
