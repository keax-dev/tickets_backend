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
- Local startup inside Docker Compose together with MySQL and the frontend

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
- Docker
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
- MySQL 8.x if you want to run the backend outside Docker Compose
- Docker Desktop or Docker Engine if you want to use the full Compose stack or run Testcontainers-based persistence tests locally

## Configuration

The main application configuration lives in [src/main/resources/application.properties](src/main/resources/application.properties).

This backend uses a **single** `application.properties` file for the main runtime configuration. Sensitive and environment-specific values are injected through environment variables.

### Relevant Environment Variables

| Variable | Default | Purpose |
| --- | --- | --- |
| `JWT_SECRET` | none | Required secret used to sign JWT access tokens |
| `DB_HOST` | `localhost` | MySQL host |
| `DB_PORT` | `3306` | MySQL port |
| `DB_NAME` | `management_tickets` | Database name |
| `DB_USERNAME` | `root` | Database username |
| `DB_PASSWORD` | none | Database password |
| `APP_SECURITY_ALLOWED_ORIGIN_1` | `http://localhost:4200` | First allowed CORS origin |
| `APP_SECURITY_ALLOWED_ORIGIN_2` | `http://127.0.0.1:4200` | Second allowed CORS origin |
| `APP_SECURITY_REFRESH_COOKIE_SECURE` | `false` | Whether the refresh cookie requires HTTPS |

## Quick Start with Docker Compose

The recommended full-system workflow lives in the `tickets-frontend` repository, because that is where `docker-compose.yml` orchestrates:

- MySQL
- this backend
- the Angular frontend served through Nginx

### Expected layout

```text
Projects/
  tickets-frontend/
  tickets-backend/
```

### Steps

1. Clone both repositories as sibling folders.
2. In `tickets-frontend`, create `.env` from `.env.example`.
3. From `tickets-frontend`, run:

```powershell
docker compose up --build
```

### Useful URLs after startup

- Backend API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Frontend: `http://localhost:4200`

### What happens on first startup

If the database is empty:

- Flyway validates and applies migrations
- tables and constraints are created
- reference data such as categories and baseline SLA configuration is inserted

That leaves the system ready to test without manually preparing the schema.

### Demo users and credentials

The migrations also create demo users ready for manual authentication. All active users share the same password:

```text
Password123!
```

Active users by role:

| User type | Role | User / email |
| --- | --- | --- |
| Administrator | `ADMIN` | `admin.demo@tickets.local` |
| Support manager | `SUPPORT_MANAGER` | `manager.demo@tickets.local` |
| Support agent | `SUPPORT_AGENT` | `agent.alvarez@tickets.local` |
| Support agent | `SUPPORT_AGENT` | `agent.nunez@tickets.local` |
| Support agent | `SUPPORT_AGENT` | `agent.ortega@tickets.local` |
| Customer | `CUSTOMER` | `customer.finance@tickets.local` |
| Customer | `CUSTOMER` | `customer.operations@tickets.local` |
| Customer | `CUSTOMER` | `customer.sales@tickets.local` |
| Customer | `CUSTOMER` | `customer.hr@tickets.local` |
| Customer | `CUSTOMER` | `customer.executive@tickets.local` |

Additional seeded users:

| Type | Status | User / email | Usage |
| --- | --- | --- | --- |
| Support agent | Inactive | `agent.inactive@tickets.local` | For denied-login testing with inactive accounts |
| Customer | Inactive | `customer.inactive@tickets.local` | For denied-login testing with inactive accounts |
| Technical system actor | Inactive | `system@tickets.local` | Internal automation user, not intended for interactive sign-in |

### Reset the database from scratch

If you want to remove the persisted Docker data and force a clean initialization:

```powershell
docker compose down -v
docker compose up --build
```

## Running the Backend Locally Without Docker Compose

1. Create the database:

```sql
CREATE DATABASE management_tickets
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

2. Export the minimum required variables.

PowerShell:

```powershell
$env:JWT_SECRET = "replace-with-a-secret-of-at-least-32-characters"
$env:DB_HOST = "localhost"
$env:DB_PORT = "3306"
$env:DB_NAME = "management_tickets"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "replace-with-your-password"
.\mvnw.cmd spring-boot:run
```

macOS or Linux:

```bash
export JWT_SECRET="replace-with-a-secret-of-at-least-32-characters"
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=management_tickets
export DB_USERNAME=root
export DB_PASSWORD="replace-with-your-password"
./mvnw spring-boot:run
```

3. Open Swagger UI:

- `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Flyway validates and applies migrations at startup.

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
  test/
    java/com/tickets/managementtickets/
      architecture/
      support/
      ... module tests ...
README.md
README.en.md
Dockerfile
```

## Additional Notes

- Database schema changes are managed through Flyway migrations in `src/main/resources/db/migration`
- Reference data such as default categories and baseline SLA setup is created through migrations
- The backend `Dockerfile` builds the project with Maven and then runs the `.jar` on a lighter JRE image
- Application services remain plain Java classes and are wired from the composition root in `bootstrap/ApplicationConfiguration`
