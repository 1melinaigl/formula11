# Feature Specification: Detector de jugadores subvaluados

**Feature Branch**: `010-senales-subvaluados`

**Created**: 2026-10-30

**Status**: Draft

**Input**: User description: "Feature extra: detector de jugadores subvaluados. Después de cada recálculo semanal se generan señales de jugadores cuyo precio está por debajo de un valor estimado; el frontend las muestra como alertas y permite comprar en un click; un panel mide si las señales acertaron."

**Related contract**: [Contrato de la Entrega 2](../005-contratos-entrega2/contracts/openapi.yaml). Esta feature implementa únicamente las operaciones con `x-feature: 010-feature-extra`: `GET /api/senales` y `GET /api/senales/aciertos`.

## Scope

Después de recalcular cada semana, el sistema busca jugadores subvaluados con dos criterios (`ENTRE_ESTRATEGIAS` y `PRECIO_ATRASADO`), guarda las señales y las expone para que el frontend las muestre como alertas. Además mide, semana a semana, si los jugadores señalados subieron más que el mercado.

No implementa:

- La compra en un click: el frontend usa `POST /api/ordenes/compra` de la `007`, que ya invoca el método reutilizable de `MercadoService` (constitución, punto 10). No hace falta un endpoint nuevo.
- El cálculo de cotizaciones (`006`), la carga de métricas (`011`) ni las lecturas de la `008`.
- Notificaciones push: el frontend consulta cada 30 segundos, como indica el contrato.
- Frontend.

## Clarifications

### Session 2026-10-30

- Q: ¿Cuándo se generan las señales? → A: Al final de cada semana procesada por `RecalculoService`, tanto en el recálculo manual como en el del scheduler. Si la generación falla, se registra en el log y el recálculo no falla: las señales son un extra.
- Q: ¿Con qué estrategia se generan? → A: Con la estrategia activa al momento del recálculo. La señal guarda el código de esa estrategia.
- Q: ¿Qué es `ENTRE_ESTRATEGIAS`? → A: El precio de la otra estrategia en la misma semana es al menos un `umbral` (15 % por defecto) más alto que el de la activa: el otro modelo valora más al jugador. `precio` es el de la activa, `precioEstimado` el de la otra y `brecha = (precioEstimado / precio - 1) * 100`.
- Q: ¿Qué es `PRECIO_ATRASADO`? → A: Jugadores de la misma posición con un score parecido esa semana (diferencia de hasta `0,05`) valen bastante más. `precioEstimado` es la mediana de los precios de esos pares, con un mínimo de 5 pares; si la brecha llega al umbral, el precio quedó atrasado respecto de su rendimiento (el tope semanal de ±20 % de la `006` hace que el precio tarde en alcanzar al score).
- Q: ¿Qué jugadores pueden recibir señales? → A: Solo los que jugaron esa semana: tienen al menos una métrica con minutos mayor que cero entre el lunes y el domingo. Los pares de `PRECIO_ATRASADO` también deben haber jugado. Así se evita alertar todas las semanas por diferencias viejas de jugadores que no juegan.
- Q: ¿Cuántas señales por semana? → A: Como máximo 20 por tipo (configurable), las de mayor brecha, para no inundar al usuario de alertas.
- Q: ¿Un recálculo de una semana ya procesada cambia los ids? → A: No. La señal es única por jugador, semana y tipo: si se recalcula, se actualiza la existente, se crean las nuevas y se borran las que ya no aplican. Así el frontend no vuelve a alertar algo que ya mostró.
- Q: ¿Qué son las señales "vigentes"? → A: Las de la última semana que tiene señales. Con `vigentes=false` se devuelven todas, de la más nueva a la más vieja.
- Q: ¿Cómo se ordenan? → A: Fecha descendente, brecha descendente e id ascendente.
- Q: ¿Cómo se miden los aciertos? → A: Para cada semana con señales que ya tiene cotización la semana siguiente en la misma estrategia: `variacionSenalados` es el promedio de la variación de los jugadores señalados, `variacionMercado` el promedio de todos los jugadores con cotización en las dos semanas, y una señal acierta si su jugador subió más que el mercado. `tasaAcierto = aciertos / señales evaluadas * 100` y `diferenciaPromedio` es el promedio de `variación del señalado - variación del mercado` sobre todas las señales evaluadas. Las semanas sin semana siguiente todavía no se informan.
- Q: ¿Qué devuelve el panel sin datos? → A: `semanas` vacía, `tasaAcierto: 0` y `diferenciaPromedio: 0`.
- Q: ¿Quién puede ver las señales? → A: Cualquier usuario autenticado, incluido el superusuario.

## User Scenarios & Testing

### User Story 1 - Detectar jugadores subvaluados después de cada recálculo (Priority: P1)

