# Feature Specification: Mercado — compra y venta de tokens

**Feature Branch**: `005-mercado-compra-venta` (apilada sobre `003-004-cotizaciones-ranking`)
**Created**: 2026-10-24
**Status**: Draft
**Input**: Plan de desarrollo Entrega 2, track C: "005-mercado-compra-venta". Documento de visión §3.3 y endpoints `POST /orders/buy`, `POST /orders/sell`, `GET /users/:id/portfolio`.

## Clarifications

### Session 2026-10-24

- Q: ¿Qué roles deben poder comprar y vender tokens desde el mercado? → A: Solo `USUARIO` puede comprar y vender; `SUPERUSUARIO` no ve controles de operación o recibe acceso denegado.
- Q: ¿Qué identificador debe usar el frontend al consultar el portfolio del comprador? → A: Construir la ruta exclusivamente con `usuario.id` de la sesión; no permitir otro identificador.
- Q: ¿Se deben permitir compras y ventas cuando `fechaCotizacion` es `null` y el backend informa el precio inicial de 1 crédito? → A: Permitir compra y venta al precio inicial de 1 crédito; mostrar la marca “precio inicial”.
- Q: ¿Qué debe hacer la pantalla si la orden devuelve `201` pero falla la actualización posterior del portfolio o de los datos de mercado? → A: Confirmar la operación, mostrar el error de actualización y permitir reintentar solo las consultas de datos, sin reenviar la orden.

## User Scenarios & Testing

### User Story 1 - Comprar tokens (Priority: P1)

Como usuario autenticado, quiero comprar tokens de un jugador desde su detalle usando la cotización vigente.

**Independent Test**: Con MSW, el detalle muestra saldo y tenencia; al comprar 5 tokens se envía `POST /api/ordenes/compra` con `{ jugadorId, cantidad }`, se confirma la operación y se vuelve a consultar el portfolio.

**Acceptance Scenarios**:

1. **Given** el detalle de un jugador, **Then** el panel "Operar" muestra saldo, tokens del usuario (con precio promedio), tokens disponibles de los emitidos, precio vigente con su fecha y total estimado.
2. **Given** una cantidad válida, **When** el usuario confirma, **Then** se envía la orden con Bearer y `X-Correlation-Id`, se muestra "Compraste N tokens a P créditos. Total: T créditos. Saldo: S créditos." y se vuelven a pedir portfolio y datos de mercado (saldo, tenencia y disponibilidad).
3. **Given** una orden en curso, **Then** el botón queda deshabilitado ("Enviando...") para evitar doble envío.
4. **Given** un `SUPERUSUARIO`, **When** abre el detalle o el mercado, **Then** no puede iniciar una compra ni una venta y recibe el estado de acceso denegado definido por la aplicación.

### User Story 2 - Vender tokens (Priority: P1)

1. **Given** tokens del jugador, **When** el usuario elige "Vender" y confirma, **Then** se envía `POST /api/ordenes/venta` y se muestra "Vendiste ...".
2. **Given** `?operacion=venta` en la URL, **Then** el panel abre con "Vender" seleccionado (lo usa la compra en un click del 008 con `?operacion=compra`).

### User Story 3 - Validaciones y errores (Priority: P1)

1. Cantidad vacía, no entera o menor a 1: mensaje y no se envía.
2. Compra con total mayor al saldo: "Saldo insuficiente: necesitás X créditos."
3. Compra mayor a `tokensDisponibles`: "Solo quedan N tokens disponibles." Más de 100 tokens por orden: "Podés operar hasta 100 tokens por orden."
4. Venta mayor a la tenencia: "Solo tenés N tokens para vender." / "No tenés tokens de este jugador para vender."
5. Error del backend (p. ej. 409): se muestra `mensaje` y `correlationId`; no se muestra confirmación.
6. Si no se pudo obtener el precio de `/mercado`: se muestra el error y no el formulario. Sin recálculo, el precio se marca como "precio inicial".
7. Si la orden devuelve `201` pero falla una consulta de actualización, se conserva la confirmación de la operación, se muestra el error con su `correlationId` y el reintento solo vuelve a consultar los datos, sin reenviar la orden.

## Requirements

- **FR-001**: Las rutas viven solo en `src/api/endpoints.js` (`mercadoJugador`, `comprar`, `vender`, `portfolio`).
- **FR-002**: El precio mostrado es referencial; el backend fija el precio final y la respuesta es la fuente de la confirmación.
- **FR-003**: Las validaciones del cliente no reemplazan las del backend; sus errores se muestran tal cual.
- **FR-004**: El campo de cantidad tiene label, `aria-invalid`, `aria-describedby` al total y al error; la confirmación usa `role="status"`.
- **FR-005**: Solo un usuario con rol `USUARIO` puede iniciar compras o ventas desde el frontend; el `SUPERUSUARIO` no puede operar contra sí mismo.
- **FR-006**: La consulta del portfolio usa siempre el `usuario.id` de la sesión autenticada; el frontend no acepta ni permite editar otro `usuarioId`.
- **FR-007**: Cuando `fechaCotizacion` es `null`, el frontend permite comprar y vender al precio inicial de 1 crédito y muestra la marca "precio inicial".
- **FR-008**: Después de una respuesta `201`, un fallo al actualizar portfolio o mercado no invalida la confirmación ni puede provocar un reenvío automático de la orden; el reintento solo consulta nuevamente los datos.

## Contrato (`backend/specs/005-contratos-entrega2/contracts/openapi.yaml`)

- `GET /api/jugadores/{jugadorId}/mercado` → `MercadoJugadorResponse`: `{ jugadorId, tokensEmitidos, tokensDisponibles, precioVigente, fechaCotizacion }`; `fechaCotizacion` es null mientras no haya recálculo (precio inicial de 1 crédito). Es la fuente del precio y de la disponibilidad.
- `GET /api/usuarios/{usuarioId}/portfolio` → `PortfolioResponse`: `saldo` y `posiciones: [{ jugador, cantidad, precioPromedio, ... }]`; el frontend sustituye `{usuarioId}` exclusivamente por el `id` del usuario autenticado.
- `POST /api/ordenes/compra` y `/api/ordenes/venta` con `OrdenRequest { jugadorId, cantidad }` (cantidad de 1 a 100); el usuario sale del JWT. Respuesta `201` con `OperacionResponse { id, tipo, jugador, cantidad, precioUnitario, total, saldoPosterior, contraparte, fecha, estrategia, version }`.
- Errores: `409` si el superusuario no tiene tokens suficientes; `422` si el usuario no tiene saldo o tokens; `404` si el jugador no existe. Se muestra el `mensaje` del backend con su `correlationId`.
- El saldo inicial sigue a confirmar con el docente (lo define el backend al registrar).
