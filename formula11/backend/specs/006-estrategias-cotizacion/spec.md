# Feature Specification: Estrategias de valuación y cotización semanal de jugadores

**Feature Branch**: `006-estrategias-cotizacion`

**Created**: 2026-10-24

**Status**: Draft

**Input**: User description: "Estrategias de valuación y cotización semanal de jugadores"

**Related contract**: [Contrato de la Entrega 2](../005-contratos-entrega2/contracts/openapi.yaml). Esta feature implementa únicamente las operaciones que tienen `x-feature: 006-estrategias-cotizacion`.

## Scope

Esta feature calcula y persiste la cotización semanal de cada jugador, conserva su historial, permite administrar estrategias versionadas y expone el recálculo manual definido por el contrato. El mercado utiliza la estrategia activa.

No implementa:

- La carga desde WhoScored, que pertenece a la feature `011-scraper-whoscored`.
- Detalle de jugador, historial público, cotización a una fecha ni ranking, que pertenecen a la feature `008`.
- Detector de subvaluados, que pertenece a la feature `010`.
- Compra, venta, creación del superusuario ni otras operaciones de la feature `007`.
- Frontend.

La tabla `metrica_partido` se llena con datos de prueba en esta feature. La integración futura debe hacerlo mediante la interfaz de actualización de métricas, sin acoplar el cálculo al scraper.

## User Scenarios & Testing

### User Story 1 - Consultar y administrar estrategias (Priority: P1)

Como usuario autenticado quiero consultar las estrategias disponibles y su versión vigente; como superusuario quiero crear versiones nuevas y elegir cuál utiliza el mercado.

**Why this priority**: Las estrategias determinan cómo se calcula cada cotización y deben ser configurables sin perder trazabilidad histórica.

**Independent Test**: Con `@WithMockUser(roles = "SUPERUSUARIO")`, listar las estrategias sembradas, crear una versión con pesos modificados y activar una estrategia. Con `@WithMockUser(roles = "USUARIO")`, verificar que puede consultar la estrategia activa pero recibe `403` al administrarla.

**Acceptance Scenarios**:

1. **Given** no existen configuraciones previas, **When** se inicializa el mercado, **Then** existen `RENDIMIENTO_POSICION` y `CONSISTENCIA`, y `RENDIMIENTO_POSICION` es la estrategia activa por defecto.
2. **Given** un usuario autenticado consulta `GET /api/estrategias`, **When** la operación termina, **Then** recibe las dos estrategias con su versión vigente y la marca `activa`.
3. **Given** un usuario autenticado consulta `GET /api/estrategias/{codigo}/versiones`, **When** el código existe, **Then** recibe las versiones de la más nueva a la más vieja.
4. **Given** un superusuario envía `POST /api/estrategias/{codigo}/versiones` con pesos y parámetros válidos, **When** se procesa la solicitud, **Then** se crea una versión nueva sin modificar la anterior y se devuelve `201` con `EstrategiaResponse`.
5. **Given** una nueva versión fue creada, **When** ocurre el próximo recálculo, **Then** se usa esa versión; las cotizaciones ya guardadas conservan la estrategia, versión y pesos que las produjeron.
6. **Given** un superusuario activa una estrategia existente, **When** llama a `POST /api/estrategias/{codigo}/activar`, **Then** la respuesta `200` la marca como activa, se invalida la caché de cotización vigente y el mercado la usa desde ese momento.
7. **Given** un usuario sin rol `SUPERUSUARIO` intenta crear una versión o activar una estrategia, **When** llama al endpoint, **Then** recibe `403` con el esquema `ErrorSoloSuperusuario`.

### User Story 2 - Calcular la cotización semanal (Priority: P1)

Como superusuario quiero recalcular las dos estrategias para todos los jugadores, de forma manual o automática, para actualizar sus precios y conservar la evolución semanal.

**Why this priority**: La cotización periódica es la capacidad central de la feature y alimenta al mercado.

**Independent Test**: Sembrar jugadores y métricas de varios partidos, invocar `POST /api/cotizaciones/recalcular` y comprobar dos series de cotizaciones por jugador, el precio, score, variación y trazabilidad de estrategia.

