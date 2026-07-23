package com.tickets.managementtickets.identity.infrastructure.web.dto;

import com.tickets.managementtickets.identity.domain.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
    @NotBlank @Size(max = 80) String firstName,
    @NotBlank @Size(max = 80) String lastName,
    @NotBlank @Email @Size(max = 160) String email,
    @NotBlank @Size(min = 12, max = 128) String password,
    @NotNull Role role
) {
}
