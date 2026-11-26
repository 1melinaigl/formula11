---
description: "Tareas para implementar Actuator y Prometheus"
---

# Tasks: Actuator y Prometheus

**Input**: Documentos de diseño en `specs/016-actuator-prometheus/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/actuator.md](contracts/actuator.md), [contracts/metricas-negocio.md](contracts/metricas-negocio.md), [quickstart.md](quickstart.md)

**Tests**: Se incluyen. Los piden la spec (SC-001 a SC-004) y la constitución (punto 6). En cada historia, los tests se escriben primero y tienen que fallar antes de implementar.

**Organization**: Una fase por historia de usuario de la spec, en orden de prioridad.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: se puede hacer en paralelo (otro archivo, sin depender de tareas pendientes).
- **[Story]**: historia de la spec (US1 a US4).

## Path Conventions

- Todo se ejecuta desde `formula11/backend`.
- Código nuevo: `src/main/java/com/formula11/observabilidad/`.
- Tests: `src/test/java/com/formula11/{unit,integration,e2e}/observabilidad/`.

## Restricciones

- No modificar ni borrar tests existentes (constitución, punto 6). Los tests nuevos de
  services existentes van en clases nuevas.
- No cambiar los constructores de `MercadoService`, `RecalculoService` ni `CargaMetricasService` (research R5).
- En `pom.xml` solo se agrega `micrometer-registry-prometheus`.
- En `SecurityConfig`, lo de Actuator va en un bloque aparte con comentario.
- Sin migraciones ni cambios en endpoints de la API.
- Commits con prefijo `016` y sin coautoría.

---

## Phase 1: Setup

**Purpose**: baseline y dependencia.

- [X] T001 Registrar el baseline: correr `mvn verify` en `formula11/backend` y anotar el resultado debajo de esta tarea (se espera 358 tests, 0 fallas, después del merge de la 014 y el fix de Sonar).
  - Baseline 2026-11-26 sobre `entrega3-observabilidad` (4fa9bf2, con la 014 mergeada): `mvn verify` con 358 tests, 0 fallas, 0 errores.
- [X] T002 Agregar `<dependency><groupId>io.micrometer</groupId><artifactId>micrometer-registry-prometheus</artifactId><scope>runtime</scope></dependency>` en `pom.xml`, sin versión (la maneja el parent 3.5.5), junto a `spring-boot-starter-actuator`.

---

## Phase 2: Foundational

No hay tareas bloqueantes compartidas aparte de T002: cada historia agrega lo suyo.
`micrometer-core` (con `MeterRegistry`) ya llega con el actuator.

**Checkpoint**: `mvn -q compile` pasa con la dependencia nueva.

---

## Phase 3: User Story 1 - Saber si la aplicación está sana (Priority: P1) 🎯 MVP

**Goal**: `/actuator/health` es público y solo muestra el estado. El componente `cargaMetricas` da `DOWN` si la última carga terminada fue `FALLIDA`. El detalle lo ve solo el superusuario.

**Independent Test**: con una carga `FALLIDA` guardada, `GET /actuator/health` sin sesión responde 503 con `{"status":"DOWN"}`. Con una `COMPLETADA` posterior responde 200 `UP`. El superusuario ve `components.cargaMetricas.details`.

### Tests for User Story 1 ⚠️

- [X] T003 [P] [US1] `CargaMetricasHealthIndicatorTest` en `src/test/java/com/formula11/unit/observabilidad/CargaMetricasHealthIndicatorTest.java`, con `CargaMetricasService` mockeado. Cubre los casos de [data-model.md](data-model.md):
  - sin cargas → `UP` con `detalle=Sin cargas de métricas`;
  - `COMPLETADA` → `UP` con `estado`, `fin` y `ejecucionId`;
  - `PARCIAL` → `UP` con `estado=PARCIAL`;
  - `FALLIDA` → `DOWN` con `error`.
- [X] T004 [P] [US1] `CargaMetricasUltimaTerminadaTest` en `src/test/java/com/formula11/unit/service/metricas/CargaMetricasUltimaTerminadaTest.java` (clase nueva, no tocar `CargaMetricasServiceTest`). Construye `CargaMetricasService` con mocks, como el test existente, y verifica que `ultimaTerminada()` pide a `EjecucionMetricasRepository.findFirstByEstadoInAndFinNotNullOrderByFinDescIdDesc` los estados `COMPLETADA`, `PARCIAL` y `FALLIDA` (sin `EN_CURSO`) y devuelve lo que trae el repositorio.
- [X] T005 [US1] `CargaMetricasHealthIntegrationTest` en `src/test/java/com/formula11/integration/observabilidad/CargaMetricasHealthIntegrationTest.java` (`@SpringBootTest`, perfil `test`, Testcontainers como los demás de integración):
  - guarda una `EjecucionMetricas` `FALLIDA`, después una `EN_CURSO`, y el bean `cargaMetricasHealthIndicator` da `DOWN` (la `EN_CURSO` se ignora);
  - después guarda una `COMPLETADA` y da `UP`;
  - borra las ejecuciones que creó en `@AfterEach`.
- [X] T006 [US1] `ActuatorHealthE2ETest` en `src/test/java/com/formula11/e2e/observabilidad/ActuatorHealthE2ETest.java` (`@SpringBootTest` + `@AutoConfigureMockMvc`, perfil `e2e`):
  - `GET /actuator/health` sin sesión → 200, cuerpo con `status=UP` y sin `components`;
  - con usuario común, tampoco hay `components`;
  - con superusuario hay `components.cargaMetricas.status`;
  - con una carga `FALLIDA` guardada → 503 y `status=DOWN` sin sesión; se borra al final.

### Implementation for User Story 1

- [X] T007 [P] [US1] En `src/main/java/com/formula11/persistence/metricas/EjecucionMetricasRepository.java`, agregar `Optional<EjecucionMetricas> findFirstByEstadoInAndFinNotNullOrderByFinDescIdDesc(Collection<String> estados);`.
- [X] T008 [US1] En `src/main/java/com/formula11/service/metricas/CargaMetricasService.java`, agregar `public Optional<EjecucionMetricas> ultimaTerminada()`, que usa la consulta de T007 con las constantes `COMPLETADA`, `PARCIAL` y `FALLIDA` que ya existen. No cambia el constructor. Depende de T007.
- [X] T009 [US1] `CargaMetricasHealthIndicator` en `src/main/java/com/formula11/observabilidad/CargaMetricasHealthIndicator.java` (`@Component`, `implements HealthIndicator`). Llama a `CargaMetricasService.ultimaTerminada()` y arma `Health.up()` o `Health.down()` con los detalles de [contracts/actuator.md](contracts/actuator.md): `estado`, `fin`, `ejecucionId`, `error` solo si falló, o `detalle` si no hay cargas. Depende de T008.
- [X] T010 [US1] En `src/main/resources/application.yml`, agregar `roles: SUPERUSUARIO` debajo de `management.endpoint.health.show-details: when_authorized`.
- [X] T011 [US1] Correr T003 a T006 y dejarlos en verde.

**Checkpoint**: la US1 funciona sola. `health` ya era público, así que no hace falta tocar `SecurityConfig` todavía.

---

## Phase 4: User Story 2 - Información operativa solo para el superusuario (Priority: P1)

**Goal**: `info`, `metrics`, `prometheus` y `loggers` expuestos y solo para el superusuario. Sin sesión, 401 con el JSON de la API; usuario común, 403 con el JSON de la API.

**Independent Test**: recorrer los endpoints de [contracts/actuator.md](contracts/actuator.md) con las tres sesiones y comparar los códigos con la tabla.

### Tests for User Story 2 ⚠️

- [X] T012 [US2] `ActuatorSeguridadE2ETest` en `src/test/java/com/formula11/e2e/observabilidad/ActuatorSeguridadE2ETest.java` (`@SpringBootTest` + `@AutoConfigureMockMvc` + `@AutoConfigureObservability(tracing = false)`, perfil `e2e`, para que exista `/actuator/prometheus`). Casos:
  - Para `/actuator/info`, `/actuator/metrics`, `/actuator/prometheus` y `/actuator/loggers` (test parametrizado): sin sesión → 401 con `mensaje` y `correlationId`; usuario común → 403 con `mensaje`; superusuario → 200.
  - `/actuator/info` con superusuario tiene `app.nombre=Formula11` y un `app.version` que no está vacío ni es el texto `@project.version@`.
  - `/actuator/env` con superusuario no responde 200, porque no está expuesto (hoy da el 500 genérico de ruta inexistente; ver el contrato).
  - `POST /actuator/loggers/AUDITORIA` con `{"configuredLevel":"WARN"}` como superusuario → 204. Una llamada a `GET /api/jugadores` no agrega líneas con su correlation ID en `target/auditoria-e2e/auditoria.log`. Al final se restaura el nivel que tenía (con `null` heredaría el `WARN` del root del perfil `e2e`).
  - El mismo `POST` con usuario común → 403.

### Implementation for User Story 2

- [X] T013 [US2] En `src/main/resources/application.yml`, cambiar `management.endpoints.web.exposure.include` a `health,info,metrics,prometheus,loggers`. Agregar `management.info.env.enabled: true` y el bloque `info.app.nombre: Formula11`, `info.app.version: '@project.version@'` (research R3), con un comentario de una línea.
- [X] T014 [US2] En `src/main/java/com/formula11/security/SecurityConfig.java`:
  - Quitar `"/actuator/health"` de la lista de `permitAll`.
  - Agregar antes de `anyRequest()` un bloque comentado (`// Actuator (B, entrega 3): health público, el resto solo superusuario.`) con `EndpointRequest.to(HealthEndpoint.class).permitAll()` y `EndpointRequest.toAnyEndpoint().hasRole("SUPERUSUARIO")`.
  - En `exceptionHandling`, agregar un `accessDeniedHandler` que responda 403 con `ErrorResponse("No tiene permisos para realizar esta operación", Instant.now(), correlationId(request))`, el mismo texto que `GlobalExceptionHandler`.