**Acceptance Scenarios**:

1. **Given** jugadores y métricas persistidas, **When** un superusuario llama `POST /api/cotizaciones/recalcular` sin cuerpo, **Then** se calcula la semana actual para las dos estrategias y se devuelve `200` con `RecalculoResponse`.
2. **Given** una semana ya calculada, **When** se recalcula esa semana, **Then** sus cotizaciones se reemplazan transaccionalmente y no se duplican.
3. **Given** el scheduler semanal se dispara, **When** solicita la actualización de métricas mediante la interfaz disponible, **Then** después llama al mismo método de service que usa el endpoint manual; el scheduler no contiene lógica de negocio.
4. **Given** el jugador tiene una cotización previa, **When** el score semanal se calcula, **Then** el precio es `precio_t = precio_{t-1} * (1 + k * (score_t - 0.5))`, con `k = 0,4`, variación limitada a `±20 %` y piso de `0,10` créditos.
5. **Given** el jugador no tiene cotización previa, **When** se calcula su primera semana, **Then** el precio inicial es `1` crédito y la variación es nula en la respuesta.
6. **Given** dos estrategias calculan la misma semana, **When** se persisten los resultados, **Then** cada una conserva su propia serie de precios y cada registro incluye código, versión y copia de los pesos utilizados.
7. **Given** finaliza correctamente un recálculo, **When** se confirma la transacción, **Then** se invalida la caché de cotización vigente y el log estructurado contiene `correlationId`, inicio, fin, semanas y resultado.

### User Story 3 - Calcular estrategias a partir de métricas (Priority: P1)

Como sistema quiero convertir las métricas de partido en un score normalizado por posición para que las cotizaciones reflejen rendimiento y consistencia.

**Why this priority**: Las estrategias deben ser configurables, reproducibles y comparables entre todas las ligas.

**Independent Test**: Ejecutar pruebas unitarias con métricas controladas para cada posición y verificar pesos positivos, negativos, normalización, disponibilidad, fallback y casos sin división por cero.

**Acceptance Scenarios**:

1. **Given** métricas de jugadores de una misma posición en todas las ligas, **When** se calcula `RENDIMIENTO_POSICION`, **Then** se suman las métricas configuradas para POR, DEF, MED o DEL, se aplican pesos positivos y negativos y el resultado se normaliza en `[0,1]` dentro de esa posición.
2. **Given** un arquero con atajadas, valla invicta, goles recibidos y tarjetas, **When** se calcula `RENDIMIENTO_POSICION`, **Then** se incluyen atajadas y valla invicta y se restan goles recibidos, amarillas y rojas según los pesos de POR.
3. **Given** un jugador con menos de 90 minutos, **When** se calcula `RENDIMIENTO_POSICION`, **Then** se aplica el peso de `menosDe90Minutos`.
4. **Given** los últimos seis partidos del jugador, **When** se calcula `CONSISTENCIA`, **Then** el score es `disponibilidad * (rating promedio - lambda * desvío estándar del rating)`, sumando titularidad y restando suplencias según los pesos configurados.
5. **Given** el jugador tiene menos de cuatro partidos en la ventana de `CONSISTENCIA`, **When** se calcula su score, **Then** se utiliza el score de `RENDIMIENTO_POSICION`.
6. **Given** un grupo de normalización tiene un solo jugador o todos sus valores son iguales, **When** se normaliza, **Then** no se divide por cero y se produce un score válido en `[0,1]`.

### User Story 4 - Recalcular evolución histórica (Priority: P2)

Como superusuario quiero recalcular un rango de semanas usando las métricas persistidas para mostrar la evolución histórica desde el inicio de la temporada.

**Why this priority**: Permite reconstruir la serie de precios sin depender de una ejecución previa del scheduler.

**Independent Test**: Enviar `RecalculoRequest` con `desde` y `hasta`, verificar todas las semanas inclusivas, la continuidad de precios y la respuesta con la cantidad de semanas y jugadores procesados.

**Acceptance Scenarios**:

