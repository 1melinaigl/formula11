# Feature Specification: Ranking de jugadores

**Feature Branch**: `003-004-cotizaciones-ranking` (apilada sobre 003, que aporta la ruta de detalle y los helpers de cotización)
**Created**: 2026-10-24
**Status**: Draft
**Input**: Plan de desarrollo Entrega 2, track C: "004-ranking". Documento de visión: `GET /players/ranking` — "Ranking de jugadores según estrategia activa".

## Clarifications

### Session 2026-10-24

- Q: ¿El ranking de esta feature debe mostrar únicamente la estrategia activa y no ofrecer selector ni enviar `estrategia`, incluso para `SUPERUSUARIO`? → A: Solo estrategia activa; el frontend no envía `estrategia`.
- Q: ¿La paginación del ranking debe usar índices de página basados en cero (`page=0` para la primera página), como el contrato y el código actual? → A: Índice cero en la API; la interfaz muestra las páginas desde 1.
- Q: Cuando el backend devuelve `estrategia`, `version` o `fecha` en `null` porque todavía no hubo recálculo, ¿la pantalla debe mostrar un título genérico y mantener la tabla o el estado vacío sin metadatos? → → A: Mostrar “Estrategia activa: <nombre>· sin recálculo todavía” (`estrategia` siempre viene; `version` y `fecha` son null hasta el primer recálculo) y el estado correspondiente.
- Q: ¿Los errores `400`, `401` y `403` del endpoint de ranking deben mostrarse con el `mensaje` y `correlationId` del contrato, limpiando la sesión solo ante `401` y sin cambiar la pantalla ante `400`/`403`? → A: Mostrar `mensaje` y `correlationId`; limpiar la sesión solo ante `401`.
- Q: ¿Los filtros deben enviar al backend los códigos contractuales de posición (`POR`, `DEF`, `MED`, `DEL`) y el nombre legible exacto de la liga, sin traducir ni filtrar localmente? → A: Enviar códigos de posición y nombre de liga al backend.

## User Scenarios & Testing

### User Story 1 - Ver el ranking (Priority: P1)

Como usuario autenticado, quiero ver a los jugadores ordenados según la estrategia activa del mercado para detectar a los que mejor rinden.

**Independent Test**: Con MSW, `GET /api/jugadores/ranking` devuelve una lista ordenada; `/ranking` la muestra en ese orden con número de puesto, posición, jugador, equipo, precio, variación semanal y score.

**Acceptance Scenarios**:

1. **Given** el menú principal con sesión, **When** el usuario elige "Ranking", **Then** navega a `/ranking` y el encabezado muestra "Ranking".
2. **Given** una respuesta del backend, **When** se muestra la tabla, **Then** se respeta el orden y el puesto recibidos; la API usa `page=0` para la primera página, la interfaz muestra las páginas desde 1 y, si hay más de una página (20 por página), aparece la paginación.
3. **Given** la página con `estrategia`, `version` y `fecha`, **When** se muestra el título, **Then** se indica "Estrategia activa: <nombre> v<versión> · al dd/mm/aaaa"; si todavía no hubo recálculo (`version` y `fecha` en `null`), se indica "Estrategia activa: <nombre> · sin recálculo todavía".
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
- **FR-005**: Los errores `400`, `401` y `403` muestran el `mensaje` y `correlationId` del esquema `Error`; solo `401` limpia la sesión y `400`/`403` mantienen la pantalla.
- **FR-006**: Los filtros envían al backend los códigos `POR`, `DEF`, `MED` o `DEL` en `posicion` y el nombre legible exacto de la liga en `liga`; el frontend no filtra localmente.

## Contrato (`backend/specs/005-contratos-entrega2/contracts/openapi.yaml`)

- `GET /api/jugadores/ranking?estrategia=&liga=&posicion=&page=&size=` → `RankingPageResponse`: página (`content`, `page`, `size`, `totalElements`, `totalPages`) con `estrategia`, `version` y `fecha` de la última cotización (null si no hubo recálculo).
- `RankingItem`: `{ puesto, jugador: JugadorResponse, precio, variacion, score }`; `variacion` en porcentaje y null en la primera semana.
- Ordena el backend por score; el filtro de posición (`POR`, `DEF`, `MED`, `DEL`) y el de liga también son del backend, y el puesto lo manda el backend.
- `estrategia` puede ser solicitada por un `SUPERUSUARIO` según el contrato general, pero queda fuera de esta feature: el frontend muestra únicamente la estrategia activa y no envía el parámetro `estrategia`.
- La API usa índices de página basados en cero; la interfaz presenta el número de página incrementado en uno.
