package com.tickets.managementtickets.shared.infrastructure.web;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "FieldErrorResponse", description = "Validation failure for a specific request field.")
public record FieldErrorResponse(
    @Schema(description = "Request field that failed validation.", example = "email")
    String field,
    @Schema(description = "Validation message associated with the field.", example = "must be a well-formed email address")
    String message
) {
}
