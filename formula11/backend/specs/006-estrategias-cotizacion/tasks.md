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

- [x] T001 [P] Crear `NormalizadorTest` en `backend/src/test/java/com/formula11/unit/model/valuacion/` para z-score de métricas sin acotar con desvío cero igual a `0`, z-score final acotado a `[-2,2]`, reescalado `[0,1]`, desvío final cero igual a `0,5` y grupo de un jugador.
- [x] T002 [P] Crear `CalculadoraPrecioTest` para precio de partida `1`, aplicación de fórmula en la primera semana, variación `null`, tope `±20 %`, piso `0,10`, cuatro decimales y `HALF_UP`.
- [x] T003 [P] Crear `RendimientoPosicionTest` para promedios semanales por partido, pesos por posición, proporción `menosDe90Minutos`, métricas positivas y negativas, estandarización de cada métrica sin acotar y normalización final del score por posición entre ligas.
- [x] T004 [P] Crear `ConsistenciaTest` para ventana de seis partidos, disponibilidad, promedio/desvío de rating, `lambda`, titularidad, suplencias y fallback con menos de cuatro partidos.
- [x] T005 [P] Crear pruebas de métricas faltantes para conteos como cero, rating ignorado con redistribución de pesos positivos y fila sin minutos devuelta en el resultado del cálculo para que el service la registre en el log.

### Implementation for domain

- [x] T006 [P] Crear records de entrada/salida necesarios para el cálculo (por ejemplo, las métricas de un jugador en un partido) en `backend/src/main/java/com/formula11/model/valuacion/`, sin anotaciones de JPA ni Spring; las métricas de conteo ausentes valen cero.
- [x] T007 [P] Crear `Normalizador` en `com.formula11.model.valuacion` con z-score de métricas sin acotar y fallback `0`, más z-score final acotado a `[-2,2]`, reescalado y fallback `0,5`.
- [x] T008 [P] Crear `CalculadoraPrecio` en `com.formula11.model.valuacion` usando `BigDecimal`, `RoundingMode.HALF_UP`, sensibilidad, tope y piso de la versión.
- [x] T009 Crear `EstrategiaValuacion` en `com.formula11.model.valuacion` como interfaz Strategy para scores brutos por jugador.
- [x] T010 Implementar `RendimientoPosicion` promediando métricas por jugador/semana, estandarizando cada métrica antes de aplicar pesos y separando el grupo por posición.
- [x] T011 Implementar `Consistencia` con los últimos seis partidos hasta el domingo, cálculo de disponibilidad y fallback al score normalizado de `RendimientoPosicion`.
- [x] T012 Usar el record anidado `CotizacionVigentePort.Cotizacion` definido por la interfaz existente en `com.formula11.service`; no crear un record duplicado en `com.formula11.model`.

**Checkpoint**: Los tests unitarios del dominio pasan sin cargar Spring ni PostgreSQL.

## Phase 2: Persistence and migrations

**Purpose**: Persistir métricas, estrategias versionadas y cotizaciones idempotentes, con consultas batch.

### Tests for persistence

- [x] T013 [P] Probar `ClockConfig` con zona `America/Argentina/Buenos_Aires` y fecha fija opcional para demos/tests.
- [x] T014 [P] Crear integración de `MetricaPartidoRepository` con Testcontainers para unicidad por jugador/fecha y consulta de toda una semana sin N+1.
- [x] T015 [P] Crear integración de `EstrategiaRepository` para seeds, versión vigente, historial descendente, estrategia activa y unicidad de activación.
- [x] T016 [P] Crear integración de `CotizacionRepository` para unicidad jugador/estrategia/fecha, consulta por fecha descendente, reemplazo semanal y lectura vigente `<= fecha`.

### Implementation for persistence

