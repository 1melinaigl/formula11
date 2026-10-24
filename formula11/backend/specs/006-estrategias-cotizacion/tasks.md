---

description: "Task list for weekly player valuation strategies and quotations"
---

# Tasks: Estrategias de valuación y cotización semanal de jugadores

**Input**: Design documents from `/specs/006-estrategias-cotizacion/`

**Prerequisites**: [spec.md](spec.md), [plan.md](plan.md), [research.md](research.md) y el contrato [openapi.yaml](../005-contratos-entrega2/contracts/openapi.yaml).

**Scope**: Backend únicamente. No implementar scraper de WhoScored, operaciones de compra/venta, creación del superusuario, detalle/historial público/ranking ni frontend.

**Constraints**: No agregar dependencias. No modificar ni borrar tests existentes. Las entidades JPA `MetricaPartido`, `Estrategia` y `Cotizacion` viven en `com.formula11.model`; la lógica pura vive en `com.formula11.model.valuacion`. V10–V14 es el rango reservado; esta feature crea V10, V11 y V12.

## Phase 1: Foundation and domain model

**Purpose**: Implementar las reglas matemáticas sin Spring ni base de datos, con tiempo y entradas deterministas.

### Tests for domain

- [ ] T001 [P] Crear `NormalizadorTest` en `backend/src/test/java/com/formula11/unit/model/valuacion/` para z-score de métricas sin acotar con desvío cero igual a `0`, z-score final acotado a `[-2,2]`, reescalado `[0,1]`, desvío final cero igual a `0,5` y grupo de un jugador.
- [ ] T002 [P] Crear `CalculadoraPrecioTest` para precio de partida `1`, aplicación de fórmula en la primera semana, variación `null`, tope `±20 %`, piso `0,10`, cuatro decimales y `HALF_UP`.
- [ ] T003 [P] Crear `RendimientoPosicionTest` para promedios semanales por partido, pesos por posición, proporción `menosDe90Minutos`, métricas positivas y negativas, estandarización de cada métrica sin acotar y normalización final del score por posición entre ligas.
- [ ] T004 [P] Crear `ConsistenciaTest` para ventana de seis partidos, disponibilidad, promedio/desvío de rating, `lambda`, titularidad, suplencias y fallback con menos de cuatro partidos.
- [ ] T005 [P] Crear pruebas de métricas faltantes para conteos como cero, rating ignorado con redistribución de pesos positivos y fila sin minutos devuelta en el resultado del cálculo para que el service la registre en el log.

### Implementation for domain

- [ ] T006 [P] Crear records de entrada/salida necesarios para el cálculo (por ejemplo, las métricas de un jugador en un partido) en `backend/src/main/java/com/formula11/model/valuacion/`, sin anotaciones de JPA ni Spring; las métricas de conteo ausentes valen cero.
- [ ] T007 [P] Crear `Normalizador` en `com.formula11.model.valuacion` con z-score de métricas sin acotar y fallback `0`, más z-score final acotado a `[-2,2]`, reescalado y fallback `0,5`.
- [ ] T008 [P] Crear `CalculadoraPrecio` en `com.formula11.model.valuacion` usando `BigDecimal`, `RoundingMode.HALF_UP`, sensibilidad, tope y piso de la versión.
- [ ] T009 Crear `EstrategiaValuacion` en `com.formula11.model.valuacion` como interfaz Strategy para scores brutos por jugador.
- [ ] T010 Implementar `RendimientoPosicion` promediando métricas por jugador/semana, estandarizando cada métrica antes de aplicar pesos y separando el grupo por posición.
- [ ] T011 Implementar `Consistencia` con los últimos seis partidos hasta el domingo, cálculo de disponibilidad y fallback al score normalizado de `RendimientoPosicion`.
- [ ] T012 Crear `CotizacionVigente` en `com.formula11.model` como record con fecha nullable para distinguir el precio inicial.

**Checkpoint**: Los tests unitarios del dominio pasan sin cargar Spring ni PostgreSQL.

## Phase 2: Persistence and migrations

**Purpose**: Persistir métricas, estrategias versionadas y cotizaciones idempotentes, con consultas batch.

### Tests for persistence

- [ ] T013 [P] Probar `ClockConfig` con zona `America/Argentina/Buenos_Aires` y fecha fija opcional para demos/tests.
- [ ] T014 [P] Crear integración de `MetricaPartidoRepository` con Testcontainers para unicidad por jugador/fecha y consulta de toda una semana sin N+1.
- [ ] T015 [P] Crear integración de `EstrategiaRepository` para seeds, versión vigente, historial descendente, estrategia activa y unicidad de activación.
- [ ] T016 [P] Crear integración de `CotizacionRepository` para unicidad jugador/estrategia/fecha, consulta por fecha descendente, reemplazo semanal y lectura vigente `<= fecha`.

### Implementation for persistence

