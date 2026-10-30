# Feature Specification: Detalle de jugador con evolución de cotizaciones

**Feature Branch**: `003-detalle-cotizaciones`
**Created**: 2026-10-24
**Status**: Draft
**Input**: Plan de desarrollo Entrega 2, track C (Frontend): "003-detalle-cotizaciones (ficha + gráfico de evolución)". Endpoints del documento de visión: `GET /players/:id` y `GET /players/:id/quotes`.

## Clarifications

### Session 2026-10-24

- Q: ¿Qué debe mostrar el detalle de un jugador a un usuario autenticado con rol `USUARIO`? → A: Solo la estrategia activa.
- Q: ¿Debe el detalle consultar `GET /api/estrategias` para marcar cuál estrategia es la activa cuando lo visualiza un `SUPERUSUARIO`? → A: Sí, consultar y marcar la activa.

## User Scenarios & Testing

### User Story 1 - Ver la ficha y la cotización vigente (Priority: P1)

Como usuario autenticado, quiero abrir el detalle de un jugador desde el catálogo y ver su ficha junto con su cotización vigente para decidir si me interesa operar con él.

**Independent Test**: Con MSW, `GET /api/jugadores/7` y `GET /api/jugadores/7/cotizaciones` devuelven datos; la pantalla `/jugadores/7` muestra la ficha y una tarjeta para la estrategia activa con precio, variación semanal, fecha y score. El superusuario puede consultar y comparar la otra estrategia.

**Acceptance Scenarios**:

1. **Given** la ficha de un jugador en el catálogo, **When** el usuario pulsa "Ver cotizaciones", **Then** navega a `/jugadores/:id`.
2. **Given** un usuario autenticado, **When** se muestra el detalle, **Then** hay una tarjeta para la estrategia activa con la última cotización, la variación semanal que informa el backend (verde si sube, roja si baja) y la fecha. El superusuario puede ver una tarjeta adicional para la estrategia solicitada.
3. **Given** una estrategia con una sola cotización, **When** se muestra su tarjeta, **Then** indica "Sin cotización anterior" en lugar de una variación.
4. **Given** el detalle abierto, **When** el usuario pulsa "Volver al catálogo" o `Escape`, **Then** vuelve a `/catalogo`.
5. **Given** un `SUPERUSUARIO`, **When** se abre el detalle, **Then** se consultan la estrategia activa y la alternativa `RENDIMIENTO_POSICION`/`CONSISTENCIA` que no sea activa, y la tarjeta de la activa queda identificada.

### User Story 2 - Ver la evolución en el tiempo (Priority: P1)

Como usuario, quiero ver un gráfico con la evolución del precio del jugador para entender su tendencia y comparar las estrategias de valuación.

**Independent Test**: Con cotizaciones de `RENDIMIENTO` y `CONSISTENCIA`, el gráfico muestra una línea por estrategia y el historial completo queda disponible como tabla.

**Acceptance Scenarios**:

1. **Given** cotizaciones de `RENDIMIENTO_POSICION` y `CONSISTENCIA` (solo el superusuario), **When** se dibuja el gráfico, **Then** hay una línea por estrategia sobre el mismo eje de fechas, con leyenda y tooltip en créditos, y la estrategia activa está marcada según `GET /api/estrategias`.
2. **Given** el historial, **When** el usuario despliega "Ver historial completo", **Then** ve una tabla ordenada de la más reciente a la más antigua con fecha, estrategia y versión, precio y score.
3. **Given** fechas `YYYY-MM-DD`, **When** se muestran, **Then** se formatean `dd/mm/aaaa` sin correrse de día por zona horaria.
4. **Given** el usuario es `USUARIO`, **When** se muestra la evolución, **Then** solo se presenta la serie de la estrategia activa y no se solicita la estrategia alternativa.

### User Story 3 - Estados de la pantalla (Priority: P1)

**Acceptance Scenarios**:

1. **Given** la carga en curso, **Then** se muestra "Cargando jugador...".
2. **Given** un jugador sin cotizaciones, **Then** se muestra la ficha y el estado vacío "Sin cotizaciones".
3. **Given** un error al obtener cotizaciones, **Then** la ficha sigue visible y el error se muestra con `mensaje` y `correlationId` en el panel de cotizaciones.
4. **Given** un error al obtener el jugador (p. ej. 404), **Then** se muestra el error y no se muestra el panel de cotizaciones.
5. **Given** un usuario sin sesión, **Then** se muestra acceso denegado y no se consulta la API.
6. **Given** un `USUARIO` solicita una estrategia distinta de la activa, **Then** el backend responde `403`, no se muestra esa estrategia y el error conserva `mensaje` y `correlationId`.
7. **Given** falla una consulta de la estrategia alternativa para un `SUPERUSUARIO`, **Then** la ficha y la estrategia activa permanecen visibles y el panel informa el error de la consulta fallida.
8. **Given** `GET /api/estrategias` falla o devuelve cero o más de una estrategia con `activa: true`, **Then** no se elige una activa por defecto, se informa un error trazable y la comparación no se presenta como válida.
9. **Given** la estrategia alternativa no tiene cotizaciones, **Then** la estrategia activa y la ficha permanecen visibles y la alternativa muestra "Sin cotizaciones", sin inventar una variación o un precio.