- [X] T015 [US2] Correr T012, y otra vez T006, y dejarlos en verde.

**Checkpoint**: US1 y US2 funcionan juntas; Actuator queda como pide la consigna, salvo las métricas de negocio.

---

## Phase 5: User Story 3 - Métricas de negocio del mercado y del recálculo (Priority: P2)

**Goal**: el contador `formula11.mercado.operaciones{tipo}` y el timer `formula11.cotizaciones.recalculo{resultado}`, alimentados por un aspecto sin tocar los services.

**Independent Test**: una compra y una venta reales suben en uno los contadores `compra` y `venta`; una compra rechazada no suma nada; un recálculo suma una muestra al timer.

### Tests for User Story 3 ⚠️

- [X] T016 [P] [US3] `MetricasNegocioTest` en `src/test/java/com/formula11/unit/observabilidad/MetricasNegocioTest.java`, con `SimpleMeterRegistry`:
  - al crear el componente, los tres contadores existen en `0`;
  - `registrarOperacionMercado(COMPRA)` sin transacción suma uno;
  - con `TransactionSynchronizationManager.initSynchronization()` no suma hasta llamar a `afterCommit()` en las sincronizaciones registradas, y con `afterCompletion(STATUS_ROLLED_BACK)` no suma;
  - `iniciarMedicion()` + `registrarRecalculo(muestra, ok)` registran `resultado=ok` o `resultado=error`, y una muestra nula no registra nada;
  - `null` no lanza.
  Limpia la sincronización en `@AfterEach`.
