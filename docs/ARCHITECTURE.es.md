[![Language: English](https://img.shields.io/badge/Language-English-0A66C2?style=for-the-badge)](ARCHITECTURE.md)
[![Idioma: Español](https://img.shields.io/badge/Idioma-Espa%C3%B1ol-6A1B9A?style=for-the-badge)](ARCHITECTURE.es.md)

# Guia de Arquitectura

[Back to README](../README.md) | [Volver al README en Espanol](../README.es.md)

## Proposito

Este documento explica como el backend aplica Clean Architecture y DDD en terminos practicos, como se relacionan las capas y por que el proyecto esta organizado por modulos de negocio en lugar de limitarse a carpetas tecnicas.

## Estilo Arquitectonico

El backend sigue:

- Clean Architecture para la direccion de dependencias y el aislamiento del framework
- Modularizacion estilo DDD mediante bounded contexts
- Composicion explicita en la capa bootstrap, en lugar de anotar los application services con estereotipos de Spring

A alto nivel, las dependencias apuntan hacia adentro:

```text
adaptadores de infraestructura -> application -> domain
bootstrap -> application + infrastructure
```

Esto significa que el dominio no conoce Spring, HTTP, JPA ni JSON. La capa application orquesta casos de uso a traves de puertos. La infraestructura implementa esos puertos.

## Bounded Contexts

Cada paquete de primer nivel debajo de `com.tickets.managementtickets` se trata como un slice arquitectonico:

| Contexto | Responsabilidad |
| --- | --- |
| `identity` | Autenticacion, autorizacion, usuarios, roles, permisos, refresh tokens |
| `ticket` | Ciclo de vida del ticket, comentarios, historial, visibilidad, idempotencia, codigos |
| `category` | Categorias de ticket y cambios de estado de categoria |
| `sla` | Gestion de politicas SLA y reglas por prioridad |
| `notification` | Almacenamiento de notificaciones, listado y marcacion de lectura |
| `dashboard` | Resumenes de dashboard y actividad reciente |
| `shared` | Excepciones compartidas, puertos y soporte base de infraestructura |
| `observability` | Adaptadores de metricas y gauges runtime |
| `bootstrap` | Arranque, cableado de beans y scheduling |

## Por Que Comentarios, Historial, Secuencia e Idempotencia Siguen Dentro de `ticket`

El bounded context `ticket` no solo es dueño de la entidad `Ticket`. Tambien es dueño de:

- Comentarios del ticket
- Historial del ticket
- Generacion de secuencia o codigo del ticket
- Registros de idempotencia
- Politicas de ciclo de vida del ticket

Estos no son contextos de negocio separados. Son conceptos de soporte del ciclo de vida del ticket, asi que mantenerlos dentro del modulo `ticket` preserva cohesion y evita una fragmentacion artificial.

Dentro de ese bounded context sigue existiendo separacion por capas:

- `domain` para estado y reglas de negocio
- `application` para casos de uso y puertos
- `infrastructure` para adaptadores web, persistencia y soporte tecnico

## Responsabilidades por Capa

### Capa Domain

Ubicaciones de ejemplo:

- `ticket/domain/model`
- `identity/domain/model`
- `sla/domain/model`

Responsabilidades:

- Representar conceptos y estado del negocio
- Hacer cumplir invariantes del dominio
- Mantenerse libre de frameworks

Ejemplos:

- `Ticket`
- `TicketStatus`
- `TicketComment`
- `User`
- `Role`
- `SlaPolicy`

La capa domain no debe depender de:

- Spring
- APIs Jakarta
- Application services
- Adaptadores de infraestructura

### Capa Application

Ubicaciones de ejemplo:

- `ticket/application/service`
- `identity/application/service`
- `ticket/application/port`
- `identity/application/port`

Responsabilidades:

- Orquestar casos de uso
- Cargar y persistir objetos de dominio mediante puertos
- Coordinar politicas, validacion, notificaciones, historial y transacciones
- Exponer commands, queries, results y ports

Decision importante de diseno:

- Los application services son clases Java puras
- Se instancian desde el composition root en `bootstrap/ApplicationConfiguration`
- Intencionalmente no llevan `@Service` ni `@Component`

Esto mantiene la capa application independiente de Spring y mas alineada a Clean Architecture.

### Capa Infrastructure

Ubicaciones de ejemplo:

- `infrastructure/web`
- `infrastructure/persistence`
- `identity/infrastructure/security`
- `observability/infrastructure`

Responsabilidades:

- Exponer controllers HTTP y DTOs de transporte
- Mapear request y response models
- Implementar puertos de repositorio con JPA
- Implementar adaptadores de seguridad
- Implementar adaptadores de metricas
- Proveer componentes tecnicos runtime como filtros y rate limiters

Las anotaciones de Spring pertenecen sobre todo aqui:

- `@RestController`
- `@Repository`
- `@Component`
- `@Service` para adaptadores tecnicos cuando aplique

### Capa Bootstrap

Ubicacion:

- `bootstrap/ApplicationConfiguration`
- `bootstrap/MaintenanceScheduler`

Responsabilidades:

- Definir el composition root
- Cablear application services con sus puertos y politicas
- Habilitar scheduling
- Mantener la configuracion del framework fuera de los casos de uso

Este es el punto clave que reemplaza la necesidad de anotar los application services con `@Service`.

## Flujo de una Request

Recorrido tipico de una request:

1. Un controller en `infrastructure/web/controller` recibe la request HTTP
2. Un DTO web se valida y se mapea a un command o query de application
3. Un application service coordina el caso de uso
4. Los objetos de dominio hacen cumplir transiciones y reglas de negocio
5. Se invocan puertos de repositorio
6. Los adaptadores de infraestructura persisten y mapean datos
7. Los results de application se transforman a DTOs web
8. El controller devuelve la respuesta

Ejemplo en tickets:

```text
TicketController
  -> TicketService
    -> TicketRepositoryPort
    -> TicketHistoryRepositoryPort
    -> TicketCommentRepositoryPort
    -> NotificationRepositoryPort
    -> transiciones del modelo de dominio
```

## Diseno de Seguridad y Autenticacion

El modelo de seguridad combina:

- JWT access tokens para llamadas API
- Rotacion de refresh token mediante cookies HttpOnly
- Validacion de roles y permisos desde el modulo `identity`
- Limitacion de intentos de login mediante `LoginRateLimiter`

Endpoints publicos:

- `/api/v1/auth/login`
- `/api/v1/auth/refresh`
- Endpoints de Swagger y OpenAPI
- Endpoint de health

Todos los demas endpoints de negocio requieren autenticacion.

## Persistencia y Migraciones

La persistencia usa:

- Entidades JPA en `infrastructure/persistence/entity`
- Repositorios Spring Data en `infrastructure/persistence/repository`
- Adapters en `infrastructure/persistence/adapter`

La evolucion del esquema se maneja con migraciones Flyway en:

- `src/main/resources/db/migration`

Las migraciones cubren:

- Tablas de identidad
- Tablas de catalogo y categorias
- Tablas de tickets
- Tablas de notificaciones
- Datos semilla de categorias y SLA
- Soporte para bloqueo de login y actor del sistema

## Observabilidad y Comportamiento Runtime

La observabilidad se implementa mediante:

- `CorrelationIdFilter` para `X-Correlation-Id`
- `ApiExceptionHandler` para respuestas problem detail estructuradas
- Adaptadores de metricas Micrometer en `observability/infrastructure`
- Endpoints Actuator para health, metrics y Prometheus

El scheduler ejecuta:

- Limpieza horaria de refresh tokens expirados y registros de idempotencia
- Cierre automatico diario de tickets resueltos

## Cumplimiento Arquitectonico con ArchUnit

El proyecto incluye [ArchitectureRulesTest](../src/test/java/com/tickets/managementtickets/architecture/ArchitectureRulesTest.java), que protege la arquitectura esperada.

Las reglas actuales verifican que:

- Application no dependa de infraestructura ni de frameworks
- Los controllers web no accedan directamente a repositorios de persistencia
- Application no dependa de DTOs web
- Los application services no lleven estereotipos de Spring
- Web no dependa de detalles de implementacion de persistencia
- Persistencia no dependa de web
- Los DTOs web no dependan de application services
- Domain no dependa de application, infraestructura ni frameworks
- Las entidades de persistencia no dependan de application
- Los modulos de primer nivel no formen ciclos de dependencias

## Estrategia de Pruebas

La piramide de pruebas actual incluye:

- Pruebas unitarias de dominio y application services
- Pruebas arquitectonicas con ArchUnit
- Pruebas de integracion web para auth y tickets
- Pruebas de integracion de persistencia con JPA y MySQL via Testcontainers

Esta mezcla aporta:

- Feedback rapido para reglas de negocio
- Proteccion de limites arquitectonicos
- Verificacion realista del comportamiento HTTP
- Verificacion realista de mapeos y queries JPA

## Resumen

El proyecto no esta agrupado solo por nombres de entidad. Esta agrupado por bounded context y, dentro de cada contexto, separa claramente domain, application e infrastructure. Por eso comentarios, historial, secuencia y mecanicas relacionadas siguen viviendo dentro del modulo `ticket` sin romper la arquitectura.
