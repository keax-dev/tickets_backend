package com.tickets.managementtickets.identity.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickets.managementtickets.shared.infrastructure.web.CorrelationIdFilter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class JsonAuthenticationEntryPoint implements org.springframework.security.web.AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JsonAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        org.springframework.security.core.AuthenticationException authException
    ) throws IOException, ServletException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of(
            "type", "https://management-tickets/errors/unauthorized",
            "title", "Unauthorized",
            "status", HttpStatus.UNAUTHORIZED.value(),
            "detail", "Authentication is required.",
            "code", "UNAUTHORIZED",
            "correlationId", request.getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE),
            "timestamp", Instant.now(),
            "fieldErrors", List.of()
        ));
    }
}