1. **Given** un rango válido con `desde` y `hasta`, **When** se solicita el recálculo, **Then** se recalculan todas las semanas del rango inclusive con las métricas persistidas y el `Clock` inyectado.
2. **Given** solo se envía `desde` o solo `hasta`, **When** se valida la solicitud, **Then** se responde `400`.
3. **Given** `desde` es posterior a `hasta`, **When** se valida la solicitud, **Then** se responde `400` con `ErrorValidacion`.
4. **Given** no hay métricas para un jugador en una semana, **When** se procesa esa semana, **Then** se guarda una cotización con el mismo precio anterior para que la serie no tenga huecos.
5. **Given** un jugador no tiene métricas cargadas, **When** se procesa cualquier semana, **Then** cotiza a `1` crédito y no varía.

## Edge Cases

- Métricas faltantes en un partido se tratan como valores ausentes definidos por el dominio y no provocan una cotización inválida; el caso queda cubierto por pruebas unitarias.
- Un jugador sin partidos en una semana conserva el precio anterior.
- Un jugador sin métricas conserva el precio inicial de `1` crédito.
- Un grupo de normalización con un solo jugador o valores iguales genera un score válido y no lanza división por cero.
- Los precios se redondean a cuatro decimales y nunca quedan por debajo de `0,10` créditos.
- Los pesos pueden ser positivos o negativos; una métrica desconocida, una posición faltante o un parámetro fuera de rango produce `400`.
- Una semana ya calculada se reemplaza, no se duplica.
- Si otro recálculo está en curso, el nuevo pedido responde `409` con `Error`.
- Si falla una semana de un rango, no se confirma parcialmente esa semana; cada semana es una unidad transaccional.
- Si falla la actualización de métricas previa del scheduler, el job no inventa datos ni ejecuta un recálculo incompleto.
- La concurrencia entre activación y recálculo debe dejar una estrategia activa coherente y debe invalidar la caché al terminar ambas operaciones.
- Un código de estrategia inexistente responde `404`; un usuario no autenticado responde `401`.

## Requirements

### Functional Requirements

