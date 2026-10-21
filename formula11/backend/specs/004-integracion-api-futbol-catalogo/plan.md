# Implementation Plan: Integración con API externa de fútbol para poblar el catálogo

**Branch**: `004-integracion-api-futbol-catalogo` | **Date**: 2026-10-07 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/004-integracion-api-futbol-catalogo/spec.md`

## Summary

Agregar una integración manual y protegida con Football-Data.org v4 mediante una capa `adapters`, sin acoplar `JugadorService` a HTTP ni a DTOs del proveedor. El adapter usará `RestClient` con URL base, header de apikey, timeouts, rate limit y caché configurables; devolverá modelos normalizados internos de equipos y jugadores. Un service de sincronización orquestará las cinco ligas, realizará upsert transaccional por `(externalProvider, externalId)` y devolverá un resumen `COMPLETADA`, `PARCIAL` o `NO_EJECUTADA`. La operación no alterará el catálogo local cuando el proveedor falle y se expondrá como `POST /api/jugadores/sincronizacion`, protegido por las reglas JWT existentes.

## Technical Context

**Language/Version**: Java 21, Spring Boot 3.5.5

**Primary Dependencies**: Spring Web `RestClient`, Spring Validation, Spring Data JPA, Spring Security, PostgreSQL, Flyway, springdoc-openapi 2.8.13, JJWT 0.12.6, JUnit 5, Mockito, Spring Test y Testcontainers 1.21.3. Añadir Spring Cache y Caffeine para la caché in-memory configurable.

**Storage**: PostgreSQL 16 mediante JPA/Flyway. `jugadores` se ampliará con `external_provider` y `external_id`, índice único compuesto y una marca de origen/legacy para retirar de forma controlada las filas creadas por V2.

**Testing**: Maven Surefire, unitarios sin red real con `MockRestServiceServer`/mocks del cliente, unitarios de service con adapter mockeado, `@DataJpaTest` + Testcontainers para upsert/constraints y `@SpringBootTest` + MockMvc para endpoint protegido y respuestas parciales.

**Target Platform**: Servicio REST ejecutado sobre JVM con PostgreSQL; Football-Data.org v4 es el proveedor inicial y FutbolOK queda soportado mediante configuración del adapter.

**Project Type**: Web service REST backend.

**Performance Goals**: Una sincronización procesa las ligas de forma secuencial o con concurrencia limitada configurable, respeta el intervalo/límite de requests del plan, usa caché por liga/equipo y no bloquea las consultas del catálogo local.

**Constraints**: La apikey nunca aparece en código, logs, URLs, excepciones ni respuestas; el arranque funciona sin ella; services no conocen HTTP; el endpoint manual requiere JWT; errores externos no borran datos locales; no se añade scheduler; no se modifica frontend ni el contrato de paginación existente.

**Scale/Scope**: Un puerto y adapter externo, modelos normalizados, configuración/caché, migración de identidad externa y legacy, service/controller/DTO de sincronización, documentación operativa y tests unitarios, integración y E2E.

## Constitution Check

*GATE: Must pass before implementation. Re-check after design and tests.*

- [x] **Arquitectura en capas**: Controller delega en `SincronizacionService`; el service usa `FootballDataPort`; el adapter encapsula `RestClient`; repository persiste el modelo local.
- [x] **Adapters explícitos**: ninguna clase de service construye URLs, headers, códigos HTTP ni DTOs de Football-Data.org.
- [x] **Caché obligatoria**: la respuesta externa se cachea en la capa de adapter/infraestructura con TTL y capacidad configurables; los errores no sustituyen entradas válidas.
- [x] **Seguridad**: la apikey proviene de entorno/configuración opcional y el endpoint usa JWT; ningún secreto se incluye en logs o cuerpos de error.
- [x] **Persistencia**: upsert protegido por constraint única `(external_provider, external_id)` y migración explícita para registros legacy del seed V2.
- [x] **Resiliencia**: timeouts, rate limit, `Retry-After`, respuestas parciales y fallos totales se traducen a estados controlados sin romper `GET /api/jugadores`.
- [x] **Calidad**: adapter y service tienen unitarios sin red real, repository tiene integración con PostgreSQL/Testcontainers y el endpoint tiene cobertura MockMvc.
- [x] **Documentación y DoD**: Swagger/OpenAPI, quickstart/README y configuración de entorno se actualizan; no se incluye una apikey real.

No se detectan violaciones constitucionales. La dependencia Caffeine se justifica por el requisito obligatorio de caché y el backend actual no tiene una implementación previa.

## Design

### Adapter boundary

```text
JugadorSincronizacionController
        -> SincronizacionService
        -> FootballDataPort
        -> FootballDataAdapter
        -> RestClient + Cache + RateLimiter
