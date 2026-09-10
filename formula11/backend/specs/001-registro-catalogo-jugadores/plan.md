# Implementation Plan: Registro de usuarios y catálogo de jugadores

**Branch**: `001-registro-catalogo-jugadores` | **Date**: 2026-09-09 | **Spec**: [spec.md](spec.md)

## Summary

Crear desde cero un backend Spring Boot para registrar usuarios, emitir un JWT firmado durante el registro y exponer un catálogo autenticado de jugadores. La implementación seguirá Controller -> Service -> Model/Persistence: los controllers solo delegan, los services orquestan validaciones y persistencia, y el modelo mantiene invariantes como las cinco ligas permitidas. La contraseña se almacenará con BCrypt y nunca se expondrá.

## Technical Context

**Language/Version**: Java 21

**Primary Dependencies**: Spring Boot 3.5.x (última versión estable disponible de la rama 3.x al crear el proyecto), Spring Web, Spring Validation, Spring Data JPA, Hibernate, Spring Security, PostgreSQL Driver, biblioteca JWT (jjwt), springdoc-openapi, Actuator, Testcontainers, JUnit 5 y MockMvc

**Storage**: PostgreSQL, base `formula11` en desarrollo local; los tests usan PostgreSQL de Testcontainers y nunca el perfil local

**Testing**: JUnit 5, tests unitarios de dominio sin Spring/DB, tests de integración de services/repositories con Testcontainers, tests E2E de API en paquete separado

**Target Platform**: JVM 21, servidor Linux/Windows con Docker disponible para tests

**Project Type**: Web service REST

**Performance Goals**: Respuestas de lectura y registro adecuadas para el catálogo inicial; no se fija un SLO de carga en esta feature

**Constraints**: El registro es el único endpoint público; JWT stateless; email único normalizado; errores y mensajes funcionales en español; sin login separado ni cotizaciones

**Scale/Scope**: Dos entidades persistentes, dos flujos REST, un catálogo consultable y la base de seguridad reutilizable para futuras features

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Arquitectura en capas**: PASS. `controller`, `service`, `model` y `persistence` serán paquetes explícitos. Controller solo llamará a Service y Model no conocerá persistencia.
- **Modelo rico**: PASS. `Liga` y las invariantes de `Usuario`/`Jugador` estarán encapsuladas en el modelo; cotizaciones y mercado quedan fuera de alcance.
- **Validación por nivel**: PASS. DTOs validan forma/saneamiento, Services verifican existencia y duplicados, y entidades/modelos protegen invariantes.
- **Seguridad**: PASS. Solo `POST /api/usuarios/registro` será público; el filtro JWT bloqueará solicitudes inválidas antes del controller.
- **Observabilidad**: PASS. Se incluirán correlation ID, logs estructurados y Actuator health/metrics.
- **Tests y calidad**: PASS. Se planifican unitarios de dominio, integración con Testcontainers y E2E aislados.
- **DoD**: PASS condicionado a implementar OpenAPI, colección Postman y configuración local junto con los tests.

## Project Structure

### Documentation (this feature)

```text
backend/specs/001-registro-catalogo-jugadores/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/
    └── openapi.yaml
```

### Source Code

```text
backend/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/formula11/
│   │   │   ├── Formula11Application.java
│   │   │   ├── controller/
│   │   │   │   ├── UsuarioController.java
│   │   │   │   └── JugadorController.java
│   │   │   ├── service/
│   │   │   │   ├── UsuarioService.java
│   │   │   │   └── JugadorService.java
│   │   │   ├── model/
│   │   │   │   ├── Usuario.java
│   │   │   │   ├── Jugador.java
│   │   │   │   └── Liga.java
│   │   │   ├── persistence/
│   │   │   │   ├── UsuarioRepository.java
│   │   │   │   └── JugadorRepository.java
│   │   │   ├── security/
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   ├── JwtService.java
│   │   │   │   └── SecurityConfig.java
│   │   │   ├── dto/
│   │   │   ├── exception/
│   │   │   └── config/
│   │   └── resources/
│   │       ├── application.yml
│   │       └── application-local.yml
│   └── test/
│       ├── java/com/formula11/
│       │   ├── unit/model/
│       │   ├── integration/
│       │   └── e2e/
│       └── resources/
│           └── application-test.yml
└── postman/
    └── formula11-registro-catalogo.json
```

**Structure Decision**: Se selecciona un único proyecto Maven dentro de `backend`, con paquetes por responsabilidad. `frontend` permanece separado y no se modifica en esta feature.

## Complexity Tracking

No hay violaciones de la constitución que requieran justificar complejidad adicional.
