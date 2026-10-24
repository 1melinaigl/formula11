# Feature Specification: Detalle de jugador con evolución de cotizaciones

**Feature Branch**: `003-detalle-cotizaciones`
**Created**: 2026-10-24
**Status**: Draft
**Input**: Plan de desarrollo Entrega 2, track C (Frontend): "003-detalle-cotizaciones (ficha + gráfico de evolución)". Endpoints del documento de visión: `GET /players/:id` y `GET /players/:id/quotes`.

## User Scenarios & Testing

### User Story 1 - Ver la ficha y la cotización vigente (Priority: P1)

Como usuario autenticado, quiero abrir el detalle de un jugador desde el catálogo y ver su ficha junto con su cotización vigente para decidir si me interesa operar con él.

**Independent Test**: Con MSW, `GET /api/jugadores/7` y `GET /api/jugadores/7/cotizaciones` devuelven datos; la pantalla `/jugadores/7` muestra la ficha y una tarjeta por estrategia con precio, variación semanal, fecha y score.

**Acceptance Scenarios**:

1. **Given** la ficha de un jugador en el catálogo, **When** el usuario pulsa "Ver cotizaciones", **Then** navega a `/jugadores/:id`.
2. **Given** cotizaciones de una o más estrategias, **When** se muestra el detalle, **Then** hay una tarjeta por estrategia con la última cotización, la variación semanal que informa el backend (verde si sube, roja si baja) y la fecha.
3. **Given** una estrategia con una sola cotización, **When** se muestra su tarjeta, **Then** indica "Sin cotización anterior" en lugar de una variación.
4. **Given** el detalle abierto, **When** el usuario pulsa "Volver al catálogo" o `Escape`, **Then** vuelve a `/catalogo`.

### User Story 2 - Ver la evolución en el tiempo (Priority: P1)

Como usuario, quiero ver un gráfico con la evolución del precio del jugador para entender su tendencia y comparar las estrategias de valuación.

**Independent Test**: Con cotizaciones de `RENDIMIENTO` y `CONSISTENCIA`, el gráfico muestra una línea por estrategia y el historial completo queda disponible como tabla.

**Acceptance Scenarios**:

1. **Given** cotizaciones de varias estrategias (solo el superusuario), **When** se dibuja el gráfico, **Then** hay una línea por estrategia sobre el mismo eje de fechas, con leyenda y tooltip en créditos.
2. **Given** el historial, **When** el usuario despliega "Ver historial completo", **Then** ve una tabla ordenada de la más reciente a la más antigua con fecha, estrategia y versión, precio y score.
3. **Given** fechas `YYYY-MM-DD`, **When** se muestran, **Then** se formatean `dd/mm/aaaa` sin correrse de día por zona horaria.

### User Story 3 - Estados de la pantalla (Priority: P1)

**Acceptance Scenarios**:

1. **Given** la carga en curso, **Then** se muestra "Cargando jugador...".
2. **Given** un jugador sin cotizaciones, **Then** se muestra la ficha y el estado vacío "Sin cotizaciones".
3. **Given** un error al obtener cotizaciones, **Then** la ficha sigue visible y el error se muestra con `mensaje` y `correlationId` en el panel de cotizaciones.
4. **Given** un error al obtener el jugador (p. ej. 404), **Then** se muestra el error y no se muestra el panel de cotizaciones.
5. **Given** un usuario sin sesión, **Then** se muestra acceso denegado y no se consulta la API.

## Requirements

- **FR-001**: Las rutas de la API se definen solo en `src/api/endpoints.js` para poder alinearlas con el contrato de la Entrega 2 sin tocar componentes.
- **FR-002**: Ambas consultas usan `solicitarProtegida` (Bearer, `X-Correlation-Id`, limpieza de sesión ante `401`).
- **FR-003**: La respuesta de cotizaciones se acepta como lista o como objeto paginado (`content`) y se descartan entradas sin fecha o con precio no numérico.
- **FR-004**: La librería de gráficos se carga de forma diferida para no agrandar el bundle inicial.

## Contrato (`backend/specs/005-contratos-entrega2/contracts/openapi.yaml`)

- `GET /api/jugadores/{jugadorId}` → `JugadorDetalleResponse`: datos del jugador más `cotizacionActual` (null si no hubo recálculo).
- `GET /api/jugadores/{jugadorId}/cotizaciones[?estrategia=]` → `{ jugadorId, estrategia, cotizaciones: Cotizacion[] }`, ordenadas por fecha ascendente, sin paginar.
- `Cotizacion`: `{ jugadorId, fecha, precio, score, variacion, estrategia, version }`; `variacion` es un porcentaje (12.5 = 12,5 %) y es null en la primera semana.
- Estrategias: `RENDIMIENTO_POSICION` (A) y `CONSISTENCIA` (B). El público ve solo la activa; pedir otra es exclusivo del `SUPERUSUARIO` (403 para el resto). Por eso la comparación de las dos curvas se muestra solo al superusuario (el `rol` llega en la respuesta del login).
- Fuera de alcance: cotización a una fecha (`/cotizaciones/vigente?fecha=`).