- [ ] T017 Crear `ClockConfig` con bean `Clock` en `America/Argentina/Buenos_Aires` y propiedad opcional `app.clock.fecha-fija`.
- [ ] T018 Crear `V10__crear_metricas_partido.sql` con tabla, foreign key a `jugadores`, campos de métricas, la secuencia para `MetricaPartido` usada por `GenerationType.SEQUENCE` con `allocationSize = 50` e índice `(jugador_id, fecha_partido)`.
- [ ] T019 Crear entidad JPA `MetricaPartido` en `com.formula11.model` usando `GenerationType.SEQUENCE` con `allocationSize = 50`, crear su secuencia en V10 y agregar `MetricaPartidoRepository` con consulta batch por rango semanal y jugadores.
- [ ] T020 Crear `V11__crear_estrategias.sql` con `IDENTITY`, `jsonb`, restricciones, versionado, índice/constraint de estrategia activa y seeds de `RENDIMIENTO_POSICION` y `CONSISTENCIA` versión 1; no crear secuencias en esta migración.
- [ ] T021 Crear entidad JPA `Estrategia` en `com.formula11.model` usando `GenerationType.IDENTITY`, mapeando `pesos` y `parametros` con `@JdbcTypeCode(SqlTypes.JSON)`.
- [ ] T022 Crear `V12__crear_cotizaciones.sql` con la secuencia para `Cotizacion` usada por `GenerationType.SEQUENCE` con `allocationSize = 50`, snapshot de pesos, índice `(jugador_id, fecha DESC)` y unicidad `(jugador_id, estrategia, fecha)`.
- [ ] T023 Crear entidad JPA `Cotizacion` en `com.formula11.model` usando `GenerationType.SEQUENCE` con `allocationSize = 50`, crear su secuencia en V12 y agregar `CotizacionRepository` para historial, cotización previa y cotización vigente.
- [ ] T024 Configurar `hibernate.jdbc.batch_size` y `hibernate.order_inserts` para habilitar inserts batch, y validar que el recálculo no cargue métricas jugador por jugador.

**Checkpoint**: Flyway V10–V12 ejecuta sobre V1–V3 y las integraciones de repositorios pasan contra PostgreSQL real.

## Phase 3: Strategy administration

**Purpose**: Administrar versiones inmutables y estrategia activa según el contrato y los roles.

### Tests for User Story 1

- [ ] T025 [P] Probar `EstrategiaService` para listar las dos estrategias, marcar la activa y devolver versiones de la más nueva a la más vieja.
- [ ] T026 [P] Probar creación de versión con pesos/params válidos, incremento de versión, inmutabilidad de la anterior y rechazo de métrica/posición/parámetro inválido.
- [ ] T027 [P] Probar activación de estrategia existente y `404` para código desconocido.

### Implementation for User Story 1

- [ ] T028 Implementar `EstrategiaService` para listar, consultar versiones, crear versiones inmutables y activar una estrategia.
- [ ] T029 Crear `EstrategiaResponse` y `NuevaVersionRequest` como records con validaciones contractuales para pesos y parámetros.
- [ ] T030 Crear `EstrategiaController` con `GET /api/estrategias`, `GET/POST /api/estrategias/{codigo}/versiones` y `POST /api/estrategias/{codigo}/activar`, documentado con `@Operation`/`@ApiResponse`.
- [ ] T031 Configurar `@EnableMethodSecurity` y `@PreAuthorize("hasRole('SUPERUSUARIO')")` para crear versiones y activar; mantener el listado disponible para usuarios autenticados.
- [ ] T032 Agregar a `GlobalExceptionHandler` el mapeo explícito de `AccessDeniedException` a `403 ErrorResponse`.

**Checkpoint**: La administración respeta el contrato, la inmutabilidad y `USUARIO` no puede administrar estrategias.

## Phase 4: Weekly recalculation and current quotation

**Purpose**: Calcular, persistir y reemplazar semanas completas con el mismo service para HTTP y scheduler.

### Tests for User Stories 2 and 4

- [ ] T033 [P] Probar `RecalculoService` sin cuerpo: procesa el domingo de la última semana cerrada anterior al `Clock`, no la semana en curso.
- [ ] T034 [P] Probar recálculo de rango inclusivo, rango inválido, `desde`/`hasta` incompletos y continuidad con cotizaciones anteriores y posteriores al rango.
- [ ] T035 [P] Probar semana sin partidos, jugador sin métricas y primera semana: jugador sin juego conserva el precio anterior o `1` si no existe, jugador que juega aplica la fórmula sobre el precio anterior o `1`, y un jugador sin métricas en ninguna semana permanece en `1`; verificar variación nullable en la primera cotización.
- [ ] T036 [P] Probar reemplazo idempotente de una semana, transacción independiente por semana y `409` cuando existe otro recálculo en curso.
- [ ] T037 [P] Probar respuesta `RecalculoResponse` con estado, contadores, estrategias/versiones, inicio, fin y `correlationId`.

### Implementation for recalculation

