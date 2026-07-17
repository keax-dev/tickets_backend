package com.tickets.managementtickets.shared.application.model;

import org.springframework.data.domain.Page;

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

    public static <T> PageResponse<T> fromPage(Page<T> page) {
        List<String> sortValues = page.getSort()
            .stream()
            .map(order -> order.getProperty() + "," + order.getDirection().name().toLowerCase())
            .toList();

        return new PageResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            sortValues
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
