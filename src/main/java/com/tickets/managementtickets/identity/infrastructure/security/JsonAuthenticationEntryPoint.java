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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        String correlationId = resolveCorrelationId(request);
        response.setHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "https://management-tickets/errors/unauthorized");
        body.put("title", "Unauthorized");
        body.put("status", HttpStatus.UNAUTHORIZED.value());
        body.put("detail", "Authentication is required.");
        body.put("code", "UNAUTHORIZED");
        body.put("correlationId", correlationId);
        body.put("timestamp", Instant.now());
        body.put("fieldErrors", List.of());

        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        Object correlationId = request.getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE);
        return correlationId == null ? UUID.randomUUID().toString() : correlationId.toString();
    }
}
