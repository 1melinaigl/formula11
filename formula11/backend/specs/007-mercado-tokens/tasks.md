---

description: "Task list for implementing the 007 token market feature"
---

# Tasks: Compra y venta de tokens de jugadores

**Input**: Design documents from `specs/007-mercado-tokens/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/openapi-market.md](./contracts/openapi-market.md), [quickstart.md](./quickstart.md)

**Tests**: Included because the specification requires unit, integration and E2E coverage.

## Phase 1: Setup

- [x] T001 Confirm the existing Maven/Spring Boot test profile and PostgreSQL/Testcontainers conventions in `backend/pom.xml` and `backend/src/test/java/com/formula11`
- [x] T002 [P] Add local/test market configuration defaults (development superuser credentials) and `spring.flyway.out-of-order=true` in `backend/src/main/resources/application-local.yml` and `backend/src/test/resources/application-test.yml`
- [x] T003 [P] Add the market configuration properties `mercado.saldo-inicial` (default `100`) and superuser email/password bound to `SUPERUSUARIO_EMAIL`/`SUPERUSUARIO_PASSWORD` in `backend/src/main/resources/application.yml`
- [x] T004 [P] Add the market operation contract examples to the maintained Postman collection under `backend/postman/` and keep paths aligned with `backend/specs/005-contratos-entrega2/contracts/openapi.yaml`
- [ ] T005 [P] Open the contract PR from branch `005-contrato-403-superusuario` adding a specific `ErrorSuperusuarioNoOpera` response (`403`, "El superusuario no puede operar en el mercado") to `POST /api/ordenes/compra`, `POST /api/ordenes/venta` and `GET /api/usuarios/{usuarioId}/portfolio` in `backend/specs/005-contratos-entrega2/contracts/openapi.yaml`; it needs approval from all three members and MUST be merged before T045

## Phase 2: Foundational

- [x] T006 Create Flyway migration `backend/src/main/resources/db/migration/V20__agregar_rol_usuario.sql` to add the non-null `USUARIO`/`SUPERUSUARIO` role column with a `CHECK`, backfilling existing rows as `USUARIO`, plus a partial unique index allowing at most one `SUPERUSUARIO`
- [x] T007 Create Flyway migration `backend/src/main/resources/db/migration/V21__crear_cuentas.sql` with only the `cuentas` table structure (unique `usuario_id` FK, `saldo NUMERIC(19,4) NOT NULL CHECK (saldo >= 0)`); it inserts no rows
- [x] T008 Create Flyway migration `backend/src/main/resources/db/migration/V22__crear_tenencias.sql` for `tenencias` with FKs, unique `(usuario_id, jugador_id)`, `CHECK (cantidad >= 0)`, non-negative `precio_promedio NUMERIC(19,4)`, and an index on `jugador_id`
- [x] T009 Create Flyway migration `backend/src/main/resources/db/migration/V23__crear_transacciones.sql` for `COMPRA`/`VENTA`, user/player/counterparty FKs, positive quantity, four-decimal unit price/total, nullable strategy/version, `TIMESTAMPTZ` date, and an index on `(usuario_id, fecha DESC)`
- [x] T010 Create Flyway migration `backend/src/main/resources/db/migration/V24__crear_auditoria_mercado.sql` for append-only audit rows with author FK, operation FK, `jsonb` before/after snapshots, and timestamp
- [x] T011 Create Flyway migration `backend/src/main/resources/db/migration/V25__backfill_cuentas_existentes.sql` assigning every existing `USUARIO` exactly one account with `100.0000` credits
- [x] T012 Add `Rol`, `Cuenta`, `Tenencia`, `Transaccion`, and `AuditoriaMercado` domain entities with explicit constructors/mutators enforcing non-negative balances, non-negative holdings, positive transaction quantities, and four-decimal monetary normalization in `backend/src/main/java/com/formula11/model/`
- [x] T013 Extend `backend/src/main/java/com/formula11/model/Usuario.java` with the role value, default `USUARIO` behavior, and an `esSuperusuario()` query, without breaking existing constructors/tests
- [x] T014 Add repository interfaces and pessimistic-lock queries for accounts and holdings in `backend/src/main/java/com/formula11/persistence/CuentaRepository.java`, `TenenciaRepository.java`, `TransaccionRepository.java`, and `AuditoriaMercadoRepository.java`; expose no audit update/delete workflow
- [x] T015 Update `backend/src/main/java/com/formula11/security/JwtService.java` and `JwtAuthenticationFilter.java` to emit/read the `rol` claim while preserving existing `usuarioId` and email authentication
- [x] T016 Add `backend/src/main/java/com/formula11/config/ClockConfig.java` with an injectable `Clock`, and `MercadoProperties` with profile-aware validation of superuser credentials in `backend/src/main/java/com/formula11/config/`
- [x] T017 Add domain exceptions (including `SuperusuarioNoOperaException`) and map them to `400`, `403`, `404`, `409`, and `422` with Spanish `Error` responses in `backend/src/main/java/com/formula11/exception/GlobalExceptionHandler.java`

**Checkpoint**: Migrations validate against the existing schema, entities/repositories compile, JWT roles are available, and no user story starts before these prerequisites pass.

## Phase 3: User Story 1 - Comprar tokens (Priority: P1)

**Goal**: Let an authenticated `USUARIO` buy tokens from the superuser atomically.

**Independent Test**: With 100 superuser tokens and a user balance of 100 credits, execute a two-token purchase and verify `201`, balance 98, buyer quantity 2, superuser holding 98, one transaction, and one audit row with both sides before/after.

- [x] T018 [P] [US1] Add unit tests for `Cuenta`, `Tenencia`, weighted-average purchase, and invalid non-positive states in `backend/src/test/java/com/formula11/unit/model/MercadoModelTest.java`
- [x] T019 [P] [US1] Add fixed-clock unit tests for successful purchase, insufficient balance `422`, insufficient superuser stock `409`, superuser buyer `403`, invalid quote, and atomic failure in `backend/src/test/java/com/formula11/unit/service/MercadoServiceTest.java`
- [x] T020 [US1] Define `CotizacionVigentePort` and its quote value object in `backend/src/main/java/com/formula11/service/CotizacionVigentePort.java`
- [x] T021 [US1] Implement `CotizacionFijaAdapter` returning price `1.0000` with null strategy/version/date in `backend/src/main/java/com/formula11/adapter/CotizacionFijaAdapter.java`
- [x] T022 [US1] Implement `MercadoService.comprar(usuarioId, jugadorId, cantidad)`: reject the superuser, resolve player and superuser, get the quote with the `Clock` date, lock user account → superuser holding → user holding, validate balance/stock, debit the user balance, transfer tokens, update the weighted average, persist the transaction and an audit row with user balance, user tokens and superuser tokens before/after, all in one transaction, in `backend/src/main/java/com/formula11/service/MercadoService.java`
- [x] T023 [US1] Add `OrdenRequest`, `OperacionResponse`, and market mapping helpers matching the exact contract fields and four-decimal monetary representation in `backend/src/main/java/com/formula11/dto/`
- [x] T024 [US1] Add `POST /api/ordenes/compra` to `backend/src/main/java/com/formula11/controller/MercadoController.java`, deriving the buyer from JWT and delegating exclusively to `MercadoService.comprar`
- [x] T025 [US1] Add E2E coverage for successful purchase and `400/401/403/404/409/422` responses in `backend/src/test/java/com/formula11/e2e/MercadoE2ETest.java`

## Phase 4: User Story 2 - Vender tokens (Priority: P1)

**Goal**: Let an authenticated `USUARIO` sell owned tokens to the superuser atomically.

**Independent Test**: Seed a user holding two tokens, execute a two-token sale, and verify `201`, credited balance, user quantity zero with preserved average price, superuser holding restored, one transaction, and one audit row.

- [x] T026 [P] [US2] Extend fixed-clock service unit tests for successful sale, insufficient holding `422`, superuser seller `403`, zero-quantity holding retention, and rollback in `backend/src/test/java/com/formula11/unit/service/MercadoServiceTest.java`
- [x] T027 [US2] Implement `MercadoService.vender(usuarioId, jugadorId, cantidad)` using the same superuser check, quote, lock ordering, atomic transfer, transaction, and audit path as purchase in `backend/src/main/java/com/formula11/service/MercadoService.java`
- [x] T028 [US2] Add `POST /api/ordenes/venta` to `backend/src/main/java/com/formula11/controller/MercadoController.java`, deriving the seller from JWT and delegating exclusively to `MercadoService.vender`
- [x] T029 [US2] Extend `MercadoE2ETest.java` with successful sale, insufficient holding `422`, superuser `403`, invalid request `400`, missing JWT `401`, and no-partial-change assertions

## Phase 5: User Story 3 - Consultar mercado (Priority: P1)

**Goal**: Expose issued tokens, superuser stock, and current quote for a player.

**Independent Test**: Query an existing player with JWT and verify `200 MercadoJugadorResponse` containing 100 issued tokens, current superuser holding, price 1, and null quote date.

- [x] T030 [P] [US3] Add `MercadoJugadorResponse` with exact contract fields and nullability in `backend/src/main/java/com/formula11/dto/MercadoJugadorResponse.java`
- [x] T031 [US3] Implement a read-only market query in `MercadoService` using player lookup, superuser holding, and `CotizacionVigentePort` in `backend/src/main/java/com/formula11/service/MercadoService.java`
- [x] T032 [US3] Add `GET /api/jugadores/{jugadorId}/mercado` to `backend/src/main/java/com/formula11/controller/MercadoJugadorController.java` with contract-compatible `200/401/404` behavior
- [x] T033 [US3] Extend `MercadoE2ETest.java` with existing-player, nonexistent-player, missing-JWT, and post-purchase stock assertions

## Phase 6: User Story 4 - Cuentas, roles y superusuario (Priority: P1)

**Goal**: Provision user accounts and one idempotent superuser (no account) with 100 tokens per player.

**Independent Test**: Start a clean local/test application, verify one superuser without account and with 100-token holdings, register a user with 100 credits and role `USUARIO`, and verify role in login response/JWT for both.

- [x] T034 [P] [US4] Extend `UsuarioService` registration to create a `Cuenta` with `mercado.saldo-inicial` credits and role `USUARIO`, taking the registration date from the injected `Clock` instead of `Instant.now()` in `Usuario`, in `backend/src/main/java/com/formula11/service/UsuarioService.java` and `backend/src/main/java/com/formula11/model/Usuario.java`
- [x] T035 [P] [US4] Add `rol` to `LoginResponse` and to JWT generation in `backend/src/main/java/com/formula11/dto/LoginResponse.java` and `backend/src/main/java/com/formula11/security/JwtService.java`; `RegistroUsuarioResponse` does not change because contract 005 does not include the role there
- [x] T036 [US4] Implement idempotent `SuperusuarioInitializer` as an `ApplicationRunner` that reads profile defaults or required environment variables, fails explicitly outside local/test when credentials are missing, creates the `SUPERUSUARIO` row in `usuarios` without an account, and inserts a 100-token holding (average price 1) for every player that lacks one, in `backend/src/main/java/com/formula11/service/SuperusuarioInitializer.java`
- [x] T037 [US4] Update `SincronizacionService.sincronizar()` so that, in the same transaction: (a) `LEGACY` players without transactions are deleted after deleting their superuser holding, replacing the plain `deleteAllByOrigen("LEGACY")`; (b) `LEGACY` players with transactions are kept; (c) each newly persisted player receives a 100-token superuser holding, in `backend/src/main/java/com/formula11/service/SincronizacionService.java`, `JugadorRepository.java`, and `TenenciaRepository.java`
- [x] T038 [US4] Add integration tests for migrations, existing-user account backfill, superuser without account, the single-superuser unique index, idempotent initializer (run twice, no duplicates), required production credentials, local/test defaults, new-player seeding, and synchronization with legacy players with and without transactions in `backend/src/test/java/com/formula11/integration/MercadoPersistenceIntegrationTest.java`
- [x] T039 [US4] Add E2E assertions for registration account creation and login role/JWT claim for `USUARIO` and `SUPERUSUARIO` in `backend/src/test/java/com/formula11/e2e/MercadoE2ETest.java`

## Phase 7: User Story 5 - Servicios reutilizables (Priority: P2)

**Goal**: Ensure internal consumers use the same service methods as HTTP.

**Independent Test**: Invoke `comprar` and `vender` directly with a fixed Clock and verify identical results, validations, transaction rows, and audit rows without a controller.

- [x] T040 [P] [US5] Add direct service tests proving `comprar` and `vender` accept domain inputs without HTTP/DTO dependencies, reject the superuser, and use the injected Clock in `backend/src/test/java/com/formula11/unit/service/MercadoServiceTest.java`

## Phase 8: Polish & Cross-Cutting Concerns

- [x] T041 [P] Add OpenAPI annotations/response documentation for the three market endpoints (including the superuser `403`) and the login role change in `backend/src/main/java/com/formula11/controller/MercadoController.java`, `UsuarioController.java`, and `backend/src/main/java/com/formula11/config/OpenApiConfig.java`
- [x] T042 [P] Add structured market operation logs using the existing correlation ID without logging passwords, JWTs, or sensitive snapshots in `backend/src/main/java/com/formula11/service/MercadoService.java`
- [x] T043 [P] Add repository/integration assertions that `AuditoriaMercado` has no update/delete path, stays append-only, and stores both sides (user balance, user tokens, superuser tokens) in `backend/src/test/java/com/formula11/integration/MercadoPersistenceIntegrationTest.java`
- [x] T043a Add a PostgreSQL/Testcontainers concurrency integration test where two simultaneous transactions try to buy the last tokens of a player and to spend the same balance; assert exactly one succeeds, no negative balances or holdings, and the 100-token sum per player holds, in `backend/src/test/java/com/formula11/integration/MercadoConcurrenciaIntegrationTest.java`
- [x] T044 Run the focused market unit/integration/E2E suite and then the complete Maven test suite from `backend/`; resolve failures without modifying or deleting existing tests
- [x] T045 Validate the manual scenarios in `specs/007-mercado-tokens/quickstart.md`, the OpenAPI contract in `specs/005-contratos-entrega2/contracts/openapi.yaml`, and the Postman collection before marking the feature complete

## Dependencies & Execution Order

### Story dependency graph

```text
Setup (T001-T005)
  -> Foundational (T006-T017)
      -> US1 Compra (T018-T025)
          -> US2 Venta (T026-T029)
          -> US3 Mercado (T030-T033)
          -> US4 Cuentas/roles/superusuario (T034-T039)
              -> US5 Service reutilizable (T040)
                  -> Polish (T041-T045)
