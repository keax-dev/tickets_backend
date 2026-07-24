package com.tickets.managementtickets.identity.infrastructure.web.dto;

import com.tickets.managementtickets.identity.domain.model.Role;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "CurrentUserResponse", description = "Authenticated user information exposed to the frontend.")
public record CurrentUserResponse(
    @Schema(description = "Unique user identifier.", example = "10000000-0000-0000-0000-000000000003")
    String id,
    @Schema(description = "User first name.", example = "Ana")
    String firstName,
    @Schema(description = "User last name.", example = "Alvarez")
    String lastName,
    @Schema(description = "User email.", example = "agent.alvarez@tickets.local")
    String email,
    @Schema(description = "Assigned role.", example = "SUPPORT_AGENT")
    Role role,
    @ArraySchema(schema = @Schema(example = "TICKET_READ_ASSIGNED"), arraySchema = @Schema(description = "Resolved permissions granted to the authenticated role."))
    List<String> permissions
) {

    public static CurrentUserResponse from(com.tickets.managementtickets.identity.application.result.AuthenticatedUserResponse response) {
        return new CurrentUserResponse(
            response.id(),
            response.firstName(),
            response.lastName(),
            response.email(),
            response.role(),
            response.permissions()
        );
    }
}
