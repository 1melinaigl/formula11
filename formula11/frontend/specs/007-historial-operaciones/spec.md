# Feature Specification: Historial de operaciones

**Feature Branch**: `007-historial-operaciones` (apilada sobre `006-portfolio-graficos`)
**Created**: 2026-10-24
**Status**: Draft
**Input**: Plan de desarrollo Entrega 2, track C: "007-historial-operaciones". Documento de visión §3.4 (el portfolio incluye el historial de operaciones) y endpoint `GET /users/:id/transactions`.

## User Scenarios & Testing

### User Story 1 - Ver mis operaciones (Priority: P1)

Como usuario autenticado, quiero ver mis compras y ventas para saber qué hice y cómo cambió mi saldo.

**Acceptance Scenarios**:

1. **Given** el portfolio, **When** pulso "Historial", **Then** navego a `/operaciones`; el encabezado muestra "Historial de operaciones" y "Mi portfolio" vuelve al portfolio (también con `Escape`).
2. **Given** una compra o venta confirmada en el detalle, **Then** el mensaje de confirmación tiene un link "Ver historial".
3. **Given** operaciones, **Then** la tabla muestra, de la más reciente a la más antigua: fecha y hora, tipo (Compra/Venta como texto), jugador con link al detalle, tokens, precio unitario, movimiento en el saldo (compra en rojo con signo menos, venta en verde con signo más), saldo final y contraparte.
4. **Given** más de una página, **Then** aparece la paginación (10 por página) y se navega con Anterior/Siguiente.

### User Story 2 - Filtrar por tipo (Priority: P2)

1. **Given** el historial, **When** elijo Compras o Ventas, **Then** se consulta al backend con `tipo` y se vuelve a la página 1; "Todas" quita el filtro.
2. **Given** el filtro ya activo, **When** lo vuelvo a pulsar, **Then** no se consulta de nuevo.

### User Story 3 - Estados (Priority: P1)

Carga; vacío ("Sin operaciones", con mensaje según el filtro y links a ranking y catálogo); error con `mensaje` y `correlationId`; sin sesión (acceso denegado, sin consultar la API).

## Requirements

- **FR-001**: La ruta vive en `src/api/endpoints.js` (`transacciones`).
- **FR-002**: La consulta envía `page`, `size=10` y `tipo`/`jugadorId` solo si hay filtro.
- **FR-003**: La respuesta es siempre una página (`content`, `page`, `size`, `totalElements`, `totalPages`); tipos desconocidos se descartan.
- **FR-004**: Las fechas `YYYY-MM-DD` se muestran sin correrse por zona horaria; los instantes, en hora local de 24 h.
- **FR-005**: A 375px se ocultan precio, saldo final, contraparte y la insignia de posición, sin desborde horizontal.

## Contrato (`backend/specs/005-contratos-entrega2/contracts/openapi.yaml`)

- `GET /api/usuarios/{usuarioId}/transacciones?page=&size=&tipo=&jugadorId=` → `TransaccionesPageResponse` (página de `OperacionResponse`), de la más nueva a la más vieja. 403 si el id no es el del JWT.
- `OperacionResponse { id, tipo: COMPRA | VENTA, jugador, cantidad, precioUnitario, total, saldoPosterior, contraparte, fecha (date-time), estrategia, version }`.
- No hay parámetro de orden: lo fija el backend.