- [x] T017 Crear `ClockConfig` con bean `Clock` en `America/Argentina/Buenos_Aires` y propiedad opcional `app.clock.fecha-fija`.
- [x] T018 Crear `V10__crear_metricas_partido.sql` con tabla, foreign key a `jugadores`, campos de métricas, la secuencia para `MetricaPartido` usada por `GenerationType.SEQUENCE` con `allocationSize = 50` e índice `(jugador_id, fecha_partido)`.
- [x] T019 Crear entidad JPA `MetricaPartido` en `com.formula11.model` usando `GenerationType.SEQUENCE` con `allocationSize = 50`, crear su secuencia en V10 y agregar `MetricaPartidoRepository` con consulta batch por rango semanal y jugadores.
- [x] T020 Crear `V11__crear_estrategias.sql` con `IDENTITY`, `jsonb`, restricciones, versionado, índice/constraint de estrategia activa y seeds de `RENDIMIENTO_POSICION` y `CONSISTENCIA` versión 1; no crear secuencias en esta migración.
- [x] T021 Crear entidad JPA `Estrategia` en `com.formula11.model` usando `GenerationType.IDENTITY`, mapeando `pesos` y `parametros` con `@JdbcTypeCode(SqlTypes.JSON)`.
- [x] T022 Crear `V12__crear_cotizaciones.sql` con la secuencia para `Cotizacion` usada por `GenerationType.SEQUENCE` con `allocationSize = 50`, snapshot de pesos, índice `(jugador_id, fecha DESC)` y unicidad `(jugador_id, estrategia, fecha)`.
- [x] T023 Crear entidad JPA `Cotizacion` en `com.formula11.model` usando `GenerationType.SEQUENCE` con `allocationSize = 50`, crear su secuencia en V12 y agregar `CotizacionRepository` para historial, cotización previa y cotización vigente.
- [x] T024 Configurar `hibernate.jdbc.batch_size` y `hibernate.order_inserts` para habilitar inserts batch, y validar que el recálculo no cargue métricas jugador por jugador.

**Checkpoint**: Flyway V10–V12 ejecuta sobre V1–V3 y las integraciones de repositorios pasan contra PostgreSQL real.

## Phase 3: Strategy administration

**Purpose**: Administrar versiones inmutables y estrategia activa según el contrato y los roles.

### Tests for User Story 1

- [x] T025 [P] Probar `EstrategiaService` para listar las dos estrategias, marcar la activa y devolver versiones de la más nueva a la más vieja.
- [x] T026 [P] Probar creación de versión con pesos/params válidos, incremento de versión, inmutabilidad de la anterior y rechazo de métrica/posición/parámetro inválido.
- [x] T027 [P] Probar activación de estrategia existente y `404` para código desconocido.

### Implementation for User Story 1

- [x] T028 Implementar `EstrategiaService` para listar, consultar versiones, crear versiones inmutables y activar una estrategia.
- [x] T029 Crear `EstrategiaResponse` y `NuevaVersionRequest` como records con validaciones contractuales para pesos y parámetros.
- [x] T030 Crear `EstrategiaController` con `GET /api/estrategias`, `GET/POST /api/estrategias/{codigo}/versiones` y `POST /api/estrategias/{codigo}/activar`, documentado con `@Operation`/`@ApiResponse`.
- [x] T031 Configurar `@EnableMethodSecurity` y `@PreAuthorize("hasRole('SUPERUSUARIO')")` para crear versiones y activar; mantener el listado disponible para usuarios autenticados.
- [x] T032 Agregar a `GlobalExceptionHandler` el mapeo explícito de `AccessDeniedException` a `403 ErrorResponse`.

**Checkpoint**: La administración respeta el contrato, la inmutabilidad y `USUARIO` no puede administrar estrategias.

## Phase 4: Weekly recalculation and current quotation

**Purpose**: Calcular, persistir y reemplazar semanas completas con el mismo service para HTTP y scheduler.

### Tests for User Stories 2 and 4

- [x] T033 [P] Probar `RecalculoService` sin cuerpo: procesa el domingo de la última semana cerrada anterior al `Clock`, no la semana en curso.
- [x] T034 [P] Probar recálculo de rango inclusivo, rango inválido, `desde`/`hasta` incompletos y continuidad con cotizaciones anteriores y posteriores al rango.
- [x] T035 [P] Probar semana sin partidos, jugador sin métricas y primera semana: jugador sin juego conserva el precio anterior o `1` si no existe, jugador que juega aplica la fórmula sobre el precio anterior o `1`, y un jugador sin métricas en ninguna semana permanece en `1`; verificar variación nullable en la primera cotización.
- [x] T036 [P] Probar reemplazo idempotente de una semana, transacción independiente por semana y `409` cuando existe otro recálculo en curso.
- [x] T037 [P] Probar respuesta `RecalculoResponse` con estado, contadores, estrategias/versiones, inicio, fin y `correlationId`.

### Implementation for recalculation

- [x] T038 Crear `RecalculoRequest` y `RecalculoResponse` como records alineados con los esquemas del contrato.
- [x] T039 Implementar `RecalculoSemanaService` como componente separado con `@Transactional`, eliminación/reemplazo semanal y persistencia batch.
- [x] T040 Implementar `RecalculoService` con lock en memoria, validación de rango, cálculo del domingo cerrado mediante `Clock` y procesamiento de ambas estrategias.
- [x] T041 Implementar selección batch de métricas por semana/posición y adaptación de entidades a entradas de `model.valuacion`.
- [x] T042 Aplicar la cadena de precios: buscar la última cotización de la misma estrategia con fecha anterior; usar `1` solo como momento cero; extender backfill hasta la última semana ya calculada.
- [x] T043 Crear `CotizacionController` para `POST /api/cotizaciones/recalcular`, con DTO, autorización, documentación y respuestas `200`, `400`, `401`, `403` y `409`.