- [ ] T038 Crear `RecalculoRequest` y `RecalculoResponse` como records alineados con los esquemas del contrato.
- [ ] T039 Implementar `RecalculoSemanaService` como componente separado con `@Transactional`, eliminación/reemplazo semanal y persistencia batch.
- [ ] T040 Implementar `RecalculoService` con lock en memoria, validación de rango, cálculo del domingo cerrado mediante `Clock` y procesamiento de ambas estrategias.
- [ ] T041 Implementar selección batch de métricas por semana/posición y adaptación de entidades a entradas de `model.valuacion`.
- [ ] T042 Aplicar la cadena de precios: buscar la última cotización de la misma estrategia con fecha anterior; usar `1` solo como momento cero; extender backfill hasta la última semana ya calculada.
- [ ] T043 Crear `CotizacionController` para `POST /api/cotizaciones/recalcular`, con DTO, autorización, documentación y respuestas `200`, `400`, `401`, `403` y `409`.

**Checkpoint**: Una ejecución manual y un backfill producen series completas, trazables e idempotentes.

## Phase 5: Scheduler, Clock, cache and market port

**Purpose**: Automatizar el recálculo, aislar la actualización de métricas y servir la cotización vigente.

### Tests for scheduler and integration

- [ ] T044 [P] Probar el scheduler con `ActualizacionMetricasPort` exitoso, fallido y sin implementación: siempre registra el evento y recalcula con métricas locales.
- [ ] T045 [P] Probar `CotizacionVigenteService` para estrategia activa, fecha `<=` solicitada, precio inicial con fecha/estrategia/versión `null` y cache hit/miss.
- [ ] T046 [P] Probar invalidación de caché después de recálculo confirmado y activación de otra estrategia.

### Implementation for scheduler and integration

- [ ] T047 Crear `ActualizacionMetricasPort` y su implementación inicial no operativa, registrando que la carga queda a cargo de la feature 011.
- [ ] T048 Crear `CotizacionCacheConfig` con caché Caffeine propia, clave/tipo aislado de `footballApiCache` e invalidación explícita; conectar la invalidación en `EstrategiaService` al activar y en `RecalculoService` al terminar.
- [ ] T049 Crear `CotizacionVigentePort` en `com.formula11.service` y `CotizacionVigenteService` para resolver la última cotización de la estrategia activa o el precio inicial; si existe `CotizacionFijaAdapter` como implementación provisoria de la feature 007, eliminarlo.
- [ ] T050 Crear `CotizacionScheduler` con `@EnableScheduling`, cron configurable `app.cotizacion.cron` por defecto lunes 06:00, MDC/correlationId y limpieza en `finally`.
- [ ] T051 Hacer que el scheduler capture y registre fallas de `ActualizacionMetricasPort` y llame el mismo método de `RecalculoService` con la semana cerrada.

**Checkpoint**: El job corre con `Clock`, continúa ante fallas de métricas y el puerto de mercado devuelve datos cacheables y coherentes.

## Phase 6: Contract, security and quality gates

**Purpose**: Cubrir seguridad, contrato HTTP, documentación y regresiones.

### Tests for API and security

- [ ] T052 [P] Crear `EstrategiaE2ETest` con MockMvc/`@SpringBootTest` para `200`, `401`, `403`, `404` y respuestas de administración.
- [ ] T053 [P] Crear `CotizacionE2ETest` con MockMvc/`@SpringBootTest` para `200`, `400` por rango inválido, `401`, `403` y `409`.
- [ ] T054 [P] Verificar con `@WithMockUser(roles = "SUPERUSUARIO")` y `@WithMockUser(roles = "USUARIO")` las reglas de autorización, sin modificar tests existentes.
- [ ] T055 [P] Verificar que `ErrorResponse`, `correlationId`, esquemas y códigos coincidan con `specs/005-contratos-entrega2/contracts/openapi.yaml`.

### Implementation and verification

- [ ] T056 Actualizar configuración `application.yml` con `app.cotizacion.cron`, zona/fecha fija del `Clock` y propiedades necesarias, sin secretos.
- [ ] T057 Revisar que el filtro JWT no se modifique para inventar roles de la feature 007; documentar la integración de authorities pendiente donde corresponda.
- [ ] T058 Ejecutar `mvn test` desde `backend`, incluyendo Testcontainers cuando estén disponibles, y corregir solo regresiones causadas por esta feature.
- [ ] T059 Verificar consultas batch/no-N+1, índices, unicidad, invalidación de caché, logs estructurados y ausencia de llamadas directas a `LocalDate.now()`/`Instant.now()` en Services.
- [ ] T060 Ejecutar `git diff --check` y revisar que los cambios de implementación se limiten al backend necesario y no incluyan frontend ni dependencias nuevas.

**Final checkpoint**: El backend compila, las migraciones ejecutan, los tests unitarios/integración/E2E pasan y el contrato de la Entrega 2 queda respetado.
