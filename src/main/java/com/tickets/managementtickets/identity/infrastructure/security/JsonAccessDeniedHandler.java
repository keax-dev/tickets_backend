package com.tickets.managementtickets.identity.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickets.managementtickets.shared.infrastructure.web.CorrelationIdFilter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class JsonAccessDeniedHandler implements org.springframework.security.web.access.AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public JsonAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of(
            "type", "https://management-tickets/errors/access-denied",
            "title", "Forbidden",
            "status", HttpStatus.FORBIDDEN.value(),
            "detail", "You do not have permission to perform this action.",
            "code", "ACCESS_DENIED",
            "correlationId", request.getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE),
            "timestamp", Instant.now(),
            "fieldErrors", List.of()
        ));
    }
}
