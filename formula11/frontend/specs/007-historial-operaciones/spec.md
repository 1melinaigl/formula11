# Feature Specification: Historial de operaciones

**Feature Branch**: `007-historial-operaciones` (apilada sobre `006-portfolio-graficos`)
**Created**: 2026-10-24
**Status**: Draft
**Input**: Plan de desarrollo Entrega 2, track C: "007-historial-operaciones". Documento de visión §3.4 (el portfolio incluye el historial de operaciones) y endpoint `GET /users/:id/transactions`.

## Clarifications

### Session 2026-10-24

- Q: ¿Qué ve el `SUPERUSUARIO` en el historial de operaciones? → A: No tiene historial propio: si entra a `/operaciones` por la URL ve un aviso y no se consulta la API.
- Q: ¿El historial ofrece filtrar por jugador? → A: No, queda fuera de alcance; el historial filtra solo por tipo.
- Q: ¿Cómo se muestra una operación hecha al precio inicial (`estrategia` null)? → A: Con la marca "(precio inicial)" junto al precio unitario; las demás no muestran estrategia.

## User Scenarios & Testing

### User Story 1 - Ver mis operaciones (Priority: P1)

Como usuario autenticado, quiero ver mis compras y ventas para saber qué hice y cómo cambió mi saldo.

**Acceptance Scenarios**:

1. **Given** el portfolio, **When** pulso "Historial", **Then** navego a `/operaciones`; el encabezado muestra "Historial de operaciones" y "Mi portfolio" vuelve al portfolio (también con `Escape`).
2. **Given** una compra o venta confirmada en el detalle, **Then** el mensaje de confirmación tiene un link "Ver historial".
3. **Given** operaciones, **Then** la tabla muestra, de la más reciente a la más antigua: fecha y hora, tipo (Compra/Venta como texto), jugador con link al detalle, tokens, precio unitario (con la marca "(precio inicial)" si la operación tiene `estrategia` en null), movimiento en el saldo (compra en rojo con signo menos, venta en verde con signo más), saldo final y contraparte.
4. **Given** más de una página, **Then** aparece la paginación (10 por página, "Página N de M") y se navega con Anterior/Siguiente conservando el filtro activo. Con una sola página no se muestra.
5. **Given** un `SUPERUSUARIO`, **When** entra a `/operaciones` por la URL, **Then** ve el aviso "El superusuario es la contraparte del mercado y no tiene operaciones propias." y no se consultan las transacciones (tampoco ve el acceso desde el portfolio, que no tiene; ver 006).
6. **Given** una página fuera de rango (la respuesta llega vacía con `totalElements` mayor a 0), **Then** se vuelve una sola vez a la última página válida, sin entrar en un bucle.

### User Story 2 - Filtrar por tipo (Priority: P2)

1. **Given** el historial, **When** elijo Compras o Ventas, **Then** se consulta al backend con `tipo` y se vuelve a la página 1; "Todas" quita el filtro.
2. **Given** el filtro ya activo, **When** lo vuelvo a pulsar, **Then** no se consulta de nuevo.
3. **Given** una consulta pendiente, **When** cambio el filtro o la página, **Then** la respuesta anterior no reemplaza a la vigente.

### User Story 3 - Estados (Priority: P1)

- Carga: "Cargando operaciones..." al entrar, al cambiar de filtro y al cambiar de página.
- Vacío: título "Sin operaciones", con "Todavía no hiciste operaciones." (Todas), "Todavía no hiciste compras." (Compras) o "Todavía no hiciste ventas." (Ventas), y links a ranking y catálogo.
- Error: `mensaje` y `correlationId`. Un `401` limpia la sesión y muestra el acceso denegado; un `403` se muestra como cualquier error.
- Sin sesión: acceso denegado, sin consultar la API.

## Requirements

- **FR-001**: La ruta vive en `src/api/endpoints.js` (`transacciones`).
- **FR-002**: La consulta envía `page`, `size=10` y `tipo` solo si hay filtro. El filtro por jugador (`jugadorId`) queda fuera de alcance: la pantalla no lo ofrece ni lo envía.
- **FR-003**: La respuesta es siempre una página (`content`, `page`, `size`, `totalElements`, `totalPages`); las operaciones con un `tipo` distinto de `COMPRA` o `VENTA` se descartan sin mostrarse. El orden lo fija el backend (de la más nueva a la más vieja) y el front no lo cambia.
- **FR-004**: Las fechas de las operaciones son `date-time` y se muestran en la zona horaria del navegador como "dd/mm/aaaa, HH:mm" (24 h). Si llegara una fecha sin hora (`YYYY-MM-DD`) se muestra como "dd/mm/aaaa" sin correrse de día; si falta, "—"; si no se puede interpretar, el texto recibido.
- **FR-005**: Hasta 760px se ocultan precio (con su marca de precio inicial), saldo final, contraparte y la insignia de posición; a 375px el ancho del documento no supera el de la ventana. La marca de precio inicial no se ve en celular: es una limitación aceptada de la aclaración elegida.
- **FR-006**: El historial es solo para el rol `USUARIO` (una sesión sin `rol` cuenta como `USUARIO`); para el `SUPERUSUARIO` no se consulta la API.
- **FR-007**: El movimiento en el saldo es `-total` para una compra y `+total` para una venta, con el `total` del backend; se muestra con signo y color, y el tipo se muestra como texto ("Compra"/"Venta"). Montos con dos decimales y coma decimal (`es-AR`). La contraparte es el texto que manda el backend (por ejemplo "superusuario"); si falta, "—".
- **FR-008**: La tabla tiene un `caption` ("Historial de operaciones") y encabezados de columna; los filtros son un grupo con nombre ("Filtrar por tipo de operación") cuyos botones exponen `aria-pressed`.
- **FR-009**: La ruta usa solo el `usuario.id` de la sesión; el JWT no se muestra en la UI ni en los errores.

## Contrato (`backend/specs/005-contratos-entrega2/contracts/openapi.yaml`)

- `GET /api/usuarios/{usuarioId}/transacciones?page=&size=&tipo=&jugadorId=` → `TransaccionesPageResponse` (página de `OperacionResponse`), de la más nueva a la más vieja. 403 si el id no es el del JWT.
- `OperacionResponse { id, tipo: COMPRA | VENTA, jugador, cantidad, precioUnitario, total, saldoPosterior, contraparte, fecha (date-time), estrategia, version }`.
- No hay parámetro de orden: lo fija el backend.

## Dependencias

- El link "Ver historial" de la confirmación sale del panel de operación (005); el botón "Historial" está en el portfolio (006).
