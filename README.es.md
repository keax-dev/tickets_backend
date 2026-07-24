[![Language: English](https://img.shields.io/badge/Language-English-0A66C2?style=for-the-badge)](README.md)
[![Idioma: Español](https://img.shields.io/badge/Idioma-Espa%C3%B1ol-6A1B9A?style=for-the-badge)](README.es.md)

# Management Tickets Backend

Backend en Spring Boot 4 para gestion de tickets, autenticacion, administracion de SLA, notificaciones, catalogos y reportes de dashboard.

[Architecture Guide (English)](docs/ARCHITECTURE.md) | [Guia de Arquitectura (Espanol)](docs/ARCHITECTURE.es.md)

## Vision General

Este proyecto expone una API REST para un sistema de mesa de ayuda orientado a tickets. Soporta:

- Autenticacion basada en JWT con rotacion de refresh token
- Control de acceso por roles para administradores, managers, agentes y clientes
- Gestion del ciclo de vida del ticket desde su creacion hasta su cierre
- Comentarios e historial de tickets
- Administracion de politicas SLA y resumenes de dashboard
- Notificaciones, categorias, metricas de observabilidad y mantenimiento programado

## Stack Tecnologico

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
- GitHub Actions para CI

## Modulos Funcionales

El backend esta organizado por bounded contexts:

- `identity` - Autenticacion, usuarios, roles, permisos, refresh tokens
- `ticket` - Tickets, comentarios, historial, generacion de codigos, idempotencia, ciclo de vida
- `category` - Categorias de ticket y gestion de estado
- `sla` - Reglas SLA por prioridad
- `notification` - Notificaciones de usuario y marcacion de lectura
- `dashboard` - Endpoints de resumen y actividad reciente
- `shared` - Excepciones compartidas, helpers de seguridad, helpers web y puertos transaccionales
- `observability` - Adaptadores de metricas y gauges
- `bootstrap` - Arranque de la aplicacion, composition root y mantenimiento programado

Para entender el por que de esta estructura, revisa [docs/ARCHITECTURE.es.md](docs/ARCHITECTURE.es.md).

## Requisitos

- Java 21 o superior
- Maven 3.9 o superior si no usas el wrapper
- MySQL 8.x
- Docker es opcional y solo hace falta para correr localmente las pruebas de integracion de persistencia con Testcontainers

## Configuracion

La configuracion principal vive en [src/main/resources/application.properties](src/main/resources/application.properties).

### Variables de Entorno Requeridas

| Variable | Valor por defecto | Proposito |
| --- | --- | --- |
| `JWT_SECRET` | none | Secreto obligatorio para firmar los JWT de acceso |
| `DB_HOST` | `localhost` | Host de MySQL |
| `DB_PORT` | `3306` | Puerto de MySQL |
| `DB_NAME` | `management_tickets` | Nombre de la base de datos |
| `DB_USERNAME` | `root` | Usuario de base de datos |
| `DB_PASSWORD` | empty | Contrasena de base de datos |
| `SPRING_PROFILES_ACTIVE` | none | Perfil opcional de Spring, normalmente `local` en desarrollo |

### Perfiles Disponibles

- `default` - Configuracion base
- `local` - Ajustes para desarrollo local
- `test` - Propiedades de soporte para pruebas
- `prod` - Overrides de produccion para actuator

## Ejecucion Local

1. Crea la base de datos:

```sql
CREATE DATABASE management_tickets
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

2. Exporta el secreto JWT y opcionalmente activa el perfil local.

PowerShell:

```powershell
$env:JWT_SECRET = "reemplaza-por-un-secreto-de-al-menos-32-caracteres"
$env:SPRING_PROFILES_ACTIVE = "local"
.\mvnw.cmd spring-boot:run
```

macOS o Linux:

```bash
export JWT_SECRET="reemplaza-por-un-secreto-de-al-menos-32-caracteres"
export SPRING_PROFILES_ACTIVE=local
./mvnw spring-boot:run
```

3. Abre Swagger UI:

- `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Flyway validara y aplicara las migraciones al arrancar.

## Flujo de Autenticacion

- `POST /api/v1/auth/login` Autentica al usuario y devuelve:
  - Un JWT access token en el body
  - Un refresh token rotado como cookie HttpOnly
- `POST /api/v1/auth/refresh` Usa la cookie de refresh para emitir un nuevo access token
- `POST /api/v1/auth/logout` Revoca el refresh token y limpia la cookie
- `GET /api/v1/auth/me` Devuelve el usuario autenticado y sus permisos resueltos

Swagger muestra tambien los endpoints protegidos porque forman parte del contrato de la API. Para consumirlos desde Swagger UI:

1. Ejecuta `POST /api/v1/auth/login`
2. Copia el `accessToken` recibido
3. Haz clic en `Authorize` dentro de Swagger UI
4. Pega el token en el dialogo `bearerAuth`

## Principales Areas de la API

- `/api/v1/auth`
- `/api/v1/users`
- `/api/v1/categories`
- `/api/v1/sla-policies`
- `/api/v1/tickets`
- `/api/v1/notifications`
- `/api/v1/dashboard`

Swagger es la fuente de verdad para el contrato completo de requests y responses.

## Pruebas

Ejecutar pruebas unitarias y de arquitectura:

PowerShell:

```powershell
.\mvnw.cmd test
```

macOS o Linux:

```bash
./mvnw test
```

Ejecutar el quality gate completo, incluyendo pruebas de integracion:

PowerShell:

```powershell
.\mvnw.cmd verify
```

macOS o Linux:

```bash
./mvnw verify
```

Notas:

- Las pruebas de integracion web no necesitan Docker
- Las pruebas de persistencia usan Testcontainers con MySQL
- Cuando Docker no esta disponible, esas pruebas se saltan intencionalmente

## Observabilidad

El backend expone endpoints operativos mediante Spring Boot Actuator:

- `/actuator/health`
- `/actuator/info`
- `/actuator/metrics`
- `/actuator/prometheus`

La aplicacion tambien:

- Propaga `X-Correlation-Id` en requests y respuestas de error
- Registra metricas de tickets y autenticacion con Micrometer
- Deja trazas de ejecucion para las tareas de mantenimiento programado

## Mantenimiento Programado

El scheduler esta habilitado desde la clase principal y actualmente ejecuta:

- Limpieza horaria de refresh tokens expirados y registros de idempotencia
- Cierre automatico diario de tickets resueltos a las `02:00` de la hora del servidor

## Integracion Continua

GitHub Actions esta configurado en [.github/workflows/backend-ci.yml](.github/workflows/backend-ci.yml).

El pipeline de CI:

- Corre en `push` a `main`
- Corre en `pull_request` hacia `main`
- Usa Java 21 sobre Ubuntu
- Ejecuta `./mvnw -B -ntp verify`
- Define permisos minimos con `contents: read`
- Cancela ejecuciones antiguas en progreso para el mismo workflow y ref
- Sube reportes de Surefire y Failsafe cuando falla el build

## Estructura del Repositorio

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
      ... pruebas por modulo ...
```

## Notas Adicionales

- Los cambios de esquema de base de datos se gestionan con Flyway en `src/main/resources/db/migration`
- Los datos de referencia, como categorias por defecto y configuracion inicial de SLA, se crean con migraciones
- Los application services se mantienen como clases Java puras y se componen desde `bootstrap/ApplicationConfiguration`