- **FR-001**: El sistema MUST persistir `metrica_partido` con jugador, fecha del partido, rival, minutos, titular, goles, asistencias, tiros, pases clave, regates, entradas, intercepciones, amarillas, rojas, atajadas, goles recibidos, valla invicta, rating y fuente.
- **FR-002**: `metrica_partido` MUST tener unicidad por `(jugador, fecha del partido)`.
- **FR-003**: El sistema MUST persistir `estrategia` con código, nombre, versión, pesos, parámetros JSONB, indicador activa y fecha de creación.
- **FR-004**: El sistema MUST crear las estrategias `RENDIMIENTO_POSICION` y `CONSISTENCIA`, con pesos y parámetros válidos, y activar `RENDIMIENTO_POSICION` por defecto.
- **FR-005**: Cada peso MUST identificar una métrica y una posición (`POR`, `DEF`, `MED`, `DEL`) para `RENDIMIENTO_POSICION`, o `TODAS` para `CONSISTENCIA`; un peso negativo MUST restar del score.
- **FR-006**: `RENDIMIENTO_POSICION` MUST contemplar goles, asistencias, tiros, pases clave, regates, entradas, intercepciones y rating; para POR también atajadas y valla invicta; y debe poder penalizar amarillas, rojas, goles recibidos y menos de 90 minutos.
- **FR-007**: `CONSISTENCIA` MUST usar los últimos seis partidos, disponibilidad, promedio y desvío estándar del rating, `lambda`, titularidad y suplencias; con menos de cuatro partidos MUST usar `RENDIMIENTO_POSICION`.
- **FR-008**: Los scores MUST normalizarse en `[0,1]` dentro de la misma posición y comparando todas las ligas.
- **FR-009**: Modificar pesos o parámetros MUST crear una nueva versión inmutable y MUST comenzar a usarse desde el próximo recálculo.
- **FR-010**: El endpoint `GET /api/estrategias` MUST devolver las estrategias con su versión vigente y la activa, conforme a `EstrategiaResponse`.
- **FR-011**: El endpoint `GET /api/estrategias/{codigo}/versiones` MUST devolver el historial de versiones de la más nueva a la más vieja y responder `404` si el código no existe.
- **FR-012**: Solo `SUPERUSUARIO` MUST poder crear versiones, activar estrategias y solicitar recálculos; los demás usuarios autenticados MUST recibir `403`.
- **FR-013**: El endpoint `POST /api/estrategias/{codigo}/versiones` MUST aceptar `NuevaVersionRequest` y responder `201` con `EstrategiaResponse`, o `400`, `401`, `403` o `404` según el contrato.
- **FR-014**: El endpoint `POST /api/estrategias/{codigo}/activar` MUST responder `200` con `EstrategiaResponse`, o `401`, `403` o `404` según el contrato.
- **FR-015**: El sistema MUST persistir `cotizacion` con jugador, fecha, precio, score, estrategia, versión y copia de los pesos usados; MUST mantener un índice por `(jugador_id, fecha DESC)`.
- **FR-016**: Cada semana MUST calcular y guardar una cotización para las dos estrategias, con series de precios independientes; el mercado MUST operar con la activa.
- **FR-017**: La primera cotización MUST ser de `1` crédito. Las siguientes MUST usar `k = 0,4`, limitar la variación semanal a `±20 %`, aplicar piso de `0,10` y persistir `BigDecimal` con cuatro decimales.
- **FR-018**: El endpoint `POST /api/cotizaciones/recalcular` MUST aceptar opcionalmente `RecalculoRequest`, recalcular la semana actual sin cuerpo, o todas las semanas inclusivas de un rango, y responder `RecalculoResponse`.
- **FR-019**: El recálculo MUST reemplazar una semana existente y ser transaccional por semana; un recálculo concurrente MUST responder `409`.
- **FR-020**: El scheduler MUST tener cron configurable, solicitar primero la actualización de métricas mediante una interfaz y luego invocar el mismo método de service del endpoint manual. No MUST contener lógica de negocio.
- **FR-021**: Todo Service que use fecha u hora actual MUST recibir un `java.time.Clock` inyectado; no MUST llamar directamente a `LocalDate.now()` ni `Instant.now()`.
- **FR-022**: La compra o venta de tokens no forma parte de esta feature, pero cualquier integración futura MUST delegar en métodos reutilizables de Service; un Controller solo traduce HTTP.
- **FR-023**: La implementación de `CotizacionVigentePort` MUST exponer `obtener(Long jugadorId, LocalDate fecha)` y devolver el último registro de la estrategia activa con fecha menor o igual a la solicitada.
- **FR-024**: Si no existe cotización vigente, `CotizacionVigentePort` MUST devolver `CotizacionVigente(jugadorId, 1 crédito, fecha solicitada, null, null)`. El puerto debe estar en `com.formula11.service` y el record en `com.formula11.model` si todavía no existen; debe eliminarse `CotizacionFijaAdapter` si es provisoria.
- **FR-025**: La caché de cotización vigente MUST invalidarse al finalizar cada recálculo y al activar otra estrategia.
- **FR-026**: Los logs estructurados del scheduler y del recálculo MUST incluir `correlationId` sin registrar secretos.
- **FR-027**: Las migraciones Flyway MUST cubrir V10 a V14 para las tablas, índices, seeds y evolución de esquema requeridos.
- **FR-028**: Los endpoints MUST respetar autenticación JWT, roles, esquemas, códigos de respuesta y nombres definidos en el contrato de la Entrega 2; los errores MUST usar `Error` o el response específico del contrato.

### Contract Surface

| Operación | Método y ruta | Autorización | Respuestas de éxito |
|---|---|---|---|
| Recálculo | `POST /api/cotizaciones/recalcular` | `SUPERUSUARIO` | `200 RecalculoResponse` |
| Listado vigente | `GET /api/estrategias` | Usuario autenticado | `200 EstrategiaResponse[]` |
| Historial de versiones | `GET /api/estrategias/{codigo}/versiones` | Usuario autenticado | `200 EstrategiaResponse[]` |
| Crear versión | `POST /api/estrategias/{codigo}/versiones` | `SUPERUSUARIO` | `201 EstrategiaResponse` |
| Activar estrategia | `POST /api/estrategias/{codigo}/activar` | `SUPERUSUARIO` | `200 EstrategiaResponse` |