Como sistema quiero generar señales al terminar cada semana del recálculo para que el mercado tenga oportunidades detectadas automáticamente.

**Why this priority**: Sin señales no hay nada que mostrar ni medir.

**Independent Test**: Con cotizaciones y métricas sembradas de una semana, invocar la generación y verificar qué jugadores reciben cada tipo de señal, las brechas y los precios estimados.

**Acceptance Scenarios**:

1. **Given** un jugador que jugó y cuyo precio en la otra estrategia es 20 % más alto que en la activa, **When** se generan las señales de esa semana, **Then** recibe una señal `ENTRE_ESTRATEGIAS` con `brecha` 20, `precio` de la activa y `precioEstimado` de la otra.
2. **Given** un jugador que jugó y cinco pares de su posición con score parecido cuya mediana de precio es 25 % más alta, **When** se generan las señales, **Then** recibe una señal `PRECIO_ATRASADO` con esa mediana como `precioEstimado`.
3. **Given** un jugador con menos de 5 pares, una brecha menor al umbral o que no jugó esa semana, **When** se generan las señales, **Then** no recibe señal.
4. **Given** más señales de un tipo que el máximo configurado, **When** se generan, **Then** se guardan solo las de mayor brecha.
5. **Given** una semana ya generada, **When** se recalcula, **Then** las señales que siguen aplicando conservan su id, las nuevas se agregan y las que dejaron de aplicar se borran.
6. **Given** la generación falla, **When** termina el recálculo, **Then** el recálculo responde normalmente y el error queda en el log con `correlationId`.

---

### User Story 2 - Consultar las señales (Priority: P1)

Como usuario autenticado quiero ver las señales vigentes para decidir si compro, y el historial completo de señales.

**Why this priority**: Es lo que el frontend consulta cada 30 segundos para mostrar las alertas.

**Independent Test**: Sembrar señales de dos semanas y consultar `GET /api/senales` con y sin `vigentes`.

**Acceptance Scenarios**:

1. **Given** señales de dos semanas, **When** se llama `GET /api/senales`, **Then** recibe `200` con `SenalesPageResponse` y solo las de la última semana, ordenadas por brecha descendente.
2. **Given** `vigentes=false`, **When** se consulta, **Then** recibe las señales de todas las semanas, de la más nueva a la más vieja.
3. **Given** no hay señales, **When** se consulta, **Then** recibe una página vacía.
4. **Given** `page` o `size` inválidos, **When** se consulta, **Then** recibe `400`.
5. **Given** una solicitud sin JWT, **When** se consulta, **Then** recibe `401`.

---

### User Story 3 - Medir los aciertos (Priority: P2)

Como usuario autenticado quiero saber si las señales anteriores acertaron para confiar (o no) en las nuevas.

**Why this priority**: Le da credibilidad a la feature, pero las alertas funcionan sin el panel.

**Independent Test**: Sembrar cotizaciones de dos semanas y señales de la primera, y verificar variaciones, aciertos, tasa y diferencia.

**Acceptance Scenarios**:

1. **Given** señales de una semana y cotizaciones de la semana siguiente, **When** se llama `GET /api/senales/aciertos`, **Then** recibe `200` con esa semana, la cantidad de señales, las variaciones promedio y los aciertos.
2. **Given** señales de la última semana sin semana siguiente, **When** se consulta, **Then** esa semana no aparece.
3. **Given** no hay señales medibles, **When** se consulta, **Then** recibe `semanas` vacía, `tasaAcierto` 0 y `diferenciaPromedio` 0.
4. **Given** una solicitud sin JWT, **When** se consulta, **Then** recibe `401`.

## Edge Cases

- Sin estrategia activa, o sin cotizaciones de la activa en esa semana, no se generan señales.
- Si un jugador no tiene cotización en la otra estrategia, no recibe `ENTRE_ESTRATEGIAS`.
- Un jugador puede tener las dos señales en la misma semana; son dos señales distintas.
- Un jugador cuyo precio está en el piso de `0,10` puede dar brechas grandes; igual se informa, porque el piso también indica que el precio bajó mucho.
- La mediana de una cantidad par de pares es el promedio de los dos centrales, con 4 decimales.
- Las variaciones y porcentajes usan 2 decimales `HALF_UP`; los precios, 4.
- Si se borra o cambia la estrategia activa entre semanas, cada señal conserva la estrategia con la que se generó y el panel la mide con esa estrategia.

## Requirements

### Functional Requirements

