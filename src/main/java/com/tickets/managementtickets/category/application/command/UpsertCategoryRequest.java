package com.tickets.managementtickets.category.application.command;

public record UpsertCategoryRequest(long version, String name, String description) {
}
