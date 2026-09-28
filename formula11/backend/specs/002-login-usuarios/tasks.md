---

description: "Tareas de implementación del login de usuarios"
---

# Tasks: Login de usuarios

**Input**: Design documents from `/specs/002-login-usuarios/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/openapi.yaml`

**Tests**: Incluidos porque la especificación exige pruebas unitarias, de integración con PostgreSQL/Testcontainers y E2E.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirmar la infraestructura existente y preparar la superficie contractual sin agregar dependencias nuevas.

- [X] T001 Confirmar en `backend/pom.xml` que Spring Validation, Spring Security, JJWT, springdoc, JUnit 5 y Testcontainers PostgreSQL existentes cubren la feature; no agregar dependencias nuevas.
- [X] T002 [P] Añadir la operación `POST /api/usuarios/login` y sus esquemas `LoginRequest`, `LoginResponse` y errores `400/401` en `backend/specs/002-login-usuarios/contracts/openapi.yaml`.
- [X] T003 [P] Actualizar `backend/postman/formula11-registro-catalogo.json` con la solicitud pública `Iniciar sesión` (email y contraseña) y un script que capture el `token` de la respuesta exitosa en la variable de colección.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Crear los contratos y reglas compartidas que bloquean el flujo de login.

- [X] T004 [P] Crear el record `LoginRequest` en `backend/src/main/java/com/formula11/dto/LoginRequest.java` con `email` obligatorio, máximo 254 caracteres, formato email válido, y `password` obligatoria con entre 8 y 72 caracteres.
- [X] T005 [P] Crear el record `LoginResponse` en `backend/src/main/java/com/formula11/dto/LoginResponse.java` con exactamente `id`, `nombre`, `email` y `token`, sin contraseña, hash ni `fechaRegistro`.
- [X] T006 Crear `CredencialesInvalidasException` en `backend/src/main/java/com/formula11/exception/CredencialesInvalidasException.java`, con un único mensaje genérico para email inexistente y contraseña incorrecta.
- [X] T007 Actualizar `backend/src/main/java/com/formula11/exception/GlobalExceptionHandler.java` para mapear `CredencialesInvalidasException` a `401` mediante `ErrorResponse(mensaje, timestamp, correlationId)`.

**Checkpoint**: Los DTOs y el contrato de error están definidos; el login aún no está expuesto ni autenticando.

---

## Phase 3: User Story 1 - Ingreso de usuario registrado (Priority: P1) 🎯 MVP

**Goal**: Permitir que un usuario registrado ingrese con email y contraseña y reciba un JWT equivalente al del registro.

**Independent Test**: Registrar un usuario, iniciar sesión con credenciales válidas y usar el token devuelto contra `GET /api/jugadores`.

### Tests for User Story 1

- [X] T008 [US1] Crear y usar `backend/src/test/java/com/formula11/unit/service/UsuarioLoginServiceTest.java` para probar login exitoso y emisión de token, verificando `id`, `nombre`, email normalizado y llamada a `JwtService.generarToken`; no modificar `UsuarioServiceTest.java`.
- [X] T009 [US1] Añadir en `backend/src/test/java/com/formula11/unit/service/UsuarioLoginServiceTest.java` un caso de login con email en mayúsculas y espacios externos, verificando búsqueda por `Usuario.normalizarEmail`; no modificar `UsuarioServiceTest.java`.
- [X] T010 [P] [US1] Crear `backend/src/test/java/com/formula11/integration/UsuarioLoginIntegrationTest.java` sin modificar `UsuarioRepositoryIntegrationTest`, cubriendo búsqueda de usuario normalizado y validación BCrypt contra PostgreSQL Testcontainers.
- [X] T011 [US1] Crear el flujo feliz E2E con MockMvc sobre `@SpringBootTest` en `backend/src/test/java/com/formula11/e2e/LoginUsuarioE2ETest.java`, verificando `POST /api/usuarios/login` público, respuesta `200` con `id`, `nombre`, `email`, `token` y uso del token en un endpoint protegido.

### Implementation for User Story 1