- **FR-001**: Al terminar de procesar cada semana, `RecalculoService` MUST invocar la generación de señales de esa semana mediante un puerto (`GeneradorSenalesPort`). Un error en la generación MUST registrarse con `correlationId` y MUST NOT interrumpir el recálculo.
- **FR-002**: La generación MUST usar la estrategia activa y solo jugadores que jugaron esa semana (al menos una métrica con minutos mayor que cero entre el lunes y el domingo de la semana).
- **FR-003**: `ENTRE_ESTRATEGIAS` MUST generarse cuando el precio de la otra estrategia en la misma fecha es mayor que el de la activa y `brecha >= umbral`.
- **FR-004**: `PRECIO_ATRASADO` MUST generarse cuando existen al menos `minimoPares` jugadores que jugaron, de la misma posición, con `|score - scoreDelJugador| <= ventanaScore`, y la mediana de sus precios supera al precio del jugador con `brecha >= umbral`.
- **FR-005**: `brecha` MUST ser `(precioEstimado / precio - 1) * 100` con 2 decimales `HALF_UP`. Las reglas de detección MUST vivir en el modelo, sin Spring ni base de datos.
- **FR-006**: Por semana y tipo MUST guardarse como máximo `maximoPorTipo` señales, las de mayor brecha (desempate por id de jugador ascendente).
- **FR-007**: Cada señal MUST persistirse con jugador, fecha (domingo de la semana), tipo, estrategia, brecha, precio, precio estimado y fecha de creación, y MUST ser única por jugador, fecha y tipo. Regenerar una semana MUST actualizar las existentes, crear las nuevas y borrar las que ya no aplican, en una transacción por semana.
- **FR-008**: Los parámetros `umbral` (15), `ventanaScore` (0,05), `minimoPares` (5) y `maximoPorTipo` (20) MUST ser configurables en `app.senales` y validarse al arrancar.
- **FR-009**: `GET /api/senales` MUST aceptar `vigentes` (por defecto `true`), `page` (por defecto 0) y `size` (por defecto 20, entre 1 y 100) y devolver `SenalesPageResponse`. Con `vigentes=true` MUST incluir solo las señales de la fecha más reciente; con `false`, todas. El orden MUST ser fecha descendente, brecha descendente e id ascendente, paginando en la base.
- **FR-010**: `GET /api/senales/aciertos` MUST devolver `AciertosResponse` según la regla de medición de las Clarifications, con porcentajes de 2 decimales `HALF_UP`, semanas ordenadas por fecha ascendente y `0` en `tasaAcierto` y `diferenciaPromedio` cuando no haya señales medibles.
- **FR-011**: Los dos endpoints MUST requerir JWT y permitir cualquier rol. Los errores MUST usar el esquema `Error`.
- **FR-012**: La migración MUST ser `V19`, la última del rango de A, y crear solo la tabla `senal` con sus restricciones e índices.
- **FR-013**: Swagger y la colección de Postman MUST documentar los dos endpoints.

### Contract Surface

| Operación | Método y ruta | Autorización | Respuestas |
|---|---|---|---|
| Señales | `GET /api/senales` | Usuario autenticado | `200 SenalesPageResponse`, `400`, `401` |
| Aciertos | `GET /api/senales/aciertos` | Usuario autenticado | `200 AciertosResponse`, `401` |

El contrato no lista `400` en `GET /api/senales`; se usa el esquema `Error` existente para `page` o `size` inválidos, como en el resto de los listados paginados. No se modifica el contrato.

### Key Entities

- **Senal**: oportunidad detectada para un jugador en una semana, con tipo, estrategia, brecha, precio y precio estimado.
- **Cotizacion**, **MetricaPartido**, **Estrategia**, **Jugador**: entidades existentes que se leen.

## Non-Functional Requirements

- **NFR-001**: El detector y la medición de aciertos MUST tener tests unitarios sin Spring.
- **NFR-002**: La persistencia y la generación MUST tener tests de integración con Testcontainers.
- **NFR-003**: Los endpoints MUST tener tests E2E con MockMvc en el paquete `e2e`.
- **NFR-004**: La generación de una semana con todo el catálogo (unos 2.600 jugadores) MUST tardar menos de 5 segundos en la compu de la demo.
- **NFR-005**: No se modifican ni borran tests existentes.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Después de un recálculo completo de la temporada, cada semana tiene a lo sumo 40 señales (20 por tipo).
- **SC-002**: Recalcular dos veces la misma semana deja los mismos ids de señales cuando los datos no cambian.
- **SC-003**: El panel de aciertos coincide con un cálculo manual sobre los datos sembrados en los tests.
- **SC-004**: Un fallo en la generación de señales nunca hace fallar un recálculo.

## Assumptions

- La `006` guarda una cotización por jugador, estrategia y semana (con la corrección de versiones de `fix-006-versiones`) y la `011` carga las métricas con minutos.
- `Jugador.posicion` es `POR`, `DEF`, `MED` o `DEL`.
- Esta rama se crea después de mergear la corrección de la `006` y la `008` en `dev`.
- La compra en un click la resuelve el frontend con el endpoint existente de compra.
