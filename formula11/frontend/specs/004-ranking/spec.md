# Feature Specification: Ranking de jugadores

**Feature Branch**: `003-004-cotizaciones-ranking` (apilada sobre 003, que aporta la ruta de detalle y los helpers de cotización)
**Created**: 2026-10-24
**Status**: Draft
**Input**: Plan de desarrollo Entrega 2, track C: "004-ranking". Documento de visión: `GET /players/ranking` — "Ranking de jugadores según estrategia activa".

## User Scenarios & Testing

### User Story 1 - Ver el ranking (Priority: P1)

Como usuario autenticado, quiero ver a los jugadores ordenados según la estrategia activa del mercado para detectar a los que mejor rinden.

**Independent Test**: Con MSW, `GET /api/jugadores/ranking` devuelve una lista ordenada; `/ranking` la muestra en ese orden con número de puesto, posición, jugador, equipo, precio, variación semanal y score.

**Acceptance Scenarios**:

1. **Given** el menú principal con sesión, **When** el usuario elige "Ranking", **Then** navega a `/ranking` y el encabezado muestra "Ranking".
2. **Given** una respuesta del backend, **When** se muestra la tabla, **Then** se respeta el orden recibido (el frontend no reordena) y el puesto es la posición en la lista visible.
3. **Given** filas con `estrategia` y `fecha`, **When** se muestra el título, **Then** se indica "Estrategia activa: <nombre> · al dd/mm/aaaa".
4. **Given** una fila con `precioAnterior` o `variacion`, **Then** la variación se muestra en verde si sube, roja si baja y "—" si no hay dato; un score ausente se muestra como "—".
5. **Given** una fila, **When** el usuario pulsa el nombre, **Then** navega al detalle `/jugadores/:id` (feature 003).

### User Story 2 - Filtrar por posición (Priority: P2)

Como usuario, quiero ver el ranking de una sola posición, porque la estrategia A compara a cada jugador contra su posición.

**Acceptance Scenarios**:

1. **Given** el ranking cargado, **When** el usuario elige Portero, Defensor, Mediocampista o Delantero, **Then** se muestran solo esos jugadores, renumerados desde 1, sin una nueva consulta.
2. **Given** una posición sin jugadores, **Then** se muestra "Sin jugadores".
3. **Given** el filtro activo, **Then** el botón queda marcado (`aria-pressed`) y "Todos" vuelve a la lista completa.

### User Story 3 - Estados (Priority: P1)

1. Carga: "Cargando ranking...".
2. Lista vacía: "Ranking vacío".
3. Error: `mensaje` y `correlationId`.
4. Sin sesión: acceso denegado, sin consultar la API.

## Requirements

- **FR-001**: La ruta de la API vive solo en `src/api/endpoints.js` (`ranking`).
- **FR-002**: La consulta usa `solicitarProtegida`.
- **FR-003**: Se aceptan filas planas o con el jugador anidado (`{ jugador, precio, ... }`), en lista o paginadas (`content`).
- **FR-004**: En pantallas de hasta 760px se ocultan Equipo y Score para que la tabla no desborde.

## Assumptions (a confirmar con `backend/specs/005-contratos-entrega2`)

- Ruta `GET /api/jugadores/ranking`, siguiendo el prefijo actual del catálogo.
- Fila: `{ id, nombre, equipo, liga, posicion, precio, precioAnterior | variacion, score, estrategia, fecha }`.
- El backend devuelve el ranking ya ordenado por la estrategia activa; el filtro por posición es local. Si el contrato agrega `?posicion=` o paginación, se pasa al backend.
- Fuera de alcance: cotización a una fecha (`?fecha=`).
