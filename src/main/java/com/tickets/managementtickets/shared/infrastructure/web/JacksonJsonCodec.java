package com.tickets.managementtickets.shared.infrastructure.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickets.managementtickets.shared.application.port.JsonCodec;
import org.springframework.stereotype.Component;

@Component
public class JacksonJsonCodec implements JsonCodec {

    private final ObjectMapper objectMapper;

    public JacksonJsonCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("The value could not be serialized.", exception);
        }
    }

    @Override
    public <T> T deserialize(String value, Class<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("The value could not be deserialized.", exception);
        }
    }
}
