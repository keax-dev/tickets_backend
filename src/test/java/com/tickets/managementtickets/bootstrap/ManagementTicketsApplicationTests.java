package com.tickets.managementtickets.bootstrap;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// Boot the full Spring application context exactly as the real application would do.
@SpringBootTest
// Force the test profile so test-only properties, Flyway settings, and beans are used.
@ActiveProfiles("test")
// Run this integration test only when Docker is available, because the database comes from Testcontainers.
@Testcontainers(disabledWithoutDocker = true)
// This test suite verifies the Spring Boot application context and Flyway/MySQL wiring when Docker is available.
class ManagementTicketsApplicationTests {

    // Container section: provides a real MySQL database for the application context integration test.
    @Container
    static final MySQLContainer<?> MYSQL_CONTAINER = new MySQLContainer<>("mysql:8.4")
        .withDatabaseName("management_tickets")
        .withUsername("root")
        .withPassword("");

    // Dynamic properties section: connects Spring Boot to the Testcontainers database and test JWT secret.
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Override the datasource URL so Spring points to the ephemeral MySQL container instead of a local database.
        registry.add("spring.datasource.url", MYSQL_CONTAINER::getJdbcUrl);
        // Provide the username exposed by the container.
        registry.add("spring.datasource.username", MYSQL_CONTAINER::getUsername);
        // Provide the password exposed by the container.
        registry.add("spring.datasource.password", MYSQL_CONTAINER::getPassword);
        // Supply a deterministic JWT secret so security beans can initialize successfully during startup.
        registry.add("app.security.jwt-secret", () -> "management-tickets-test-secret-key-that-is-long-enough");
    }

    @Test
    void shouldLoadContext() {
        // There is no explicit assertion here.
        // This test passes simply by reaching the end of the method without startup failures.
        // If a bean cannot be created, Flyway cannot migrate, the datasource cannot connect,
        // or security configuration is invalid, Spring will fail before this method completes.
    }
}
