# Implementation Plan: Paginación del catálogo de jugadores

**Branch**: `003-paginacion-catalogo-jugadores` | **Date**: 2026-10-07 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/003-paginacion-catalogo-jugadores/spec.md`

## Summary

Modificar `GET /api/jugadores` para consultar el catálogo con paginación, orden y filtros, manteniendo la autenticación actual. El repository expondrá una consulta paginada basada en `Pageable` y predicados dinámicos; el service validará y compondrá los parámetros permitidos, mapeará cada `Jugador` a `JugadorResponse` y construirá un DTO de respuesta propio con `content`, `page`, `size`, `totalElements` y `totalPages`. El controller declarará los query params y las anotaciones Swagger, y los tests existentes de catálogo se adaptarán al nuevo contrato.

## Technical Context

**Language/Version**: Java 21, Spring Boot 3.5.5

**Primary Dependencies**: Spring Web, Spring Data JPA, Spring Security, springdoc-openapi 2.8.13, JUnit 5, Mockito, MockMvc y Testcontainers 1.21.3; no se requiere una dependencia nueva.

**Storage**: PostgreSQL 16 mediante Spring Data JPA; el modelo `Jugador` existente contiene `id`, `nombre`, `equipo`, `liga` y `posicion`.

**Testing**: Maven Surefire, tests unitarios de `JugadorService`, integración `@DataJpaTest` para `JugadorRepository` y E2E `@SpringBootTest` + MockMvc contra PostgreSQL en Testcontainers.

**Target Platform**: Servicio REST ejecutado sobre JVM con PostgreSQL.

**Project Type**: Web service REST backend.

**Performance Goals**: La base de datos debe aplicar filtros, orden y paginación antes de materializar resultados; no se debe cargar el catálogo completo en memoria para responder una página.

**Constraints**: Mantener `GET /api/jugadores` y su protección JWT actual; `page` es base 0, `size` por defecto 20 y máximo 100; `sort` admite un único criterio `campo,direccion`; los campos de orden son `id`, `nombre`, `equipo`, `liga` y `posicion`; los filtros son `liga`, `equipo` y `posicion`; los filtros de texto son búsquedas parciales normalizadas; los errores usan `ErrorResponse`; no exponer directamente `Page` de Spring; no modificar el frontend.

**Scale/Scope**: Un DTO de respuesta paginada, validación y composición de consulta en el service, ampliación del repository, actualización del controller y documentación OpenAPI versionada, más cobertura unitaria, de integración y E2E.

## Constitution Check

*GATE: Must pass before implementation. Re-check after design and tests.*

- [x] **Arquitectura**: se mantienen las capas Controller → Service → Repository/Model; el controller no construye consultas ni transforma entidades.
- [x] **Persistencia y rendimiento**: filtros, orden y límites se delegan a PostgreSQL mediante Spring Data JPA; no se conserva el `findAll` completo para una solicitud paginada.
- [x] **Contrato**: se introduce un DTO estable propio y no se serializa `Page` directamente; los nombres de parámetros y metadatos coinciden con la spec.
- [x] **Validación y errores**: el service rechaza campos de orden, direcciones, filtros y rangos no permitidos con `IllegalArgumentException`, reutilizando `GlobalExceptionHandler` y `ErrorResponse`.
- [x] **Seguridad**: no se modifica `SecurityConfig`; el endpoint continúa requiriendo el JWT actual.
- [x] **Calidad**: se cubren service, repository y endpoint E2E, incluyendo combinaciones de filtros, paginación, página fuera de rango y errores `400`.
- [x] **Documentación**: se actualizan las anotaciones Swagger y se crea el contrato OpenAPI de la feature.

No se detectan violaciones constitucionales ni necesidad de una dependencia externa.

## Design

### Request and validation flow

1. `JugadorController` recibe `page`, `size`, `sort`, `liga`, `equipo` y `posicion` con defaults documentados.
2. `JugadorService` valida límites, formato y valores permitidos; convierte `sort` a un `Sort` seguro mediante una lista blanca de campos y direcciones.
3. El service normaliza los filtros de texto y construye predicados de coincidencia parcial sin distinguir mayúsculas/minúsculas. `liga` se convierte a la representación de `Liga` usada por la persistencia después de validar su valor público.
4. `JugadorRepository` ejecuta una consulta `Page<Jugador>` con `Pageable` y los predicados; los filtros se aplican antes del conteo y del contenido paginado.
5. El service mapea el contenido a `JugadorResponse` y devuelve `JugadoresPageResponse`, calculando `totalPages` como 0 para un conjunto vacío y conservando el índice solicitado en páginas fuera de rango.

### Stable ordering

Cuando no se proporciona `sort`, se aplica `nombre ASC` y `id ASC`. Cuando se proporciona un criterio válido, se aplica el criterio solicitado y `id ASC` como desempate para evitar cambios de página entre consultas cuando existan valores iguales.

### Error handling

Los errores de query params se expresan como `IllegalArgumentException` con mensajes en español y pasan por `GlobalExceptionHandler`, que genera el `ErrorResponse` estándar. La ausencia o invalidez del JWT sigue siendo responsabilidad de la configuración de seguridad existente.

## Project Structure

### Documentation (this feature)

```text
specs/003-paginacion-catalogo-jugadores/
├── plan.md              # This file (/speckit-plan command output)
├── spec.md              # Feature requirements (/speckit-specify output)
└── contracts/openapi.yaml # Contract updated/created during implementation
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/formula11/
│   ├── controller/JugadorController.java       # Query params y Swagger
│   ├── dto/JugadoresPageResponse.java          # Contrato paginado propio
│   ├── persistence/JugadorRepository.java      # Consulta Page + predicados
│   └── service/JugadorService.java             # Validación, consulta y mapeo
├── src/test/java/com/formula11/
│   ├── unit/service/JugadorServiceTest.java   # Defaults, validación y mapeo
│   ├── integration/JugadorRepositoryIntegrationTest.java # Filtros/orden/paginación SQL
│   └── e2e/RegistroCatalogoE2ETest.java        # HTTP, seguridad y contrato JSON
└── specs/003-paginacion-catalogo-jugadores/
    └── contracts/openapi.yaml                  # Contrato versionado de la feature