**Checkpoint**: Una ejecución manual y un backfill producen series completas, trazables e idempotentes.

## Phase 5: Scheduler, Clock, cache and market port

**Purpose**: Automatizar el recálculo, aislar la actualización de métricas y servir la cotización vigente.

### Tests for scheduler and integration

- [x] T044 [P] Probar el scheduler con `ActualizacionMetricasPort` exitoso, fallido y sin implementación: siempre registra el evento y recalcula con métricas locales.
- [x] T045 [P] Probar `CotizacionVigenteService` usando `vigente(Jugador, Instant)`, conversión con la zona del `Clock`, estrategia activa, fecha `<=` local, precio inicial con `fechaCotizacion`/estrategia/versión `null` y cache hit/miss.
- [x] T046 [P] Probar invalidación de caché después de recálculo confirmado y activación de otra estrategia.

### Implementation for scheduler and integration

- [x] T047 Crear `ActualizacionMetricasPort` y su implementación inicial no operativa, registrando que la carga queda a cargo de la feature 011.
- [x] T048 Crear `CotizacionCacheConfig` con caché Caffeine propia, clave/tipo aislado de `footballApiCache` e invalidación explícita; conectar la invalidación en `EstrategiaService` al activar y en `RecalculoService` al terminar.
- [x] T049 Implementar sin modificar la interfaz existente `CotizacionVigentePort` en `com.formula11.service`, usando su método `vigente(Jugador, Instant)` y su record `CotizacionVigentePort.Cotizacion`; convertir el `Instant` con la zona del `Clock`, resolver la última cotización de la estrategia activa o el precio inicial y eliminar `CotizacionFijaAdapter` si existe al mergear.
- [x] T050 Crear `CotizacionScheduler` con `@EnableScheduling`, cron configurable `app.cotizacion.cron` por defecto lunes 06:00, MDC/correlationId y limpieza en `finally`.
- [x] T051 Hacer que el scheduler capture y registre fallas de `ActualizacionMetricasPort` y llame el mismo método de `RecalculoService` con la semana cerrada.

**Checkpoint**: El job corre con `Clock`, continúa ante fallas de métricas y el puerto de mercado devuelve datos cacheables y coherentes.

## Phase 6: Contract, security and quality gates

**Purpose**: Cubrir seguridad, contrato HTTP, documentación y regresiones.

### Tests for API and security

- [x] T052 [P] Crear `EstrategiaE2ETest` con MockMvc/`@SpringBootTest` para `200`, `401`, `403`, `404` y respuestas de administración.
- [x] T053 [P] Crear `CotizacionE2ETest` con MockMvc/`@SpringBootTest` para `200`, `400` por rango inválido, `401`, `403` y `409`.
- [x] T054 [P] Verificar con `@WithMockUser(roles = "SUPERUSUARIO")` y `@WithMockUser(roles = "USUARIO")` las reglas de autorización, sin modificar tests existentes.
- [x] T055 [P] Verificar que `ErrorResponse`, `correlationId`, esquemas y códigos coincidan con `specs/005-contratos-entrega2/contracts/openapi.yaml`.

### Implementation and verification

- [x] T056 Actualizar configuración `application.yml` con `app.cotizacion.cron`, zona/fecha fija del `Clock` y propiedades necesarias, sin secretos.
- [x] T057 Revisar que el filtro JWT no se modifique para inventar roles de la feature 007; documentar la integración de authorities pendiente donde corresponda.
- [x] T058 Ejecutar `mvn test` desde `backend`, incluyendo Testcontainers cuando estén disponibles, y corregir solo regresiones causadas por esta feature.
- [x] T059 Verificar consultas batch/no-N+1, índices, unicidad, invalidación de caché, logs estructurados y ausencia de llamadas directas a `LocalDate.now()`/`Instant.now()` en Services.
- [x] T060 Ejecutar `git diff --check` y revisar que los cambios de implementación se limiten al backend necesario y no incluyan frontend ni dependencias nuevas.

**Final checkpoint**: El backend compila, las migraciones ejecutan, los tests unitarios/integración/E2E pasan y el contrato de la Entrega 2 queda respetado.