No se agregan endpoints para la carga de métricas ni para consultar historial público, cotización a una fecha o ranking.

### Key Entities

- **MetricaPartido**: métricas de un jugador en un partido, identificadas de forma única por jugador y fecha.
- **Estrategia**: configuración versionada de una fórmula, con pesos por métrica, parámetros, estado activo y fecha de creación.
- **Cotizacion**: resultado semanal por jugador y estrategia, con precio, score y snapshot de la configuración usada.
- **CotizacionVigente**: record de integración con el mercado: `jugadorId`, `precio`, `fecha`, `estrategia` y `version`.
- **Recálculo**: ejecución transaccional de una o varias semanas, con estado, contadores, estrategias calculadas, inicio, fin y `correlationId`.

## Non-Functional Requirements

- **NFR-001**: Los cálculos de fórmulas MUST tener pruebas unitarias exhaustivas y deterministas con métricas de prueba.
- **NFR-002**: Persistencia y endpoints MUST tener pruebas de integración contra PostgreSQL real mediante Testcontainers.
- **NFR-003**: Las pruebas de seguridad MUST simular `@WithMockUser(roles = "SUPERUSUARIO")` y `@WithMockUser(roles = "USUARIO")`; no se crea el superusuario en esta feature.
- **NFR-004**: Las migraciones y el recálculo MUST conservar la trazabilidad de versión y pesos aun cuando se creen configuraciones nuevas.
- **NFR-005**: La configuración del cron, `k`, tope, piso y parámetros de estrategias MUST ser explícita y validable.
- **NFR-006**: Los logs MUST ser estructurados, incluir `correlationId` en scheduler y recálculo y no ocultar errores con defaults silenciosos.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Para cada jugador procesado, un recálculo completo genera exactamente una cotización por estrategia y semana, sin duplicados.
- **SC-002**: El 100 % de las cotizaciones persistidas conserva el código, versión y snapshot de pesos de la estrategia que las produjo.
- **SC-003**: El 100 % de los scores calculados queda en `[0,1]`, incluso con un único jugador, valores iguales, métricas faltantes o ausencia de métricas.
- **SC-004**: Una segunda ejecución del mismo recálculo reemplaza los registros de esas semanas y deja la misma cantidad de filas.
- **SC-005**: Un usuario sin rol de superusuario nunca puede crear versiones, activar estrategias ni recalcular mediante los endpoints protegidos.
- **SC-006**: Un recálculo exitoso invalida la cotización vigente cacheada y la siguiente lectura refleja la serie de la estrategia activa.
- **SC-007**: El scheduler y el endpoint manual producen el mismo resultado para la misma fecha, datos y configuración.
- **SC-008**: El backfill de un rango válido produce una cotización por jugador, estrategia y semana del rango inclusive.

## Assumptions

- Las entidades `Jugador`, sus posiciones y ligas existen por features anteriores y sus identificadores son la referencia de `MetricaPartido` y `Cotizacion`.
- Las fechas de cotización representan el cierre semanal y se calculan en la zona horaria configurada por la aplicación.
- `Clock` se inyecta en todo Service que dependa de fecha/hora; los tests usan un `Clock` fijo para reproducibilidad.
- El rol `SUPERUSUARIO` ya es emitido por JWT en la feature 007; esta feature solo aplica la autorización definida en el contrato.
- La interfaz de actualización de métricas será implementada por la feature 011; mientras no exista una implementación operativa, el scheduler no inventa métricas ni modifica cotizaciones.
- La compra y venta del mercado consumirán `CotizacionVigentePort` desde la feature 007 y no se duplicará lógica de cotización en esos services.
- Flyway V10 a V14 se ejecuta en orden sobre el esquema de las features previas.
- Las cinco ligas del dominio participan en la normalización; las métricas de prueba pueden representar cualquiera de ellas.
