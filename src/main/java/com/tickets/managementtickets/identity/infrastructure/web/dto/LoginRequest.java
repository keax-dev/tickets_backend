package com.tickets.managementtickets.identity.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @Schema(description = "User email used to authenticate.", example = "agent.alvarez@tickets.local")
    @NotBlank @Email @Size(max = 160) String email,
    @Schema(description = "Plain-text password provided by the user.", example = "Password123!")
    @NotBlank @Size(max = 128) String password
) {
}
