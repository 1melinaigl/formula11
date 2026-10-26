# Implementation Plan: Estrategias de valuación y cotización semanal de jugadores

**Branch**: `006-estrategias-cotizacion` | **Date**: 2026-10-24 | **Spec**: [spec.md](spec.md)

## Summary

Implementar el cálculo semanal de cotizaciones para las dos estrategias versionadas del mercado, su administración protegida para `SUPERUSUARIO`, el recálculo manual/backfill y el job programado. La lógica matemática quedará en `model`, sin Spring ni base de datos; los services orquestarán persistencia, autorización, caché, transacciones y observabilidad.

El endpoint manual y el scheduler llamarán al mismo método de `RecalculoService`. Cada semana será una transacción independiente mediante un componente separado, y la cotización vigente se resolverá por la estrategia activa a través de `CotizacionVigentePort`.

## Technical Context

**Language/Version**: Java 21, Spring Boot 3.5.5

**Dependencies**: Se reutilizan Spring Web, Validation, Data JPA, Security, Cache, Flyway, PostgreSQL, Caffeine, JJWT, springdoc, JUnit 5 y Testcontainers ya declarados en [pom.xml](../../pom.xml). No se agregan dependencias.

**Persistence**: PostgreSQL con Flyway y Hibernate 6. Las columnas `pesos` y `parametros` usarán `@JdbcTypeCode(SqlTypes.JSON)` sobre `jsonb`.

**Testing**: Unitarios puros de `model`; integración de repositorios y services con PostgreSQL mediante Testcontainers; controllers/E2E con MockMvc y `@SpringBootTest`. No se modifican ni eliminan tests existentes.

**Existing constraints**:

- El backend usa paquetes `model`, `persistence`, `service`, `controller`, `dto`, `config`, `exception` y `security`.
- El esquema actual solo contiene migraciones V1–V3; las nuevas migraciones deben conservar compatibilidad con `jugadores.id`, `jugadores.liga` y `jugadores.posicion`.
- [SecurityConfig.java](../../src/main/java/com/formula11/security/SecurityConfig.java) autentica por JWT, pero [JwtAuthenticationFilter.java](../../src/main/java/com/formula11/security/JwtAuthenticationFilter.java) actualmente no copia authorities desde el token; los tests de esta feature usarán `@WithMockUser`, y la lectura de roles desde JWT queda coordinada con la feature 007.
- [GlobalExceptionHandler.java](../../src/main/java/com/formula11/exception/GlobalExceptionHandler.java) necesita un handler explícito para `AccessDeniedException`, antes del fallback `Exception`.
- [FootballCacheConfig.java](../../src/main/java/com/formula11/config/FootballCacheConfig.java) ya demuestra el patrón de Caffeine que debe seguir la caché de cotización.

## Constitution Check

- [x] **Tiempo controlable**: `Clock` será un bean inyectable en services que dependan de fecha/hora; no se usará `LocalDate.now()` ni `Instant.now()` directamente.
- [x] **Service reutilizable**: el endpoint y el scheduler delegarán en el mismo método de `RecalculoService`; no habrá lógica de negocio en el job.
- [x] **Capas**: fórmulas y entidades de dominio no dependerán de Spring; controllers solo traducirán HTTP y DTOs.
- [x] **Validación por nivel**: DTOs validarán forma; services validarán existencia, roles y orquestación; el dominio validará invariantes y parámetros.
- [x] **Persistencia y trazabilidad**: cotizaciones guardarán estrategia, versión y snapshot de pesos; cada semana será reemplazable e idempotente.
- [x] **Seguridad**: endpoints de administración usarán `@PreAuthorize("hasRole('SUPERUSUARIO')")`; usuarios autenticados solo consultarán.
- [x] **Calidad**: se agregarán unitarios, integración Testcontainers y E2E/MockMvc en `e2e`, manteniendo los tests existentes.
- [x] **Observabilidad**: scheduler y recálculo usarán logs estructurados y `correlationId`; fallas de actualización de métricas no impedirán el recálculo con datos locales.

## Architecture and Design

### Domain model

Crear en `com.formula11.model`:

- `MetricaPartido`: datos de jugador/partido, con valores de conteo ausentes tratados como cero y validación de minutos.
- `CotizacionVigentePort.CotizacionVigente`: record anidado de la interfaz existente en `com.formula11.service`, definida por la feature 007 y no modificable por esta feature.

Las entidades JPA `MetricaPartido`, `Estrategia` y `Cotizacion` viven en `com.formula11.model`, sin sufijo, siguiendo la convención de `Jugador` y `Usuario`.

Crear la lógica de cálculo pura en `com.formula11.model.valuacion`, sin anotaciones de JPA ni de Spring:

- `EstrategiaValuacion`: interfaz pura que recibe métricas, posición, ventana y parámetros y devuelve scores brutos por jugador.
- `RendimientoPosicion`: promedia cada métrica por partido en la semana, calcula `menosDe90Minutos` como proporción, estandariza cada métrica dentro de posición sin acotar (cero si el desvío es cero), aplica los pesos y normaliza siempre la suma ponderada con z-score final acotado a `[-2,2]` y reescalado a `(z + 2) / 4`.
- `Consistencia`: usa los últimos seis partidos hasta el domingo, disponibilidad, promedio y desvío del rating, más titularidad/suplencias; con menos de cuatro partidos delega en el score normalizado de `RendimientoPosicion` y, para los demás, aplica únicamente la normalización final del score bruto.
- `Normalizador`: soporta el z-score de métricas sin acotar y el z-score final acotado a `[-2, 2]` y reescalado a `(z + 2) / 4`; el nivel de métrica devuelve `0` con desvío cero y el nivel final devuelve `0,5`.
- `CalculadoraPrecio`: aplica la fórmula con `BigDecimal`, `RoundingMode.HALF_UP`, tope de variación y piso configurables.
- Records de entrada y salida que necesiten las estrategias y el normalizador.

Para cada semana y estrategia, un jugador que no jugó conserva el precio anterior o recibe `1` crédito si no tiene ninguno; un jugador que jugó aplica `precio_t = precio_{t-1} * (1 + 0,4 * (score_t - 0,5))` sobre el precio anterior o sobre `1` crédito si no tiene ninguno. Un jugador sin métricas en ninguna semana permanece en `1` crédito.

Los adaptadores de persistencia convertirán las entidades a entradas del dominio y viceversa.

### Persistence and migrations

Crear entidades/repositorios JPA en `model`/`persistence`:

- `MetricaPartido` y repositorio con consulta por semana para todos los jugadores, evitando N+1.
- `Estrategia` y repositorio para versión vigente, historial ordenado y estrategia activa.
- `Cotizacion` y repositorio para consulta por estrategia/jugador/fecha, reemplazo semanal y lectura vigente.

Crear migraciones Flyway:

- `V10__crear_metricas_partido.sql`: tabla, foreign key a `jugadores`, columnas de métricas e índice `(jugador_id, fecha_partido)`.
- `V11__crear_estrategias.sql`: tabla versionada, `jsonb`, constraints, índice de estrategia activa y seed de ambas estrategias versión 1 con `RENDIMIENTO_POSICION` activa.
- `V12__crear_cotizaciones.sql`: tabla, `jsonb` para snapshot de pesos, índice `(jugador_id, fecha DESC)` y unicidad `(jugador_id, estrategia, fecha)`.
V13 y V14 quedan libres dentro del rango reservado para futuras evoluciones.

### Services and adapters

- `RecalculoService`: valida rango, determina el domingo de la última semana cerrada con la fecha de negocio del `Clock` UTC en `ClockConfig.ZONA_NEGOCIO`, toma un lock en memoria, procesa semanas y devuelve `RecalculoResponse`. La selección de métricas se hace por semana/posición en lote.
- `RecalculoSemanaService`: componente separado con método `@Transactional` para una semana; elimina/reemplaza las cotizaciones de esa semana y persiste en batch. Esto evita self-invocation de `@Transactional`.
- `EstrategiaService`: listar, historial, crear versión inmutable y activar estrategia; invalida caché al activar.
- `CotizacionVigenteService`: implementa `CotizacionVigentePort.vigente(Jugador, Instant)` sin modificar la interfaz, convierte el `Instant` a `LocalDate` con `ClockConfig.ZONA_NEGOCIO` (el `Clock` inyectado es UTC) y consulta la estrategia activa y la última cotización con fecha menor o igual; sin registro devuelve `CotizacionVigentePort.CotizacionVigente(1, null, null, null)`. Si existe `CotizacionFijaAdapter` al mergear, se elimina.
- `ActualizacionMetricasPort`: interfaz invocada antes del recálculo del job. Su implementación inicial registra que no hay actualización operativa, sin fallar; cualquier excepción del adapter se registra y no impide recalcular con datos persistidos.
- `CotizacionCacheConfig`: caché Caffeine siguiendo el patrón de `FootballCacheConfig`; invalidación tras cada recálculo confirmado y al activar estrategia.

Las filas excluidas por falta de minutos son una salida del dominio, no un efecto de logging: el resultado del cálculo debe devolverlas y el service es responsable de registrarlas.

### Web, configuration and security

Crear records DTO:

- `RecalculoRequest`, `RecalculoResponse`.
- `EstrategiaResponse`, `NuevaVersionRequest`.

Crear controllers documentados con `@Operation` y `@ApiResponse`:

