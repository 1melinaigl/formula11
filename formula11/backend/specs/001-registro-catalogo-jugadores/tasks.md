# Tasks: Registro de usuarios y catálogo de jugadores

**Input**: Design documents from `backend/specs/001-registro-catalogo-jugadores/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/openapi.yaml](contracts/openapi.yaml)

**Tests**: Incluidos porque la constitución exige tests unitarios, de integración y E2E, y la especificación define escenarios verificables.

**Path conventions**: Todas las rutas de implementación parten de `backend/`.

## Phase 1: Setup

**Purpose**: Crear el proyecto Java 21/Spring Boot desde cero y dejar Maven ejecutable.

- [x] T001 Crear `backend/pom.xml` con Java 21, Spring Boot 3.5.x estable, Web, Validation, Data JPA, Security, PostgreSQL, Flyway, Actuator, JWT, springdoc-openapi, JUnit 5, MockMvc y Testcontainers.
- [x] T002 [P] Crear `backend/src/main/java/com/formula11/Formula11Application.java` y la estructura de paquetes `controller`, `service`, `model`, `persistence`, `security`, `dto`, `exception` y `config`.
- [x] T003 [P] Crear `backend/src/test/java/com/formula11/unit/model`, `backend/src/test/java/com/formula11/integration` y `backend/src/test/java/com/formula11/e2e`, con configuración base de tests.
- [x] T004 [P] Configurar `backend/src/main/resources/application.yml`, `backend/src/main/resources/application-local.yml` y `backend/src/test/resources/application-test.yml`, asegurando que solo `local` use PostgreSQL `formula11` con `postgres/root`.
- [x] T005 Verificar que `mvn test` ejecuta el proyecto inicial desde `backend/` antes de agregar funcionalidad.

## Phase 2: Foundational Infrastructure

**Purpose**: Completar las capacidades que bloquean todas las user stories.

- [x] T006 Crear migraciones Flyway en `backend/src/main/resources/db/migration/` para tablas `usuarios` y `jugadores`, índice único sobre email normalizado y constraints de campos obligatorios.
- [x] T007 [P] Implementar `backend/src/main/java/com/formula11/model/Liga.java` con exactamente las cinco ligas permitidas y su representación legible en español/contrato.
- [x] T008 [P] Implementar `backend/src/main/java/com/formula11/model/Usuario.java` y `backend/src/main/java/com/formula11/model/Jugador.java` con invariantes de dominio, mapeo JPA y exclusión de `passwordHash` de serialización/logs.
- [x] T009 [P] Crear `backend/src/main/java/com/formula11/persistence/UsuarioRepository.java` y `backend/src/main/java/com/formula11/persistence/JugadorRepository.java`, incluyendo búsqueda por email normalizado y consulta ordenada del catálogo.
- [x] T010 Implementar `backend/src/main/java/com/formula11/security/JwtService.java` para firmar y validar tokens con subject de usuario, expiración configurable y secreto externo a la lógica de negocio.
- [x] T011 Implementar `backend/src/main/java/com/formula11/security/JwtAuthenticationFilter.java` y `backend/src/main/java/com/formula11/security/SecurityConfig.java` con sesiones stateless, BCrypt, filtro Bearer y registro como única ruta pública.
- [x] T012 Implementar `backend/src/main/java/com/formula11/exception/GlobalExceptionHandler.java` y DTOs de error en `backend/src/main/java/com/formula11/dto/`, cubriendo validación, duplicados, no autorizado, recurso inexistente y errores inesperados en español.
- [x] T013 [P] Implementar `backend/src/main/java/com/formula11/config/CorrelationIdFilter.java` para aceptar/generar `X-Correlation-Id`, propagarlo a MDC y devolverlo en la respuesta.
- [x] T014 [P] Configurar logs estructurados, Actuator health/metrics y exclusión de passwords, secretos y JWT completos en `backend/src/main/resources/application.yml`.
- [ ] T015 Ejecutar `mvn test` y comprobar que la aplicación inicia con el perfil `local` sin que los tests lean `application-local.yml`.

**Checkpoint**: Proyecto compilable, persistencia migrable, seguridad y manejo transversal de errores disponibles para las historias.

## Phase 3: User Story 1 - Registrar usuario y obtener credencial (Priority: P1)

**Goal**: Registrar una identidad válida, guardar la contraseña hasheada y devolver un JWT utilizable.

**Independent Test**: Ejecutar el E2E de registro contra PostgreSQL de Testcontainers y verificar respuesta `201`, usuario persistido, token válido y ausencia de password en la respuesta.

### Tests for User Story 1

- [x] T016 [P] [US1] Crear tests unitarios de normalización e invariantes de usuario en `backend/src/test/java/com/formula11/unit/model/UsuarioTest.java`, incluyendo nombre/email vacíos, espacios y email en mayúsculas.
- [x] T017 [P] [US1] Crear tests de integración de repositorio y unicidad en `backend/src/test/java/com/formula11/integration/UsuarioRepositoryIntegrationTest.java` con PostgreSQL Testcontainers, incluyendo duplicado por mayúsculas/espacios.
- [x] T018 [P] [US1] Crear tests E2E de `POST /api/usuarios/registro` en `backend/src/test/java/com/formula11/e2e/RegistroCatalogoE2ETest.java` para alta válida, validación `400`, duplicado `409` y password ausente/inválido sin emisión de token.

### Implementation for User Story 1

- [x] T019 [P] [US1] Crear `RegistroUsuarioRequest`, `RegistroUsuarioResponse` y DTOs relacionados en `backend/src/main/java/com/formula11/dto/`, con Bean Validation, trimming y tipos requeridos.
- [x] T020 [US1] Implementar `backend/src/main/java/com/formula11/service/UsuarioService.java` para normalizar email, validar duplicados, hashear con BCrypt, persistir y solicitar a `JwtService` la emisión del token.
- [x] T021 [US1] Implementar `backend/src/main/java/com/formula11/controller/UsuarioController.java` con `POST /api/usuarios/registro`, status `201`, respuesta sin `passwordHash` y delegación exclusiva al service.
- [x] T022 [US1] Mapear conflictos de índice único y validaciones de registro a mensajes en español mediante `GlobalExceptionHandler`, garantizando que no se cree un usuario parcial.
- [x] T023 [US1] Ejecutar los tests de `UsuarioTest`, `UsuarioRepositoryIntegrationTest` y `RegistroCatalogoE2ETest`, verificando que un JWT emitido pueda presentarse ante una ruta protegida.

**Checkpoint**: Registro funcional, idempotencia ante email repetido, password protegido y credencial emitida.

## Phase 4: User Story 2 - Proteger el acceso a endpoints (Priority: P1)

**Goal**: Bloquear antes del controller toda solicitud protegida sin JWT, con JWT inválido o vencido.

**Independent Test**: Usar un endpoint protegido de catálogo como recurso de prueba y verificar respuestas `401` para ausencia, firma inválida y expiración; el token válido debe continuar al service.

### Tests for User Story 2

- [x] T024 [P] [US2] Crear tests unitarios de firma y expiración en `backend/src/test/java/com/formula11/unit/security/JwtServiceTest.java`.
- [ ] T025 [P] [US2] Crear tests de filtro/configuración en `backend/src/test/java/com/formula11/integration/SecurityIntegrationTest.java` para Bearer ausente, malformado, inválido, vencido y válido.
- [x] T026 [P] [US2] Cubrir protección E2E en `backend/src/test/java/com/formula11/e2e/RegistroCatalogoE2ETest.java`, comprobando credencial ausente e inválida y acceso válido al recurso protegido.

### Implementation for User Story 2

- [x] T027 [US2] Completar `backend/src/main/java/com/formula11/security/JwtAuthenticationFilter.java` para extraer el Bearer token, validar firma/expiración y establecer el principal autenticado sin consultar lógica de negocio innecesaria.
- [x] T028 [US2] Completar `backend/src/main/java/com/formula11/security/SecurityConfig.java` para permitir únicamente `POST /api/usuarios/registro`, deshabilitar sesión/form login y devolver `401` JSON en español.
- [x] T029 [US2] Integrar la identidad JWT con el contexto de Spring Security y documentar en `backend/src/main/java/com/formula11/security/` que no existe login separado en esta feature.
- [x] T030 [US2] Ejecutar los tests unitarios y E2E de seguridad y comprobar que los errores no exponen stack traces, secretos ni tokens completos.

**Checkpoint**: Todas las rutas distintas del registro requieren una credencial JWT vigente.

## Phase 5: User Story 3 - Consultar el catálogo de jugadores (Priority: P2)

**Goal**: Permitir a cualquier usuario autenticado consultar todos los jugadores con sus datos y liga válida.

**Independent Test**: Insertar jugadores de las cinco ligas en un contenedor PostgreSQL, consultar con JWT válido y verificar todos los campos; el catálogo vacío debe responder `200 []`.

### Tests for User Story 3

- [x] T031 [P] [US3] Crear tests unitarios de `Liga` y construcción de jugador en `backend/src/test/java/com/formula11/unit/model/JugadorTest.java`, incluyendo rechazo de liga nula/no permitida.
- [x] T032 [P] [US3] Crear tests de integración de catálogo en `backend/src/test/java/com/formula11/integration/JugadorRepositoryIntegrationTest.java`, con las cinco ligas y catálogo vacío.
- [x] T033 [P] [US3] Cubrir E2E de `GET /api/jugadores` en `backend/src/test/java/com/formula11/e2e/RegistroCatalogoE2ETest.java`, con JWT válido, sin token, token inválido y respuesta sin cotización/precio.

### Implementation for User Story 3

- [x] T034 [P] [US3] Crear `JugadorResponse` y mapeadores en `backend/src/main/java/com/formula11/dto/`, exponiendo solo id, nombre, equipo, liga y posición.
- [x] T035 [US3] Implementar `backend/src/main/java/com/formula11/service/JugadorService.java` para consultar el repository y devolver una lista válida, incluida la lista vacía.
- [x] T036 [US3] Implementar `backend/src/main/java/com/formula11/controller/JugadorController.java` con `GET /api/jugadores`, autenticación heredada de SecurityConfig y delegación exclusiva al service.
- [ ] T037 [US3] Agregar fixtures de prueba controlados en `backend/src/test/resources/` para jugadores de las cinco ligas sin introducir cotizaciones, precios o transacciones.
- [x] T038 [US3] Ejecutar todos los tests de catálogo y verificar que cada respuesta contiene únicamente ligas permitidas y nunca campos monetarios.

**Checkpoint**: Catálogo autenticado completo y vacío funcionan, con contrato estable y sin dependencia de cotizaciones.

## Phase 6: Polish & Cross-Cutting Validation

**Purpose**: Cerrar el Definition of Done y dejar la feature operable/documentada.

- [x] T039 [P] Crear/actualizar `backend/postman/formula11-registro-catalogo.json` con registro, uso del Bearer token, catálogo y casos de error principales.
- [x] T040 [P] Verificar que `backend/specs/001-registro-catalogo-jugadores/contracts/openapi.yaml` coincide con mappings, DTOs, status codes, esquema de errores y enum de ligas implementados.
- [ ] T041 [P] Actualizar `backend/specs/001-registro-catalogo-jugadores/quickstart.md` si cambian nombres de variables, comandos, rutas o configuración final.
- [x] T042 Ejecutar la suite completa con `mvn test` desde `backend/`, usando Docker/Testcontainers y sin activar el perfil local.
- [ ] T043 Ejecutar el arranque local con `mvn spring-boot:run -Dspring-boot.run.profiles=local` contra `jdbc:postgresql://localhost:5432/formula11` y validar manualmente Swagger, registro y catálogo.
- [ ] T044 Revisar logs, correlation ID, Actuator y respuestas de error para confirmar el cumplimiento de observabilidad y mensajes en español.
- [ ] T045 Confirmar la matriz de requisitos FR-001 a FR-011 y criterios SC-001 a SC-005 en `backend/specs/001-registro-catalogo-jugadores/` antes de dar la feature por terminada.

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin dependencias.
- **Foundational (Phase 2)**: Depende de Setup y bloquea las user stories.
- **US1 y US2 (Phase 3-4)**: Dependen de Foundational; ambas son P1 y comparten JWT, BCrypt y manejo de errores.
- **US3 (Phase 5)**: Depende de Foundational y de la protección JWT de US2 para su flujo autenticado.
- **Polish (Phase 6)**: Depende de las tres historias y de todos sus tests.

