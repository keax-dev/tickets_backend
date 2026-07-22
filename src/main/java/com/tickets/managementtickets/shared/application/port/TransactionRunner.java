package com.tickets.managementtickets.shared.application.port;

import java.util.function.Supplier;

public interface TransactionRunner {

    <T> T readOnly(Supplier<T> action);

    <T> T required(Supplier<T> action);

    default void required(Runnable action) {
        required(() -> {
            action.run();
            return null;
        });
    }
}
