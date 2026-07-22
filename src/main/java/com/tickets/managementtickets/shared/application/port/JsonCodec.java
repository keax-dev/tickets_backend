package com.tickets.managementtickets.shared.application.port;

public interface JsonCodec {

    String serialize(Object value);

    <T> T deserialize(String value, Class<T> type);
}
