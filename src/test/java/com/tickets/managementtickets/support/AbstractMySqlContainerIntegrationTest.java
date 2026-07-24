package com.tickets.managementtickets.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// This shared test support class provides a real MySQL database for integration tests that validate JPA mappings and Flyway migrations.
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractMySqlContainerIntegrationTest {

    // Container section: expose one MySQL instance shared by subclasses in this test JVM.
    @Container
    protected static final MySQLContainer<?> MYSQL_CONTAINER = new MySQLContainer<>("mysql:8.4")
        .withDatabaseName("management_tickets")
        .withUsername("root")
        .withPassword("");

    // Dynamic properties section: route Spring datasource and JWT placeholder values to the container-backed environment.
    @DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", MYSQL_CONTAINER::getPassword);
        registry.add("app.security.jwt-secret", () -> "management-tickets-test-secret-key-that-is-long-enough");
    }
}
