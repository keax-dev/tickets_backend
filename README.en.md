[![Idioma: Espanol](https://img.shields.io/badge/Idioma-Espanol-6A1B9A?style=for-the-badge)](README.md)
[![Language: English](https://img.shields.io/badge/Language-English-0A66C2?style=for-the-badge)](README.en.md)

# Management Tickets Backend

Spring Boot 4 backend for ticket management, authentication, SLA administration, notifications, catalog management, and dashboard reporting.

[Architecture Guide (English)](docs/ARCHITECTURE.md) | [Guia de Arquitectura (Espanol)](docs/ARCHITECTURE.es.md)

## Overview

This project exposes a REST API for a service desk style ticketing system. It supports:

- JWT-based authentication with refresh-token rotation
- Role-based access control for admins, managers, agents, and customers
- Ticket lifecycle management from creation to closure
- Ticket comments and ticket history tracking
- SLA policy administration and ticket dashboard summaries
- Notifications, categories, observability metrics, and scheduled maintenance

## Technology Stack

- Java 21
- Spring Boot 4.1
- Spring Web MVC
- Spring Security
- Spring Data JPA
- MySQL
- Flyway
- Springdoc OpenAPI + Swagger UI
- Micrometer + Prometheus registry
- JUnit 5, ArchUnit, Mockito, Testcontainers
- GitHub Actions for CI

## Functional Modules

The backend is organized by bounded context:

- `identity` - Authentication, users, roles, permissions, refresh tokens
- `ticket` - Tickets, comments, history, code generation, idempotency, lifecycle
- `category` - Ticket categories and status management
- `sla` - SLA rules by priority
- `notification` - User notifications and read status updates
- `dashboard` - Summary and recent activity endpoints
- `shared` - Cross-cutting exceptions, security helpers, web helpers, transaction ports
- `observability` - Metrics adapters and gauges
- `bootstrap` - Application startup, composition root, and scheduled maintenance

For the architectural rationale behind this structure, see [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Requirements

- Java 21 or higher
- Maven 3.9 or higher if you do not use the wrapper
- MySQL 8.x
- Docker is optional and only needed to run Testcontainers-based persistence integration tests locally

## Configuration

The main application configuration lives in [src/main/resources/application.properties](src/main/resources/application.properties).

### Required Environment Variables

| Variable | Default | Purpose |
| --- | --- | --- |
| `JWT_SECRET` | none | Required secret used to sign JWT access tokens |
| `DB_HOST` | `localhost` | MySQL host |
| `DB_PORT` | `3306` | MySQL port |
| `DB_NAME` | `management_tickets` | Database name |
| `DB_USERNAME` | `root` | Database username |
| `DB_PASSWORD` | empty | Database password |
| `SPRING_PROFILES_ACTIVE` | none | Optional Spring profile, usually `local` in development |

### Available Profiles

- `default` - Base application settings
- `local` - Local developer overrides
- `test` - Test support properties
- `prod` - Production-specific actuator exposure overrides

## Running Locally

1. Create the database:

```sql
CREATE DATABASE management_tickets
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

2. Export the JWT secret and optionally the local profile.

PowerShell:

```powershell
$env:JWT_SECRET = "replace-with-a-secret-of-at-least-32-characters"
$env:SPRING_PROFILES_ACTIVE = "local"
.\mvnw.cmd spring-boot:run
```

macOS or Linux:

```bash
export JWT_SECRET="replace-with-a-secret-of-at-least-32-characters"
export SPRING_PROFILES_ACTIVE=local
./mvnw spring-boot:run
```

3. Open Swagger UI:

- `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Flyway will validate and apply database migrations on startup.

## Authentication Flow

- `POST /api/v1/auth/login` authenticates a user and returns:
  - a JWT access token in the response body
  - a rotated refresh token as an HttpOnly cookie
- `POST /api/v1/auth/refresh` uses the refresh cookie to issue a new access token
- `POST /api/v1/auth/logout` revokes the refresh token and clears the cookie
- `GET /api/v1/auth/me` returns the current authenticated user and resolved permissions

Swagger exposes protected endpoints as part of the API contract. To call them from Swagger UI:

1. Execute `POST /api/v1/auth/login`
2. Copy the returned `accessToken`
3. Click `Authorize` in Swagger UI
4. Paste the token into the `bearerAuth` dialog

## Main API Areas

- `/api/v1/auth`
- `/api/v1/users`
- `/api/v1/categories`
- `/api/v1/sla-policies`
- `/api/v1/tickets`
- `/api/v1/notifications`
- `/api/v1/dashboard`

Swagger is the source of truth for the full request and response contract.

## Testing

Run unit and architecture tests:

PowerShell:

```powershell
.\mvnw.cmd test
```

macOS or Linux:

```bash
./mvnw test
```

Run the full quality gate, including integration tests:

PowerShell:

```powershell
.\mvnw.cmd verify
```

macOS or Linux:

```bash
./mvnw verify
```

Notes:

- Web integration tests do not require Docker
- Persistence integration tests use Testcontainers with MySQL
- When Docker is not available, those Testcontainers tests are skipped by design

## Observability

The backend exposes operational endpoints through Spring Boot Actuator:

- `/actuator/health`
- `/actuator/info`
- `/actuator/metrics`
- `/actuator/prometheus`

The application also:

- Propagates `X-Correlation-Id` across requests and error responses
- Records ticket and authentication metrics through Micrometer
- Logs scheduled maintenance execution

## Scheduled Maintenance

The scheduler is enabled in the application startup class and currently runs:

- Hourly purge of expired refresh tokens and idempotency records
- Daily auto-close of resolved tickets at `02:00` server time

## Continuous Integration

GitHub Actions is configured in [.github/workflows/backend-ci.yml](.github/workflows/backend-ci.yml).

The CI pipeline:

- Runs on `push` to `main`
- Runs on `pull_request` targeting `main`
- Uses Java 21 on Ubuntu
- Executes `./mvnw -B -ntp verify`
- Enforces minimal GitHub permissions with `contents: read`
- Cancels older in-progress runs for the same workflow and ref
- Uploads Surefire and Failsafe reports when the build fails

## Repository Structure

```text
src/
  main/
    java/com/tickets/managementtickets/
      bootstrap/
      category/
      dashboard/
      identity/
      notification/
      observability/
      shared/
      sla/
      ticket/
    resources/
      db/migration/
      application.properties
      application-local.properties
      application-test.properties
      application-prod.properties
  test/
    java/com/tickets/managementtickets/
      architecture/
      support/
      ... module tests ...
README.md
README.en.md
```

## Additional Notes

- Database schema changes are managed through Flyway migrations in `src/main/resources/db/migration`
- Reference data such as default categories and SLA setup is created through migrations
- Application services remain plain Java classes and are wired from the composition root in `bootstrap/ApplicationConfiguration`