- [X] T017 [P] [US3] `MetricasNegocioAspectTest` en `src/test/java/com/formula11/unit/observabilidad/MetricasNegocioAspectTest.java`:
  - `@AfterReturning` con un `ResultadoOperacion` de `Transaccion.Tipo.COMPRA` y otro de `VENTA` registra el tipo correcto;
  - el `@Around` del recálculo devuelve lo de `proceed()` y registra `ok`; si `proceed()` lanza, relanza la misma instancia y registra `error`;
  - si `MetricasNegocio` lanza, la operación devuelve su resultado igual.
- [X] T018 [US3] `MetricasNegocioIntegrationTest` en `src/test/java/com/formula11/integration/observabilidad/MetricasNegocioIntegrationTest.java` (perfil `test`, Testcontainers). Mide con el `MeterRegistry` del contexto y compara el antes y el después, porque el registry se comparte entre tests:
  - una compra y una venta por `MercadoService` con datos como los de los tests de mercado existentes suben `compra` y `venta` en uno;
  - una compra sin saldo (lanza `OperacionNoValidaException`) no cambia nada;
  - `RecalculoService.recalcular(null)` suma uno a `formula11.cotizaciones.recalculo{resultado=ok}`.
- [X] T019 [US3] `ActuatorPrometheusE2ETest` en `src/test/java/com/formula11/e2e/observabilidad/ActuatorPrometheusE2ETest.java` (perfil `e2e`, `@AutoConfigureObservability`): como superusuario, `GET /actuator/prometheus` trae `formula11_mercado_operaciones_total{` con `tipo="compra"`, `tipo="venta"` y `tipo="p2p"`. Después de una `POST /api/ordenes/compra` por HTTP con un usuario de prueba, el valor de `tipo="compra"` subió en uno.