```

`FootballDataPort` expone modelos internos, por ejemplo `obtenerEquipos(Liga)` y `obtenerJugadores(ExternalTeam)`. `FootballDataAdapter` traduce códigos de las cinco ligas, headers y respuestas HTTP del proveedor a `ExternalTeam`/`ExternalPlayer`. El service solo recibe resultados normalizados y excepciones propias de integración.

### Configuration and cache

Crear `FootballApiProperties` con prefijo `app.football-api` y valores externos:

- `api-key` enlazada desde `${FOOTBALL_API_KEY:}` sin valor secreto por defecto.
- `base-url`, `api-key-header` con default `X-Auth-Token`, timeouts de conexión/lectura/respuesta.
- TTL, máximo de entradas, intervalo mínimo entre requests y reintentos limitados.
- Mapeo de liga de dominio a competition code del proveedor.

La configuración debe permitir que Spring arranque con apikey vacía. Una validación de operación, no de arranque, produce `ExternalApiNotConfiguredException`. La caché Caffeine separará respuestas de competiciones, equipos y jugadores por proveedor, liga/equipo y parámetros; solo se escribe tras respuestas válidas.

### Synchronization flow

1. El controller recibe el JWT ya validado por SecurityConfig y delega al service.
2. El service comprueba configuración, crea el contexto de sincronización y procesa exactamente las cinco ligas en orden estable.
3. Para cada liga, el port obtiene equipos y jugadores; el adapter atiende caché antes de hacer HTTP y aplica timeout/rate limit.
4. El service normaliza/valida cada registro, rechaza incompletos y ejecuta upserts locales por proveedor + id externo dentro de una transacción por liga.
5. Una liga fallida se reporta sin borrar sus datos anteriores; las ligas exitosas se confirman y el resultado global pasa a `PARCIAL`.
6. Si ninguna liga puede procesarse, se devuelve error de configuración/disponibilidad sin alterar datos; el catálogo local continúa operando.
7. La respuesta incluye estado global, detalle por liga, creados, actualizados, omitidos y errores sanitizados, junto con el `correlationId` de la solicitud.

### External error mapping

- `401/403`: credencial externa inválida/no autorizada; no se reintenta.
- `429`: error de límite; se respeta `Retry-After` y la política configurable, sin reintentos ilimitados.
- `5xx`, timeout o cuerpo inválido: error transitorio de proveedor; se usa caché vigente si existe y se informa la liga.
- Falta de apikey: operación `NO_EJECUTADA`, respuesta `503` y mensaje operativo sin valor de la clave.
- Fallo parcial: `200` con estado `PARCIAL`; fallo total del proveedor: `503` o estado externo normalizado según el contrato.

### Persistence and legacy seed

Crear una migración Flyway posterior a V2 que agregue `external_provider`, `external_id` y el marcador necesario para distinguir legacy. La migración o una operación inicial explícita debe retirar/migrar las filas exclusivamente provenientes del seed V2 antes de importar datos externos. El repository ofrecerá búsqueda por proveedor/id externo y operaciones de upsert dentro de transacciones; la constraint única será la defensa final contra concurrencia y duplicados.

### Observability

Los logs estructurados del adapter usarán el MDC `correlationId` existente y registrarán solo proveedor, liga/equipo, operación, estado sanitizado, latencia, si hubo cache hit y conteos. Nunca se loguearán headers completos, URLs con secretos, tokens, cuerpos completos ni apikeys. Las excepciones internas conservarán causa técnica para logs controlados, pero sus mensajes HTTP serán sanitizados en español.

## Project Structure

### Documentation (this feature)

```text
specs/004-integracion-api-futbol-catalogo/
├── plan.md              # This file (/speckit-plan command output)
├── spec.md              # Feature requirements (/speckit-specify output)
├── contracts/openapi.yaml # Endpoint de sincronización
└── quickstart.md        # Configuración y ejecución manual
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/formula11/
│   ├── adapter/
│   │   ├── FootballDataPort.java
│   │   ├── FootballDataAdapter.java
│   │   ├── FootballApiProperties.java
│   │   └── FootballDataException.java
│   ├── config/
│   │   └── CacheConfig.java
│   ├── controller/
│   │   └── JugadorSincronizacionController.java
│   ├── dto/
│   │   ├── SincronizacionResponse.java
│   │   └── LigaSyncResultResponse.java
│   ├── model/
│   │   └── Jugador.java                    # externalProvider/externalId/origen
│   ├── persistence/
│   │   └── JugadorRepository.java           # lookup/upsert por identidad externa
│   └── service/
│       └── SincronizacionService.java
├── src/main/resources/
│   ├── application.yml                      # defaults no sensibles
│   ├── application-local.yml                # referencias a variables, nunca claves
│   └── db/migration/V3__identidad_externa_jugadores.sql
├── src/test/java/com/formula11/
│   ├── unit/adapter/FootballDataAdapterTest.java
│   ├── unit/service/SincronizacionServiceTest.java
│   ├── integration/JugadorExternalIdentityIntegrationTest.java
│   └── e2e/JugadorSincronizacionE2ETest.java
└── specs/004-integracion-api-futbol-catalogo/
    ├── contracts/openapi.yaml
    └── quickstart.md
