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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        String correlationId = resolveCorrelationId(request);
        response.setHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "https://management-tickets/errors/access-denied");
        body.put("title", "Forbidden");
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("detail", "You do not have permission to perform this action.");
        body.put("code", "ACCESS_DENIED");
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