### Implementation for User Story 3

- [X] T020 [P] [US3] Enum `TipoOperacionMercado { COMPRA, VENTA, P2P }` en `src/main/java/com/formula11/observabilidad/TipoOperacionMercado.java`, con `etiqueta()` en minúsculas y javadoc que remite a [contracts/metricas-negocio.md](contracts/metricas-negocio.md).
- [X] T021 [US3] `MetricasNegocio` en `src/main/java/com/formula11/observabilidad/MetricasNegocio.java` (`@Component`, recibe `MeterRegistry`):
  - registra en el constructor los tres `Counter` `formula11.mercado.operaciones` con `tipo` y la descripción del contrato;
  - `registrarOperacionMercado(TipoOperacionMercado)` suma después del commit si `TransactionSynchronizationManager.isSynchronizationActive()`, o en el momento si no;
  - `Timer.Sample iniciarMedicion()` y `registrarRecalculo(Timer.Sample, boolean ok)` registran `formula11.cotizaciones.recalculo` con `resultado` (el `@Around` necesita `proceed()`, que lanza `Throwable`, por eso no es un `Callable`);
  - nunca lanza por un problema propio: hace `warn` en el log de la clase.
  Depende de T020.
- [X] T022 [US3] `MetricasNegocioAspect` en `src/main/java/com/formula11/observabilidad/MetricasNegocioAspect.java` (`@Aspect @Component @Order(Ordered.HIGHEST_PRECEDENCE + 10)`, después del `ExposeInvocationInterceptor`):
  - `@AfterReturning(pointcut = "execution(* com.formula11.service.mercado.MercadoService.comprar(..)) || execution(* com.formula11.service.mercado.MercadoService.vender(..))", returning = "resultado")` traduce `Transaccion.Tipo` a `TipoOperacionMercado` por nombre;
  - `@Around("execution(* com.formula11.service.cotizaciones.RecalculoService.recalcular(..))")` mide con `MetricasNegocio`;
  - todo en `try/catch` (R8).
  Depende de T021.
- [X] T023 [US3] Correr T016 a T019 y dejarlos en verde.

**Checkpoint**: las métricas de negocio se ven en `/actuator/prometheus`. A ya puede usar `MetricasNegocio` para el P2P.

---

## Phase 6: User Story 4 - Ver las métricas en un Prometheus local (Priority: P3)

**Goal**: un Prometheus local con un comando, que lea `/actuator/prometheus` con el token del superusuario.

**Independent Test**: el paso 7 de [quickstart.md](quickstart.md). El objetivo `formula11` aparece `UP` en `http://localhost:9090/targets`.

- [X] T024 [P] [US4] Crear `docker/prometheus/docker-compose.yml`:
  - servicio `prometheus` con `prom/prometheus` en una versión fija y publicada hace más de dos semanas;
  - puerto `9090:9090`;
  - montajes de solo lectura de `prometheus.yml` y `token`;
  - `extra_hosts: ["host.docker.internal:host-gateway"]`.
- [X] T025 [P] [US4] Crear `docker/prometheus/prometheus.yml` con `scrape_interval: 15s` y un job `formula11`:
  - `metrics_path: /actuator/prometheus` y `targets: ["host.docker.internal:8080"]`;
  - `authorization: { type: Bearer, credentials_file: /etc/prometheus/token }`.

  Crear también `docker/prometheus/token.example`, con una línea que explica qué va en `token`.
