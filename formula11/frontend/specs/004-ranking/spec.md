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
2. **Given** una respuesta del backend, **When** se muestra la tabla, **Then** se respeta el orden y el puesto recibidos; si hay más de una página (20 por página), aparece la paginación.
3. **Given** la página con `estrategia`, `version` y `fecha`, **When** se muestra el título, **Then** se indica "Estrategia activa: <nombre> v<versión> · al dd/mm/aaaa".
4. **Given** una fila con `variacion`, **Then** la variación se muestra en verde si sube, roja si baja y "—" si no hay dato; un score ausente se muestra como "—".
5. **Given** una fila, **When** el usuario pulsa el nombre, **Then** navega al detalle `/jugadores/:id` (feature 003).

### User Story 2 - Filtrar por posición (Priority: P2)

Como usuario, quiero ver el ranking de una sola posición, porque la estrategia A compara a cada jugador contra su posición.

**Acceptance Scenarios**:

1. **Given** el ranking cargado, **When** el usuario elige Portero, Defensor, Mediocampista o Delantero o una liga, **Then** se consulta al backend con `posicion`/`liga`, se vuelve a la página 1 y se muestra el puesto que manda el backend.
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
- **FR-003**: Las filas llegan con el jugador anidado (`{ puesto, jugador, precio, variacion, score }`) dentro de una página.
- **FR-004**: En pantallas de hasta 760px se ocultan Equipo y Score para que la tabla no desborde.

## Contrato (`backend/specs/005-contratos-entrega2/contracts/openapi.yaml`)

- `GET /api/jugadores/ranking?estrategia=&liga=&posicion=&page=&size=` → `RankingPageResponse`: página (`content`, `page`, `size`, `totalElements`, `totalPages`) con `estrategia`, `version` y `fecha` de la última cotización (null si no hubo recálculo).
- `RankingItem`: `{ puesto, jugador: JugadorResponse, precio, variacion, score }`; `variacion` en porcentaje y null en la primera semana.
- Ordena el backend por score; el filtro de posición (`POR`, `DEF`, `MED`, `DEL`) y el de liga también son del backend, y el puesto lo manda el backend.
- `estrategia` solo la puede pedir el superusuario; el front no la envía.
