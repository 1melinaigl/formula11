# Implementation Plan: Login de usuarios

**Branch**: `002-login-usuarios` | **Date**: 2026-09-23 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/002-login-usuarios/spec.md`

## Summary

Agregar el login público `POST /api/usuarios/login` reutilizando el modelo de usuario y los componentes de seguridad existentes. El controller recibirá `LoginRequest`, el service normalizará el email, buscará el usuario, validará el hash BCrypt y emitirá un JWT mediante `JwtService`; cualquier fallo de email o contraseña se convertirá en la misma excepción de credenciales inválidas y respuesta `401`. Se actualizarán las reglas de seguridad, el handler global, Swagger/OpenAPI, Postman y las pruebas unitarias, de integración y E2E con MockMvc sobre `@SpringBootTest`.

## Technical Context

**Language/Version**: Java 21, Spring Boot 3.5.5

**Primary Dependencies**: Spring Web, Spring Validation, Spring Data JPA, Spring Security, BCrypt, JJWT 0.12.6, springdoc-openapi 2.8.13, JUnit 5, Mockito, MockMvc, Testcontainers 1.21.3

**Storage**: PostgreSQL 16 en pruebas con Testcontainers; esquema existente `usuarios` con email normalizado mediante índice único.

**Testing**: Maven Surefire, JUnit 5, Mockito, `@DataJpaTest`, `@SpringBootTest` + MockMvc y Testcontainers PostgreSQL.

**Target Platform**: Servicio REST ejecutado sobre JVM con PostgreSQL.

**Project Type**: Web service REST backend.

**Performance Goals**: El login debe realizar una sola búsqueda por email normalizado y una verificación BCrypt por solicitud; no se introduce una meta de throughput nueva en esta feature.

**Constraints**: Mantener las capas Controller → Service → Model/Persistence; no exponer diferencias entre email inexistente y contraseña incorrecta; registro y login son públicos y el resto de endpoints continúa protegido; no agregar recuperación de contraseña, bloqueo por intentos ni refresh tokens; no modificar constructores ni firmas públicas de `UsuarioService` ni de otras clases existentes; los cambios sobre código existente deben ser únicamente aditivos para conservar los tests actuales sin cambios.

**Scale/Scope**: Un endpoint y dos DTOs nuevos, una excepción de dominio/aplicación, una regla de autorización, documentación contractual y cobertura de login en los paquetes de tests existentes.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **Arquitectura**: el controller solo delega en `UsuarioService`; el service coordina repositorio, modelo y `JwtService`.
- [x] **Seguridad**: el JWT existente y BCrypt se reutilizan; el login queda explícitamente público y cualquier otro endpoint permanece autenticado.
- [x] **Validación**: `LoginRequest` valida presencia, tamaño y formato; `Usuario.normalizarEmail` conserva la regla de normalización del registro.
- [x] **Errores**: la excepción de credenciales inválidas se mapea a `401` con `ErrorResponse` (`mensaje`, `timestamp`, `correlationId`).
- [x] **Calidad**: se incluyen unitarios, integración contra PostgreSQL real y E2E con MockMvc sobre `@SpringBootTest` en `com.formula11.e2e`.
- [x] **Documentación y DoD**: se actualizan contrato OpenAPI, anotaciones Swagger y colección Postman.

No se detectan violaciones constitucionales; no se requiere Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/002-login-usuarios/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/openapi.yaml
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)
```text
backend/
├── src/main/java/com/formula11/
│   ├── controller/UsuarioController.java
│   ├── dto/{LoginRequest,LoginResponse}.java
│   ├── exception/GlobalExceptionHandler.java
│   ├── model/Usuario.java
│   ├── persistence/UsuarioRepository.java
│   ├── security/{JwtService,SecurityConfig}.java
│   └── service/UsuarioService.java
├── src/test/java/com/formula11/
│   ├── unit/service/UsuarioLoginServiceTest.java
│   ├── integration/UsuarioLoginIntegrationTest.java
│   └── e2e/LoginUsuarioE2ETest.java
├── postman/formula11-registro-catalogo.json
└── specs/002-login-usuarios/contracts/openapi.yaml
```

**Structure Decision**: Se mantiene el backend Spring Boot existente y sus paquetes por responsabilidad. La implementación modifica el flujo de usuario existente en lugar de crear un módulo de autenticación paralelo. El contrato versionado de la feature vive bajo `specs/002-login-usuarios/contracts/`; Swagger se genera desde las anotaciones del controller y DTOs.

## Complexity Tracking

No aplica: el diseño reutiliza las capas, dependencias y componentes de seguridad existentes sin introducir complejidad arquitectónica adicional.