### User Story Dependencies

- **US1**: Puede comenzar después de Foundational.
- **US2**: Puede comenzar después de Foundational, aunque su E2E usa el token producido por US1.
- **US3**: Requiere la infraestructura de US2 para probar correctamente acceso autenticado; el service de catálogo permanece independiente de registro.

### Parallel Opportunities

- En Setup, T002, T003 y T004 pueden ejecutarse en paralelo después de definir `pom.xml`.
- En Foundational, T007-T009, T013 y T014 pueden trabajarse en paralelo, coordinando el contrato de entidades.
- Los tests unitarios de cada historia pueden escribirse en paralelo con otros tests, pero deben fallar antes de implementar la funcionalidad correspondiente.
- T039-T041 son independientes y pueden ejecutarse en paralelo una vez estabilizados endpoints y DTOs.

## Implementation Strategy

### MVP First

1. Completar Setup y Foundational.
2. Completar US1 y US2, incluyendo sus tests.
3. Validar registro y autenticación contra Testcontainers.
4. Completar US3 y validar catálogo.
5. Ejecutar la fase de Polish y el quickstart completo.

### Definition of Done

La feature está terminada cuando `mvn test` pasa usando Testcontainers, la aplicación levanta con `local`, el registro devuelve JWT, las rutas protegidas rechazan credenciales inválidas, el catálogo devuelve solo campos permitidos, OpenAPI/Postman están actualizados y observabilidad/correlation ID funcionan.

## Estado de pendientes

- T015 y T043 requieren levantar la aplicación con el perfil `local` contra la base PostgreSQL del desarrollador.
- T025 requiere una clase de integración de seguridad separada; la cobertura actual está incluida en el E2E y en `JwtServiceTest`.
- T037 usa datos creados dentro de los tests; todavía no hay fixtures estáticos en `src/test/resources/`.
- T041 requiere revisar el quickstart si se modifican comandos o configuración operativa.
- T044 y T045 requieren una revisión operativa/documental final de observabilidad y trazabilidad de requisitos.