```

**Structure Decision**: Se reutiliza el módulo de catálogo existente y se agrega únicamente el DTO que representa la página. La consulta dinámica permanece en el repository/service del catálogo; no se crea un módulo ni un endpoint paralelo. Los tests E2E existentes de registro y autenticación permanecen en su clase y solo se actualizan las expectativas del catálogo.

## Implementation Sequence

1. Crear el DTO `JugadoresPageResponse` con los cinco metadatos y `List<JugadorResponse> content`.
2. Ampliar `JugadorRepository` con soporte de especificaciones/paginación sin eliminar la compatibilidad necesaria para los tests y usos existentes.
3. Actualizar `JugadorService` para validar parámetros, construir filtros y orden seguro, aplicar defaults y mapear la página.
4. Actualizar `JugadorController` para recibir los query params, devolver el DTO y documentar parámetros, respuesta `200`, `400` y `401`.
5. Ajustar el manejo de parámetros inválidos si los mensajes no atraviesan correctamente `GlobalExceptionHandler`.
6. Crear o actualizar `contracts/openapi.yaml` con el nuevo esquema `JugadoresPageResponse`, parámetros y respuestas.
7. Actualizar tests unitarios, de repository y E2E; ejecutar primero los tests del catálogo y luego la suite completa.

## Complexity Tracking

No aplica: la solución utiliza capacidades existentes de Spring Data JPA y añade solo una respuesta de transporte y validación explícita. La única complejidad adicional es la lista blanca de campos de orden y la construcción de predicados combinables, necesaria para evitar consultas inválidas y conservar un contrato controlado.