- [X] T012 [US1] Implementar de forma aditiva `login(LoginRequest)` en `backend/src/main/java/com/formula11/service/UsuarioService.java`: normalizar email con `Usuario.normalizarEmail`, buscar con `UsuarioRepository.findByEmail`, verificar con el `PasswordEncoder` BCrypt existente y emitir con `JwtService.generarToken(usuario.getId(), usuario.getEmail())`, sin cambiar el constructor ni las firmas públicas existentes.
- [X] T013 [US1] Mapear el usuario autenticado a `LoginResponse` en `backend/src/main/java/com/formula11/dto/LoginResponse.java`, devolviendo email normalizado y ningún dato sensible.
- [X] T014 [US1] Añadir `POST /api/usuarios/login` en `backend/src/main/java/com/formula11/controller/UsuarioController.java`, usando `@Valid @RequestBody LoginRequest`, estado `200`, anotaciones Swagger y delegación exclusiva a `UsuarioService`.
- [X] T015 [US1] Permitir únicamente `POST /api/usuarios/login` en `backend/src/main/java/com/formula11/security/SecurityConfig.java`, manteniendo `.anyRequest().authenticated()` y sin abrir otras rutas bajo `/api/usuarios/**`.

**Checkpoint**: El caso feliz completo debe funcionar desde registro hasta acceso autenticado al catálogo.

---

## Phase 4: User Story 2 - Rechazo seguro de credenciales inválidas (Priority: P1)

**Goal**: Rechazar email inexistente y contraseña incorrecta de manera indistinguible y sin filtrar información.

**Independent Test**: Comparar dos llamadas de login, una con email inexistente y otra con contraseña incorrecta, verificando el mismo `401`, mensaje y ausencia de token.

### Tests for User Story 2

- [X] T016 [US2] Añadir en `backend/src/test/java/com/formula11/unit/service/UsuarioLoginServiceTest.java` tests unitarios para email inexistente y contraseña incorrecta, comprobando la misma `CredencialesInvalidasException` y que no se emite JWT; no modificar `UsuarioServiceTest.java`.
- [X] T017 [US2] Añadir casos E2E para email inexistente y contraseña incorrecta en `backend/src/test/java/com/formula11/e2e/LoginUsuarioE2ETest.java`, comparando estado `401`, `mensaje` genérico y ausencia de `token`.

### Implementation for User Story 2

- [X] T018 [US2] Completar el flujo de `UsuarioService.login` en `backend/src/main/java/com/formula11/service/UsuarioService.java` para convertir tanto `Optional.empty()` como `passwordEncoder.matches(...) == false` en la misma excepción y mensaje, sin distinguir la causa en la respuesta.
- [X] T019 [US2] Verificar en `backend/src/main/java/com/formula11/exception/GlobalExceptionHandler.java` que el handler específico de credenciales se evalúa antes del handler genérico y conserva `correlationId`.

**Checkpoint**: Ambos fallos deben producir el mismo contrato HTTP observable y ningún token.

---

## Phase 5: User Story 3 - Validación del formulario de login (Priority: P1)

**Goal**: Responder `400` para datos ausentes o con formato inválido y mantener el endpoint público.

**Independent Test**: Enviar cuerpo vacío, email ausente, contraseña ausente, email inválido y contraseña fuera de rango; todos deben responder `400` con `ErrorResponse`.

### Tests for User Story 3

- [X] T020 [US3] Añadir tests E2E de campos vacíos, ausentes, email inválido y contraseña fuera del rango 8-72 en `backend/src/test/java/com/formula11/e2e/LoginUsuarioE2ETest.java`, verificando `400` y ausencia de token.
- [X] T021 [US3] Añadir tests con MockMvc para validar Bean Validation a nivel HTTP en `backend/src/test/java/com/formula11/e2e/LoginUsuarioE2ETest.java`, cubriendo `@NotBlank`, `@Size` y formato de email.

### Implementation for User Story 3

- [X] T022 [US3] Confirmar en `backend/src/main/java/com/formula11/dto/LoginRequest.java` que las anotaciones producen mensajes de validación en español compatibles con `GlobalExceptionHandler`.
- [X] T023 [US3] Confirmar en `backend/src/main/java/com/formula11/controller/UsuarioController.java` que login usa `@Valid` y no requiere header `Authorization`.
- [X] T024 [US3] Actualizar las anotaciones Swagger de `backend/src/main/java/com/formula11/controller/UsuarioController.java` y los DTOs para documentar `200`, `400`, `401` y `security: []` del login.

