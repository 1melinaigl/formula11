---
description: "Tareas para implementar la auditoría de web services"
---

# Tasks: Auditoría de web services

**Input**: Documentos de diseño en `specs/015-auditoria-web-services/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/linea-auditoria.md](contracts/linea-auditoria.md), [quickstart.md](quickstart.md)

**Tests**: Se incluyen. Los pide la consigna (unit del aspecto e integración que llame a un endpoint), la spec (SC-002) y la constitución (punto 6). En cada historia los tests se escriben primero y tienen que fallar antes de implementar.

**Organization**: Una fase por historia de usuario de la spec, en orden de prioridad.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: se puede hacer en paralelo (otro archivo, sin depender de tareas pendientes).
- **[Story]**: historia de la spec (US1, US2, US3).

## Path Conventions

- Todo se ejecuta desde `formula11/backend`.
- Código: `src/main/java/com/formula11/observabilidad/`. Tests: `src/test/java/com/formula11/{unit,integration,e2e}/observabilidad/`.

## Restricciones

- No modificar ni borrar tests existentes (constitución, punto 6).
- En `pom.xml` solo se agrega `spring-boot-starter-aop`; nada más.
- No se cambia ningún controller, DTO ni endpoint.
- Sin migraciones.

---

## Phase 1: Setup

**Purpose**: baseline y dependencia.

- [X] T001 Registrar el baseline: correr `mvn verify` en `formula11/backend` y anotar en este archivo, debajo de esta tarea, los tests que ya fallaban antes de empezar (sin tocarlos).
  - Baseline 2026-11-26 sobre `dev` (5d72f9b): `mvn verify` con 316 tests, 0 fallas, 0 errores. No hay fallos previos.
- [X] T002 Agregar `<dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-aop</artifactId></dependency>` en `pom.xml`, sin versión (la maneja el parent 3.5.5), junto a las demás dependencias `spring-boot-starter-*`.

---

## Phase 2: Foundational

No hay tareas bloqueantes compartidas aparte de T002: cada historia agrega lo suyo.

**Checkpoint**: `mvn -q compile` pasa con la dependencia nueva.

---

## Phase 3: User Story 1 - Registrar cada llamada a la API (Priority: P1) 🎯 MVP

**Goal**: cada llamada a un endpoint de `com.formula11.controller` escribe una línea en el logger `AUDITORIA` con usuario, operación, parámetros, duración y resultado.

**Independent Test**: con un `ListAppender` en el logger `AUDITORIA`, llamar a un controller del contexto de Spring y ver una línea por llamada, con `usuario`, `operacion`, `parametros`, `duracionMs` y `resultado`.

### Tests for User Story 1 ⚠️

- [X] T003 [P] [US1] `LineaAuditoriaTest` en `src/test/java/com/formula11/unit/observabilidad/LineaAuditoriaTest.java`: `formatear()` devuelve `usuario=<u> operacion=<Clase.metodo> parametros=<mapa> duracionMs=<n> resultado=<OK|ERROR(Excepcion)>` en una línea, en ese orden; `parametros={}` si no hay; rechaza `usuario` vacío y `duracionMs` negativa.
- [X] T004 [P] [US1] `EnmascaradorParametrosTest` en `src/test/java/com/formula11/unit/observabilidad/EnmascaradorParametrosTest.java`, casos generales: omite `Authentication`, `Principal`, `ServletRequest`, `ServletResponse`, `HttpSession`, `BindingResult` y `Errors`; `null` se muestra como `null`; un record se muestra como JSON (`request={"jugadorId":12,"cantidad":5}`); un valor que no se puede serializar usa `String.valueOf`; un texto de más de 500 caracteres se recorta a 500 y termina en `…(recortado)`.
- [X] T005 [P] [US1] `AuditoriaWebServicesAspectTest` en `src/test/java/com/formula11/unit/observabilidad/AuditoriaWebServicesAspectTest.java`, con `ProceedingJoinPoint` y `MethodSignature` de Mockito y un `ListAppender` en el logger `AUDITORIA`:
  - con `SecurityContext` autenticado, `usuario=` es `authentication.getName()`;
  - sin autenticación o con `AnonymousAuthenticationToken`, `usuario=anónimo`;
  - si `proceed()` lanza, la línea dice `resultado=ERROR(<NombreSimple>)` y se relanza la misma instancia;
  - si preparar la línea falla (el enmascarador lanza), se devuelve el resultado de `proceed()` igual y se deja un `warn`;
  - exactamente una línea por llamada; `operacion=` es `NombreSimpleDeClase.metodo`.
- [X] T006 [US1] `AuditoriaWebServicesIntegrationTest` en `src/test/java/com/formula11/integration/observabilidad/AuditoriaWebServicesIntegrationTest.java` (`@SpringBootTest`, perfil `test`, Testcontainers como `UsuarioLoginIntegrationTest`): con un `ListAppender` en `AUDITORIA`, llamar al bean de un controller (proxy con el aspecto) en un caso OK y en uno que lance `RecursoNoEncontradoException`; verificar una línea por llamada con `resultado=OK` y `resultado=ERROR(RecursoNoEncontradoException)`.

### Implementation for User Story 1

- [X] T007 [P] [US1] Record `LineaAuditoria` en `src/main/java/com/formula11/observabilidad/LineaAuditoria.java` con `usuario` ("Email del usuario autenticado o `anónimo`. Nunca vacío."), `operacion` (`NombreSimpleDeClase.metodo`), `parametros` (ya enmascarado, `{}` si no hay), `duracionMs` ("Mayor o igual a 0.") y `resultado` (`OK` o `ERROR(NombreSimpleDeLaExcepcion)`), más `formatear()`.
- [X] T008 [P] [US1] `EnmascaradorParametros` en `src/main/java/com/formula11/observabilidad/EnmascaradorParametros.java`: recibe nombres y valores, omite los argumentos de infraestructura, convierte cada valor con el `ObjectMapper` (fallback `String.valueOf`), arma `{nombre=valor, ...}` y recorta a 500 caracteres con `…(recortado)`. Sin estado; recibe el `ObjectMapper` por constructor.
- [X] T009 [US1] `AuditoriaWebServicesAspect` en `src/main/java/com/formula11/observabilidad/AuditoriaWebServicesAspect.java` (`@Aspect @Component`): `@Around("within(com.formula11.controller..*) && @within(org.springframework.web.bind.annotation.RestController)")`; mide con `System.nanoTime()`; usuario desde `SecurityContextHolder` (R3); nombres de parámetros desde `MethodSignature.getParameterNames()`; escribe `linea.formatear()` con nivel INFO en `LoggerFactory.getLogger("AUDITORIA")`; la preparación de la línea va en `try/catch` con `warn` en el log de la clase; la excepción del controller se relanza sin envolver. Depende de T007 y T008.
- [X] T010 [US1] Correr T003 a T006 y dejarlos en verde.

**Checkpoint**: la US1 funciona sola; las líneas salen por consola porque todavía no hay `logback-spring.xml`.

---

## Phase 4: User Story 2 - No exponer datos sensibles (Priority: P1)

**Goal**: ninguna contraseña ni token llega a la línea de auditoría.

**Independent Test**: auditar un registro y un login con una contraseña conocida y verificar que la línea tiene el email pero no la contraseña.

### Tests for User Story 2 ⚠️

- [X] T011 [P] [US2] Sumar a `src/test/java/com/formula11/unit/observabilidad/EnmascaradorParametrosTest.java` (clase nueva de esta feature) los casos sensibles: `LoginRequest` y `RegistroUsuarioRequest` muestran `"password":"****"` y conservan `email` y `nombre`; los campos `password`, `contrasena`, `token`, `accessToken`, `refreshToken` y `secret` se enmascaran sin distinguir mayúsculas y a cualquier profundidad (objeto anidado y lista); un argumento llamado `token` se enmascara entero; un campo `tokensDisponibles` no se enmascara.
- [X] T012 [US2] Sumar a `src/test/java/com/formula11/integration/observabilidad/AuditoriaWebServicesIntegrationTest.java` un caso que llama a `UsuarioController.registrar` con una contraseña única y verifica que la línea tiene el email y `****`, y que la contraseña no aparece en ninguna línea capturada (SC-002).

### Implementation for User Story 2

- [X] T013 [US2] En `src/main/java/com/formula11/observabilidad/EnmascaradorParametros.java`, recorrer el árbol JSON y reemplazar por `****` los campos llamados `password`, `contrasena`, `token`, `accessToken`, `refreshToken` o `secret` (sin distinguir mayúsculas, a cualquier profundidad, nombres exactos), y enmascarar entero el argumento cuyo nombre sea uno de esos.
- [X] T014 [US2] Correr T011 y T012, y otra vez T003 a T006, y dejarlos en verde.

**Checkpoint**: US1 y US2 funcionan juntas; el contenido de la línea es definitivo.

---

## Phase 5: User Story 3 - Registro aparte y con rotación (Priority: P2)

**Goal**: las líneas van a `logs/auditoria.log`, con rotación diaria y 30 días de historia, con correlation ID y sin repetirse en la consola.

**Independent Test**: hacer un login por HTTP con un `X-Correlation-Id` fijo y encontrar una sola línea con ese correlation ID en el archivo de auditoría.

### Tests for User Story 3 ⚠️

- [X] T015 [US3] `AuditoriaWebServicesE2ETest` en `src/test/java/com/formula11/e2e/observabilidad/AuditoriaWebServicesE2ETest.java` (`@SpringBootTest` + `@AutoConfigureMockMvc`, perfil `e2e`, como los demás E2E): `POST /api/usuarios/login` con header `X-Correlation-Id` único y una contraseña conocida; leer `target/auditoria-e2e/auditoria.log`, filtrar por `[corr:<id>]` y verificar exactamente una línea con `operacion=UsuarioController.login`, `usuario=anónimo` y `"password":"****"`, y que la contraseña no aparece en el archivo.

### Implementation for User Story 3

- [X] T016 [P] [US3] En `src/main/resources/application.yml`, agregar `app.auditoria.directorio: ${APP_AUDITORIA_DIRECTORIO:logs}` dentro del bloque `app:` existente.
- [X] T017 [P] [US3] En `src/test/resources/application-test.yml` y `src/test/resources/application-e2e.yml`, agregar `app.auditoria.directorio` con `target/auditoria-test` y `target/auditoria-e2e`.
- [X] T018 [US3] Crear `src/main/resources/logback-spring.xml`: incluir `org/springframework/boot/logging/logback/defaults.xml` y `console-appender.xml` con root INFO en `CONSOLE`; `<springProperty name="AUDITORIA_DIR" source="app.auditoria.directorio" defaultValue="logs"/>`; `RollingFileAppender` `AUDITORIA_FILE` en `${AUDITORIA_DIR}/auditoria.log` con `TimeBasedRollingPolicy` (`${AUDITORIA_DIR}/auditoria.%d{yyyy-MM-dd}.log`, `maxHistory` 30) y patrón `%d{yyyy-MM-dd'T'HH:mm:ss.SSSXXX} [corr:%X{correlationId}] %msg%n`; logger `AUDITORIA` en INFO con `additivity="false"`. El encoder del archivo con `<charset>UTF-8</charset>`, para que `anónimo` no salga mal en Windows. Depende de T016.
- [X] T019 [US3] Verificar que la consola sigue mostrando `[corr:...]` como antes (respeta `logging.pattern.level`) y que los niveles de `application-e2e.yml` (`root: WARN`, `com.formula11: INFO`) siguen aplicando. Correr T015 y dejarlo en verde.

**Checkpoint**: las tres historias funcionan; se puede seguir el [quickstart.md](quickstart.md).

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T020 Correr `mvn verify` completo y compararlo con el baseline de T001: ningún test existente cambia de resultado (SC-004).
- [ ] T021 [P] En `README.md` (raíz del repo), agregar `observabilidad/` al árbol de la sección "Arquitectura" como paquete transversal ("auditoría de web services y métricas") y una línea que explique que cada llamada queda en `logs/auditoria.log`.
- [ ] T022 Seguir la prueba manual de [quickstart.md](quickstart.md) con el perfil `local` y confirmar el formato de [contracts/linea-auditoria.md](contracts/linea-auditoria.md).
- [ ] T023 Abrir el PR contra `dev` con la sección "Release notes" del [plan.md](plan.md) y avisar a C que `com.formula11.observabilidad` es un paquete transversal para `ArquitecturaTest`.

---

## Dependencies & Execution Order

```text
T001 → T002 → US1 (T003–T010) → US2 (T011–T014) → US3 (T015–T019) → Polish (T020–T023)
```

- **US1** depende solo de Setup. Es el MVP.
- **US2** usa el `EnmascaradorParametros` de la US1: va después.
- **US3** solo necesita que exista el logger `AUDITORIA` (US1); se podría hacer antes que la US2, pero su E2E verifica el enmascarado, así que conviene después.
- Dentro de cada historia: tests → implementación → correr tests.

## Parallel Example: User Story 1

```text
En paralelo: T003 LineaAuditoriaTest, T004 EnmascaradorParametrosTest, T005 AuditoriaWebServicesAspectTest
En paralelo: T007 LineaAuditoria, T008 EnmascaradorParametros
Después:     T009 AuditoriaWebServicesAspect (usa T007 y T008)
```

## Parallel Example: User Story 3

```text
En paralelo: T016 application.yml, T017 application-test.yml y application-e2e.yml
Después:     T018 logback-spring.xml
```

## Implementation Strategy

1. **MVP**: Setup + US1. Ya hay una línea por llamada (por consola).
2. **US2**: el contenido queda seguro; recién ahí se puede mostrar en una demo.
3. **US3**: archivo propio con rotación; queda como pide la consigna.
4. **Polish**: suite completa, README, quickstart y PR.

Un commit por fase (setup, US1, US2, US3, polish) para que la revisión sea fácil.