- `CotizacionController`: `POST /api/cotizaciones/recalcular`.
- `EstrategiaController`: `GET /api/estrategias`, `GET/POST /api/estrategias/{codigo}/versiones`, `POST /api/estrategias/{codigo}/activar`.

Validar que los códigos, rangos, pesos, posiciones y parámetros produzcan los códigos `400`, `401`, `403`, `404` y `409` definidos por el contrato. Agregar `@EnableMethodSecurity` y `AccessDeniedException` → `403` en el handler global.

Crear `ClockConfig` con un bean `Clock` en UTC, la constante `ClockConfig.ZONA_NEGOCIO` en `America/Argentina/Buenos_Aires` y la propiedad opcional `app.clock.fecha-fija` para demos/tests. Las semanas y fechas de cotización deben calcularse con la zona de negocio. Configurar `@EnableScheduling`, `app.cotizacion.cron` con lunes 06:00 como default y job que:

1. genera `correlationId` propio en MDC;
2. llama `ActualizacionMetricasPort`;
3. registra cualquier falla/no implementación;
4. llama el mismo método de recálculo para la última semana cerrada;
5. limpia MDC en `finally`.

## Project Structure

```text
backend/
├── src/main/java/com/formula11/
│   ├── config/{ClockConfig,CotizacionCacheConfig,SchedulingConfig}.java
│   ├── controller/{CotizacionController,EstrategiaController}.java
│   ├── dto/{RecalculoRequest,RecalculoResponse,EstrategiaResponse,NuevaVersionRequest}.java
│   ├── exception/{GlobalExceptionHandler,...}.java
│   ├── model/
│   │   ├── {MetricaPartido,Estrategia,Cotizacion}.java
│   │   └── valuacion/
│   │       ├── {EstrategiaValuacion,RendimientoPosicion,Consistencia}.java
│   │       ├── {Normalizador,CalculadoraPrecio}.java
│   │       └── records de entrada y salida
│   ├── persistence/{MetricaPartidoRepository,EstrategiaRepository,CotizacionRepository}.java
│   └── service/
│       ├── {RecalculoService,RecalculoSemanaService,EstrategiaService}.java
│       ├── {CotizacionVigentePort,CotizacionVigenteService,ActualizacionMetricasPort}.java
│       └── scheduler/CotizacionScheduler.java
├── src/main/resources/db/migration/
│   ├── V10__crear_metricas_partido.sql
│   ├── V11__crear_estrategias.sql
│   └── V12__crear_cotizaciones.sql
├── src/test/java/com/formula11/
│   ├── unit/model/{NormalizadorTest,CalculadoraPrecioTest,RendimientoPosicionTest,ConsistenciaTest}.java
│   ├── integration/{MetricaPartidoRepositoryIntegrationTest,EstrategiaRepositoryIntegrationTest,
│   │   CotizacionRepositoryIntegrationTest,RecalculoServiceIntegrationTest}.java
│   └── e2e/{EstrategiaE2ETest,CotizacionE2ETest}.java
└── specs/006-estrategias-cotizacion/{spec.md,plan.md,research.md}
```

## Implementation Sequence

1. Implementar dominio puro y sus tests: normalización, métricas faltantes, estrategias y precio.
2. Crear migraciones V10–V12; mapear entidades y repositorios con consultas batch.
3. Implementar DTOs, services, puerto de cotización vigente y caché.
4. Implementar recálculo semanal/backfill y transacción por semana.
5. Agregar configuración de `Clock`, scheduler, `ActualizacionMetricasPort`, MDC y logs.
6. Integrar seguridad por método, handler `403`, controllers y documentación OpenAPI.
7. Ejecutar integración/Testcontainers, E2E/MockMvc y suite existente; corregir solo regresiones causadas por esta feature.

## Verification

- `mvn test` desde `backend`.
- Tests unitarios enfocados de `model`.
- Tests de repositorios y recálculo con Testcontainers PostgreSQL.
- Tests MockMvc/E2E de `200`, `400`, `401`, `403`, `404` y `409`.
- Verificación de no-N+1 mediante consultas batch y de unicidad por jugador/estrategia/semana.
- Verificación de que scheduler y endpoint producen el mismo resultado con `Clock` fijo.
- `git diff --check` y validación de que solo se modifiquen archivos dentro de la feature y las fuentes necesarias del backend.

## Complexity Tracking

| Riesgo | Por qué aparece | Mitigación |
|---|---|---|
| Transacción semanal en backfill | El recálculo debe reemplazar semanas sin dejar estados parciales | Componente separado con `@Transactional` y método idempotente por semana |
| Roles JWT incompletos en el código actual | El filtro actual autentica sin authorities | Mantener tests con `@WithMockUser`; coordinar claims/authorities con feature 007 |
| Normalización de métricas | Cada métrica se estandariza antes de ponderar y hay faltantes distintos | Dominio puro, casos de borde explícitos y tests deterministas |