**Checkpoint**: Los errores de forma se diferencian de credenciales inválidas con `400` frente a `401`, sin romper el flujo público.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Sincronizar documentación, ejecutar validaciones y verificar los límites de alcance.

- [X] T025 [P] Revisar que `backend/specs/002-login-usuarios/contracts/openapi.yaml`, las anotaciones Swagger y la colección Postman describan los mismos campos, estados `200/400/401` y comportamiento público.
- [X] T026 Ejecutar la validación de `backend/specs/002-login-usuarios/quickstart.md` desde `backend/`, incluyendo `mvn test`, y corregir solo fallos relacionados con esta feature.
- [X] T027 Verificar en `backend/specs/002-login-usuarios/spec.md`, `backend/specs/002-login-usuarios/plan.md` y `backend/specs/002-login-usuarios/tasks.md` que los cambios no incorporen recuperación de contraseña, bloqueo por intentos fallidos ni refresh tokens, y que no se modifiquen ni eliminen tests existentes sin autorización.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: T001 debe confirmarse; T002 y T003 son paralelizables.
- **Foundational (Phase 2)**: T004 y T005 pueden ejecutarse en paralelo; T006 y T007 dependen de los contratos y del formato de error existente.
- **User Story 1 (Phase 3)**: T008-T011 pueden prepararse antes de T012-T015; la implementación depende de T004-T007.
- **User Story 2 (Phase 4)**: Depende de T012 y T014; sus pruebas pueden desarrollarse en paralelo.
- **User Story 3 (Phase 5)**: Depende de T014 y T007; sus pruebas pueden desarrollarse en paralelo.
- **Polish (Phase 6)**: Depende de las tres historias y de la actualización del contrato.

### User Story Dependencies

- **US1 (P1)**: Depende de la fase Foundational; es el MVP y habilita el flujo feliz completo.
- **US2 (P1)**: Depende de la implementación de US1 porque prueba las dos ramas de autenticación del mismo service.
- **US3 (P1)**: Depende del endpoint de US1, pero sus validaciones pueden probarse de forma independiente mediante MockMvc.

### Parallel Opportunities

- T002 y T003 pueden ejecutarse en paralelo.
- T004 y T005 pueden ejecutarse en paralelo.
- T010 puede prepararse en paralelo con las tareas de tests que no compartan su archivo.
- T003 y T025 pueden ejecutarse en paralelo después de estabilizar el endpoint; T003 actualiza Postman y T025 solo revisa la coherencia documental.

## Parallel Example: User Story 1

```text
Task: T008 unit tests in backend/src/test/java/com/formula11/unit/service/UsuarioLoginServiceTest.java
Task: T010 PostgreSQL integration coverage in backend/src/test/java/com/formula11/integration/UsuarioLoginIntegrationTest.java
Task: T011 E2E coverage in backend/src/test/java/com/formula11/e2e/LoginUsuarioE2ETest.java
Task: T002 OpenAPI contract in backend/specs/002-login-usuarios/contracts/openapi.yaml
```

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Completar Setup y Foundational.
2. Implementar T012-T015.
3. Ejecutar T008-T011 y validar registro, login y acceso protegido.
4. Detenerse en el checkpoint de US1 para una demostración funcional.

### Incremental Delivery

1. Añadir US2 y comprobar que email inexistente y contraseña incorrecta son indistinguibles.
2. Añadir US3 y comprobar la separación `400`/`401`.
3. Completar documentación, Postman y quickstart.
4. Ejecutar la suite completa antes de `/speckit-implement` o de cerrar la feature.

## Notes

- Cada tarea usa formato de checklist con ID secuencial, marcador `[P]` solo cuando corresponde, etiqueta de historia en fases de usuario y ruta concreta.
- La implementación debe reutilizar `JwtService`, `PasswordEncoder`, `UsuarioRepository` y `GlobalExceptionHandler`; no crear mecanismos paralelos.