## Requirements

- **FR-001**: Las rutas de la API se definen solo en `src/api/endpoints.js` para poder alinearlas con el contrato de la Entrega 2 sin tocar componentes.
- **FR-002**: Ambas consultas usan `solicitarProtegida` (Bearer, `X-Correlation-Id`, limpieza de sesión ante `401`).
- **FR-003**: La respuesta de cotizaciones usa la forma canónica `{ jugadorId, estrategia, cotizaciones }`, no paginada y ordenada por fecha ascendente. Se descartan entradas sin fecha o sin precio numérico.
- **FR-004**: La librería de gráficos se carga de forma diferida: el bundle inicial no debe contener módulos de Recharts y el gráfico debe emitirse en un chunk separado verificable en el build.
- **FR-005**: Para un `SUPERUSUARIO`, el detalle consulta `GET /api/estrategias` y marca la única estrategia cuya propiedad `activa` es verdadera; para un `USUARIO`, solo consulta y muestra la estrategia activa del endpoint de cotizaciones. Si la respuesta no identifica exactamente una activa, se informa un error trazable y no se elige una estrategia por defecto.
- **FR-006**: Las consultas de cotizaciones para un `SUPERUSUARIO` usan los códigos `RENDIMIENTO_POSICION` y `CONSISTENCIA`; la estrategia alternativa es la de código distinto de la activa. Un `USUARIO` nunca solicita la alternativa.
- **FR-008**: La tabla de historial es la alternativa textual del gráfico, debe ser accesible por teclado, mantener foco visible y mostrar fecha, estrategia, versión, precio y score. El retorno al catálogo debe ser operable por botón y `Escape`.

## Contrato (`backend/specs/005-contratos-entrega2/contracts/openapi.yaml`)

- `GET /api/jugadores/{jugadorId}` → `JugadorDetalleResponse`: datos del jugador más `cotizacionActual` (null si no hubo recálculo).
- `GET /api/jugadores/{jugadorId}/cotizaciones[?estrategia=]` → `{ jugadorId, estrategia, cotizaciones: Cotizacion[] }`, ordenadas por fecha ascendente, sin paginar.
- `JugadorDetalleResponse` contiene como mínimo `id`, `nombre`, `equipo`, `liga`, `posicion` y `cotizacionActual`; `cotizacionActual` es null si no hubo recálculo.
- `Cotizacion`: `{ jugadorId, fecha, precio, score, variacion, estrategia, version }`; `fecha` es `AAAA-MM-DD`, `precio` es un número en créditos con hasta cuatro decimales, `variacion` es un porcentaje (12.5 = 12,5 %) y es null únicamente en la primera semana de una serie.
- Las fechas `date-time` de operaciones del contrato no participan en esta feature; solo se consumen fechas de cotización `AAAA-MM-DD`.
- Estrategias: `RENDIMIENTO_POSICION` (A) y `CONSISTENCIA` (B). El público ve solo la activa; pedir otra es exclusivo del `SUPERUSUARIO` (403 para el resto). Por eso la comparación de las dos curvas se muestra solo al superusuario (el `rol` llega en la respuesta del login).
- `GET /api/estrategias` devuelve un array con las dos estrategias, cada una con `codigo`, `nombre`, `version`, `activa`, `pesos`, `parametros` y `creadaEn`; el detalle del `SUPERUSUARIO` usa la única entrada con `activa: true` para marcar la estrategia activa junto con la comparación. Si falla la consulta o no hay exactamente una activa, la UI informa el error y no presenta una selección implícita.
- Todos los endpoints protegidos requieren JWT y envían `X-Correlation-Id`; los errores usan `mensaje`, `timestamp` y `correlationId`.
- La documentación de visión que usa rutas `/players/:id` y `/players/:id/quotes` es descriptiva; para esta feature prevalece el contrato de Entrega 2 con rutas `/api/jugadores/...`.
- Fuera de alcance: cotización a una fecha (`/cotizaciones/vigente?fecha=`).
