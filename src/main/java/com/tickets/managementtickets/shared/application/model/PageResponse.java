package com.tickets.managementtickets.shared.application.model;

import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    List<String> sort
) {

    public static <T> PageResponse<T> of(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<String> sort
    ) {
        return new PageResponse<>(
            content,
            page,
            size,
            totalElements,
            totalPages,
            sort
        );
    }

    public <R> PageResponse<R> map(Function<T, R> mapper) {
        return new PageResponse<>(
            content.stream().map(mapper).toList(),
            page,
            size,
            totalElements,
            totalPages,
            sort
        );
    }
}