```

US2 and US3 can proceed in parallel after US1's shared `MercadoService`/DTO foundation is available. US4 may proceed in parallel with US2/US3 once foundational migrations and repositories are complete. US5 depends on the finalized service API from US1 and US2. T005 (contract PR) can be opened at any time and does not block implementation, but it blocks closing the feature: it must be merged before T045.

### Parallel execution examples

```text
# After foundational work:
Stream A: T018-T025 (US1 unit tests, quote adapter, purchase service/controller/E2E)
Stream B: T006-T011 (Flyway migrations, after schema review)
Stream C: T015-T017 (JWT, Clock, exception mapping)

# After US1 service API exists:
Stream A: T026-T029 (US2 sale)
Stream B: T030-T033 (US3 market query)
Stream C: T034-T039 (US4 accounts, roles, initializer)

# Final:
Stream A: T041-T042 (OpenAPI and logging)
Stream B: T043-T045 (append-only checks and validation)
```

## Implementation Strategy

1. **Foundation first**: establish schema, domain invariants, repositories, JWT role support, Clock, and error mapping.
2. **MVP**: complete US1 purchase with fixed quote and atomic persistence.
3. **Incremental market**: add sale and market availability query.
4. **Operational identity**: complete account provisioning, role propagation, and idempotent superuser seeding.
5. **Reuse and hardening**: verify direct service consumers, append-only audit, concurrency behavior, documentation, and full regression suite.