- [X] T026 [US4] En `.gitignore` (raíz del repo), agregar `formula11/backend/docker/prometheus/token`.
- [X] T027 [US4] Seguir el paso 7 del quickstart con el backend en perfil `local` y anotar el resultado debajo de esta tarea.
  - 2026-11-26, backend `local` en 8080 contra un Postgres descartable y `prom/prometheus:v2.53.3`:
    - el target `formula11` quedó `up`, sin `lastError`;
    - la consulta `formula11_mercado_operaciones_total` devuelve `compra=21`, `venta=0` y `p2p=0`. Las 20 compras de más son las del `DemoPortfolioInitializer`, que compra por `MercadoService` al arrancar el perfil `local`.
    - Después se bajó todo y se borró el token.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [ ] T028 Correr `mvn verify` completo y compararlo con el baseline de T001: ningún test existente cambia de resultado (SC-005).
- [ ] T029 [P] En `postman/formula11-registro-catalogo.json`, agregar una carpeta "Actuator" con health (sin auth), info, prometheus y `POST loggers/AUDITORIA`, usando la variable de token de superusuario que ya usa la colección.
- [ ] T030 [P] En `README.md` (raíz del repo), sumar a la sección "Auditoría" (o renombrarla "Observabilidad"):
  - los endpoints de Actuator y quién los ve;
  - el componente `cargaMetricas`;
  - las dos métricas de negocio;
  - cómo levantar el Prometheus local.
- [ ] T031 Seguir los pasos 2 a 6 de [quickstart.md](quickstart.md) con el perfil `local`, contra un Postgres descartable.
  - Adelantado junto con T027:
    - health público `{"status":"UP"}`;
    - `info` da 401 sin sesión, 403 al demo y nombre y versión al superusuario;
    - `cargaMetricas` en `UP` sin cargas;
    - `/actuator/env` da 500 al superusuario (confirma el contrato);
    - la compra suma uno;
    - el recálculo deja `formula11_cotizaciones_recalculo_seconds_count{resultado="ok"} 1`;
    - con `AUDITORIA` en `WARN` no hay línea y con `INFO` sí.

    Falta el paso 4 con una carga fallida real.
- [ ] T032 Actualizar la descripción del PR de `entrega3-observabilidad` con la línea de "Release notes" del [plan.md](plan.md). Pasarle a A el contrato de [contracts/metricas-negocio.md](contracts/metricas-negocio.md) para el P2P.

---

## Dependencies & Execution Order

```text
T001 → T002 → US1 (T003–T011) → US2 (T012–T015) → US3 (T016–T023) → US4 (T024–T027) → Polish (T028–T032)
```

- **US1** depende solo de Setup. Es el MVP.
- **US2** necesita `micrometer-registry-prometheus` (T002) para el endpoint `prometheus`. Va después de la US1 porque los dos cambian `application.yml`, y su bloque de `SecurityConfig` reemplaza la ruta pública de health.
- **US3** es independiente de US1 y US2 en código. Su E2E (T019) necesita el endpoint `prometheus` protegido (US2).
- **US4** necesita US2 y US3 para tener algo que leer.
- Dentro de cada historia: tests → implementación → correr tests.

## Parallel Example: User Story 1

```text
En paralelo: T003 CargaMetricasHealthIndicatorTest, T004 CargaMetricasUltimaTerminadaTest, T007 consulta del repositorio
Después:     T008 ultimaTerminada() → T009 CargaMetricasHealthIndicator → T010 application.yml
```

## Parallel Example: User Story 3

```text
En paralelo: T016 MetricasNegocioTest, T017 MetricasNegocioAspectTest, T020 TipoOperacionMercado
Después:     T021 MetricasNegocio → T022 MetricasNegocioAspect
```

## Implementation Strategy

1. **MVP**: Setup + US1. El health avisa si falló la carga de métricas.
2. **US2**: Actuator protegido y completo; ya cumple el ítem (b) de la consigna.
3. **US3**: métricas de negocio; cumple el ítem (c). A puede integrar el P2P.
4. **US4**: Prometheus local para la demo (opcional).
5. **Polish**: suite completa, Postman, README, quickstart y PR.

Un commit por fase (setup, US1, US2, US3, US4, polish), con prefijo `016` y sin coautoría.
