[![Language: English](https://img.shields.io/badge/Language-English-0A66C2?style=for-the-badge)](ARCHITECTURE.md)
[![Idioma: Espanol](https://img.shields.io/badge/Idioma-Espanol-6A1B9A?style=for-the-badge)](ARCHITECTURE.es.md)

# Architecture Guide

[Volver al README en Espanol](../README.md) | [Back to README in English](../README.en.md)

## Purpose

This document explains how the backend applies Clean Architecture and DDD in practical terms, how the layers depend on each other, and why the project is structured around business modules instead of technical folders only.

## Architectural Style

The backend follows:

- Clean Architecture for dependency direction and framework isolation
- DDD-style modularization through bounded contexts
- Explicit composition in the bootstrap layer instead of annotating application services with Spring stereotypes

At a high level, dependencies move inward:

```text
infrastructure adapters -> application -> domain
bootstrap -> application + infrastructure
```

This means the domain model does not know about Spring, HTTP, JPA, or JSON. The application layer orchestrates use cases through ports. Infrastructure implements those ports.

## Bounded Contexts

Each first-level package under `com.tickets.managementtickets` is treated as an architectural slice:

| Context | Responsibility |
| --- | --- |
| `identity` | Authentication, authorization, users, roles, permissions, refresh tokens |
| `ticket` | Core ticket lifecycle, comments, history, visibility, idempotency, ticket codes |
| `category` | Ticket categories and category status changes |
| `sla` | SLA policy management and priority-based timing rules |
| `notification` | Notification storage, listing, and read tracking |
| `dashboard` | Dashboard summaries and recent activity |
| `shared` | Cross-cutting exceptions, ports, base infrastructure support |
| `observability` | Metrics adapters and runtime gauges |
| `bootstrap` | Application startup, bean wiring, scheduling |

## Why Comments, History, Sequence, and Idempotency Stay Inside `ticket`

The `ticket` bounded context owns more than the `Ticket` entity itself. It also owns:

- Ticket comments
- Ticket history
- Ticket sequence generation
- Ticket idempotency records
- Ticket lifecycle policies

These are not separate business contexts. They are supporting concepts of the ticket lifecycle, so keeping them inside the `ticket` module preserves cohesion and avoids artificial fragmentation.

Inside that bounded context, responsibilities are still separated by layer:

- `domain` for business state and invariants
- `application` for use cases and ports
- `infrastructure` for web, persistence, and support adapters

## Layer Responsibilities

### Domain Layer

Location example:

- `ticket/domain/model`
- `identity/domain/model`
- `sla/domain/model`

Responsibilities:

- Represent business concepts and state
- Enforce domain invariants
- Stay framework-free

Examples:

- `Ticket`
- `TicketStatus`
- `TicketComment`
- `User`
- `Role`
- `SlaPolicy`

The domain layer must not depend on:

- Spring
- Jakarta APIs
- Application services
- Infrastructure adapters

### Application Layer

Location example:

- `ticket/application/service`
- `identity/application/service`
- `ticket/application/port`
- `identity/application/port`

Responsibilities:

- Orchestrate use cases
- Load and persist domain objects through ports
- Coordinate policies, validation, notifications, history, and transactions
- Expose commands, queries, results, and ports

Important design choice:

- Application services are plain Java classes
- They are instantiated from the composition root in `bootstrap/ApplicationConfiguration`
- They are intentionally not annotated with `@Service` or `@Component`

This keeps the application layer independent from Spring and closer to Clean Architecture rules.

### Infrastructure Layer

Location example:

- `infrastructure/web`
- `infrastructure/persistence`
- `identity/infrastructure/security`
- `observability/infrastructure`

Responsibilities:

- Expose HTTP controllers and transport DTOs
- Map request and response models
- Implement repository ports with JPA
- Implement security adapters
- Implement metrics adapters
- Provide technical runtime components such as filters and rate limiters

Spring annotations belong mostly here:

- `@RestController`
- `@Repository`
- `@Component`
- `@Service` for technical adapters when appropriate

### Bootstrap Layer

Location:

- `bootstrap/ApplicationConfiguration`
- `bootstrap/MaintenanceScheduler`

Responsibilities:

- Define the composition root
- Wire application services with their ports and policies
- Enable scheduling
- Keep framework configuration outside business use cases

This is the key place that replaces the need to annotate application services with `@Service`.

## Request Flow

Typical request path:

1. A controller in `infrastructure/web/controller` receives the HTTP request
2. A web DTO is validated and mapped to an application command or query
3. An application service coordinates the use case
4. Domain objects enforce state transitions and business rules
5. Repository ports are called
6. Infrastructure adapters persist and map data
7. Application results are mapped back to web DTOs
8. The controller returns the response

Example for tickets:

```text
TicketController
  -> TicketService
    -> TicketRepositoryPort
    -> TicketHistoryRepositoryPort
    -> TicketCommentRepositoryPort
    -> NotificationRepositoryPort
    -> domain model transitions
```

## Security and Authentication Design

The security model combines:

- JWT access tokens for API calls
- Refresh token rotation through HttpOnly cookies
- Role and permission checks through the identity module
- Login rate limiting through `LoginRateLimiter`

Public endpoints:

- `/api/v1/auth/login`
- `/api/v1/auth/refresh`
- Swagger/OpenAPI endpoints
- Health endpoint

All other business endpoints require authentication.

## Persistence and Migrations

Persistence uses:

- JPA entities in `infrastructure/persistence/entity`
- Spring Data repositories in `infrastructure/persistence/repository`
- Adapter implementations in `infrastructure/persistence/adapter`

Schema evolution is handled by Flyway migrations in:

- `src/main/resources/db/migration`

Migrations cover:

- Identity tables
- Category and catalog tables
- Ticket tables
- Notification tables
- Default categories and SLA seed data
- Login lock and system actor support

## Observability and Runtime Behavior

Observability is implemented through:

- `CorrelationIdFilter` for `X-Correlation-Id`
- `ApiExceptionHandler` for structured problem responses
- Micrometer metrics adapters in `observability/infrastructure`
- Actuator endpoints for health, metrics, and Prometheus scraping

The scheduler runs:

- Hourly cleanup of expired refresh tokens and idempotency records
- Daily auto-close of resolved tickets

## Architecture Enforcement with ArchUnit

The project includes [ArchitectureRulesTest](../src/test/java/com/tickets/managementtickets/architecture/ArchitectureRulesTest.java), which protects the intended architecture.

Current rules verify that:

- Application code does not depend on infrastructure or frameworks
- Web controllers do not access persistence repositories directly
- Application code does not depend on web DTOs
- Application services are not annotated with Spring stereotypes
- Web does not depend on persistence implementation details
- Persistence does not depend on web
- Web DTOs do not depend on application services
- Domain does not depend on application, infrastructure, or frameworks
- Persistence entities do not depend on application
- Top-level modules do not form dependency cycles

## Testing Strategy

The test pyramid currently includes:

- Unit tests for domain and application services
- Architecture tests with ArchUnit
- Web integration tests for auth and ticket controllers
- Persistence integration tests with JPA and Testcontainers-backed MySQL

This mix gives:

- Fast feedback for business rules
- Boundary protection for the architecture
- Realistic verification of HTTP behavior
- Realistic verification of JPA mappings and queries

## Summary

The project is not only grouped by entity names. It is grouped by bounded context, and inside each context it separates domain, application, and infrastructure concerns. That is why comments, history, sequence, and related ticket mechanics live inside the `ticket` module while still remaining properly layered.
