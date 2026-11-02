# Implementation Plan: Compra y venta de tokens de jugadores

**Branch**: `007-mercado-tokens` | **Date**: 2026-10-24 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/007-mercado-tokens/spec.md`

## Summary

Implementar el mercado de tokens sobre el backend Spring Boot existente. La solución agregará cuentas, roles, tenencias, transacciones y auditoría append-only; creará de forma idempotente el superusuario al arrancar; asignará 100 tokens por jugador al superusuario; y expondrá las operaciones del contrato 005 para consultar mercado, comprar y vender. La lógica de compra/venta vivirá en `MercadoService`, será reutilizable fuera de HTTP, usará `CotizacionVigentePort` con una implementación fija de 1 crédito, un `Clock` inyectado y bloqueos pesimistas para mantener la consistencia transaccional.

## Technical Context

**Language/Version**: Java 21

**Primary Dependencies**: Spring Boot 3.5.5, Spring Web, Spring Validation, Spring Data JPA, Spring Security/JJWT, Flyway, PostgreSQL, Testcontainers, springdoc-openapi

**Storage**: PostgreSQL existente, validado por Hibernate/Flyway; migraciones nuevas en `src/main/resources/db/migration/V20__...sql` a `V25__...sql` (V26-V29 libres)

**Testing**: JUnit 5/Spring Boot Test, MockMvc para E2E, Testcontainers PostgreSQL para integración, unit tests de dominio/service con `Clock.fixed`

**Target Platform**: Backend HTTP stateless ejecutado en JVM

**Project Type**: Web service monolítico en capas

**Performance Goals**: Mantener las latencias actuales del backend para consultas de mercado y órdenes; las operaciones concurrentes deben serializarse por cuenta/tenencia sin permitir doble gasto ni sobreventa.

**Constraints**: Rutas, esquemas y códigos HTTP deben coincidir con el contrato 005; precios y saldos son `BigDecimal` con escala 4 y `HALF_UP`; cantidades de tokens son enteras entre 1 y 100; errores funcionales en español; no modificar ni borrar tests existentes.

**Scale/Scope**: Catálogo existente de jugadores, una cuenta por usuario con rol `USUARIO` (el superusuario no tiene cuenta), una tenencia por par usuario-jugador, un superusuario único, y operaciones de mercado acotadas al contrato de la Entrega 2.

## Constitution Check

*GATE: Must pass before Phase 0 research and after Phase 1 design.*

- **Arquitectura en capas**: PASS. Controllers traducen HTTP; `MercadoService` orquesta; modelos preservan invariantes; repositories encapsulan persistencia.
- **Modelo rico de dominio financiero**: PASS. `Cuenta`, `Tenencia`, `Transaccion` y `AuditoriaMercado` tendrán invariantes y relaciones explícitas; el puerto de cotización evita acoplar el mercado a la estrategia futura.
- **Validación por nivel**: PASS. DTO valida forma/rango; service valida existencia, rol, saldo, stock y tenencia; modelos validan estados no negativos y transacciones válidas.
- **Seguridad**: PASS. Se reutiliza JWT, se agrega el rol al token/respuesta, las rutas de mercado permanecen protegidas y el superusuario recibe `403` si intenta comprar o vender.
- **Tiempo reproducible**: PASS. Los services reciben `Clock`; no usarán `Instant.now()`/`LocalDate.now()` directamente.
- **Operaciones reutilizables**: PASS. Controllers delegan en `MercadoService.comprar` y `MercadoService.vender`.
- **Auditoría y calidad**: PASS. La operación será atómica, la auditoría solo insertable y se agregarán unit, integration y E2E tests sin eliminar los existentes.
- **Documentación**: PASS. OpenAPI/Swagger y Postman se actualizarán conforme al contrato 005.

## Project Structure

### Documentation (this feature)

```text
specs/007-mercado-tokens/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── openapi-market.md
└── tasks.md                 # generado por /speckit-tasks
```

### Source Code (repository root)

```text
backend/
├── src/main/java/com/formula11/
│   ├── adapter/
│   │   └── CotizacionFijaAdapter.java
│   ├── config/
│   │   ├── ClockConfig.java
│   │   └── MercadoProperties.java
│   ├── controller/
│   │   ├── MercadoController.java
│   │   └── UsuarioController.java       # cambios de registro/login
│   ├── dto/
│   │   ├── OrdenRequest.java
│   │   ├── OperacionResponse.java
│   │   └── MercadoJugadorResponse.java
│   ├── exception/
│   │   ├── ...                           # excepciones de mercado
│   │   └── GlobalExceptionHandler.java
│   ├── model/
│   │   ├── Cuenta.java
│   │   ├── Tenencia.java
│   │   ├── Transaccion.java
│   │   ├── AuditoriaMercado.java
│   │   ├── Rol.java
│   │   └── Usuario.java                  # rol y fecha controlada
│   ├── persistence/
│   │   ├── CuentaRepository.java
│   │   ├── TenenciaRepository.java
│   │   ├── TransaccionRepository.java
│   │   └── AuditoriaMercadoRepository.java
│   ├── security/
│   │   ├── JwtService.java               # claim rol
│   │   └── JwtAuthenticationFilter.java
│   └── service/
│       ├── MercadoService.java
│       ├── CotizacionVigentePort.java
│       ├── UsuarioService.java           # cuenta/rol
│       └── SuperusuarioInitializer.java  # ApplicationRunner
├── src/main/resources/
│   ├── application.yml
│   ├── application-local.yml
│   ├── application-test.yml
│   └── db/migration/
│       ├── V20__agregar_rol_usuario.sql
│       ├── V21__crear_cuentas.sql
│       ├── V22__crear_tenencias.sql
│       ├── V23__crear_transacciones.sql
│       ├── V24__crear_auditoria_mercado.sql
│       └── V25__backfill_cuentas_existentes.sql
└── src/test/java/com/formula11/
    ├── unit/model/
    ├── unit/service/MercadoServiceTest.java
    ├── integration/
    └── e2e/MercadoE2ETest.java
```

**Structure Decision**: Se mantiene el monolito Spring Boot existente y sus paquetes `controller`, `service`, `persistence`, `model`, `dto`, `exception` y `security`. `adapter` se usa para la implementación de `CotizacionVigentePort`; no se agrega un módulo o aplicación independiente.

## Complexity Tracking

No hay violaciones constitucionales que requieran justificación.