```

**Structure Decision**: Se reutiliza el módulo de jugadores y seguridad existente, pero se crea una capa `adapter` explícita conforme a la constitución. No se agregan llamadas externas al `JugadorService` ni al controller de catálogo, y no se crea scheduler en esta feature.

## Implementation Sequence

1. Confirmar en una investigación acotada los endpoints de Football-Data.org v4 que entregan equipos/jugadores, competition codes, header requerido y límites del plan; dejar la decisión en `research.md` si el plan command lo genera.
2. Añadir dependencias/configuración de Caffeine y properties opcionales sin secretos; comprobar arranque sin `FOOTBALL_API_KEY`.
3. Crear migración de identidad externa, constraint e identificación/control de filas legacy de V2.
4. Implementar modelos normalizados, excepciones sanitizadas, `FootballDataPort` y `FootballDataAdapter` con RestClient, timeout, caché, rate limit y mapeo de errores.
5. Implementar repository/service de sincronización con transacción por liga, upsert idempotente y resultado parcial.
6. Implementar DTO/controller `POST /api/jugadores/sincronizacion`, anotaciones Swagger y reglas de autenticación heredadas.
7. Añadir tests del adapter con HTTP mockeado, service con port mockeado, repository contra Testcontainers y E2E protegido/parcial.
8. Crear/actualizar OpenAPI y quickstart/README con `FOOTBALL_API_KEY`, configuración local sin valor real y comandos de prueba.
9. Ejecutar `mvn test`, validar arranque sin apikey y revisar que logs/errores no contengan secretos.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| Caffeine + rate limit configurable | Caché obligatoria y límites del plan externo | Un `Map` sin TTL o requests ilimitadas no garantiza resiliencia ni cumplimiento del proveedor |
| Migración de identidad y legacy | Upsert por id externo sin duplicar filas del seed V2 | Conservar filas seed sin identidad externa produciría duplicados inevitables |
| Resultado por liga | Permitir `200 PARCIAL` y conservar datos cuando una liga falla | Un error global perdería el trabajo exitoso y ocultaría qué ligas se procesaron |
