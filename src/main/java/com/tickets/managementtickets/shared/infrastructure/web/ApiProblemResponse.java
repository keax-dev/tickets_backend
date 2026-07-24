package com.tickets.managementtickets.shared.infrastructure.web;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(name = "ApiProblemResponse", description = "Standard API error response returned by the backend.")
public record ApiProblemResponse(
    @Schema(description = "URI that identifies the error type.", example = "https://management-tickets/errors/invalid-request")
    String type,
    @Schema(description = "Short human-readable title of the error.", example = "Bad Request")
    String title,
    @Schema(description = "HTTP status code.", example = "400")
    int status,
    @Schema(description = "Detailed explanation of the error.", example = "The request contains invalid fields.")
    String detail,
    @Schema(description = "Stable business or application error code.", example = "INVALID_REQUEST")
    String code,
    @Schema(description = "Correlation identifier that can be used to trace the request in logs.", example = "f6f9f951-2b12-4f93-9fcb-84a9ef9080f6")
    String correlationId,
    @Schema(description = "Timestamp at which the error response was generated.")
    Instant timestamp,
    @ArraySchema(schema = @Schema(implementation = FieldErrorResponse.class), arraySchema = @Schema(description = "Validation errors grouped by field when applicable."))
    List<FieldErrorResponse> fieldErrors
) {
}
