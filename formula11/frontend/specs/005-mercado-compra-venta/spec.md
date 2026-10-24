# Feature Specification: Mercado — compra y venta de tokens

**Feature Branch**: `005-mercado-compra-venta` (apilada sobre `003-004-cotizaciones-ranking`)
**Created**: 2026-10-24
**Status**: Draft
**Input**: Plan de desarrollo Entrega 2, track C: "005-mercado-compra-venta". Documento de visión §3.3 y endpoints `POST /orders/buy`, `POST /orders/sell`, `GET /users/:id/portfolio`.

## User Scenarios & Testing

### User Story 1 - Comprar tokens (Priority: P1)

Como usuario autenticado, quiero comprar tokens de un jugador desde su detalle usando la cotización vigente.

**Independent Test**: Con MSW, el detalle muestra saldo y tenencia; al comprar 5 tokens se envía `POST /api/ordenes/compra` con `{ jugadorId, cantidad }`, se confirma la operación y se vuelve a consultar el portfolio.

**Acceptance Scenarios**:

1. **Given** el detalle de un jugador, **Then** el panel "Operar" muestra saldo, tokens del usuario (con precio promedio), tokens disponibles de los emitidos, precio vigente con su fecha y total estimado.
2. **Given** una cantidad válida, **When** el usuario confirma, **Then** se envía la orden con Bearer y `X-Correlation-Id`, se muestra "Compraste N tokens a P créditos. Total: T créditos. Saldo: S créditos." y se vuelven a pedir portfolio y datos de mercado (saldo, tenencia y disponibilidad).
3. **Given** una orden en curso, **Then** el botón queda deshabilitado ("Enviando...") para evitar doble envío.

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

## Requirements

- **FR-001**: Las rutas viven solo en `src/api/endpoints.js` (`mercadoJugador`, `comprar`, `vender`, `portfolio`).
- **FR-002**: El precio mostrado es referencial; el backend fija el precio final y la respuesta es la fuente de la confirmación.
- **FR-003**: Las validaciones del cliente no reemplazan las del backend; sus errores se muestran tal cual.
- **FR-004**: El campo de cantidad tiene label, `aria-invalid`, `aria-describedby` al total y al error; la confirmación usa `role="status"`.

## Contrato (`backend/specs/005-contratos-entrega2/contracts/openapi.yaml`)

- `GET /api/jugadores/{jugadorId}/mercado` → `MercadoJugadorResponse`: `{ jugadorId, tokensEmitidos, tokensDisponibles, precioVigente, fechaCotizacion }`; `fechaCotizacion` es null mientras no haya recálculo (precio inicial de 1 crédito). Es la fuente del precio y de la disponibilidad.
- `GET /api/usuarios/{usuarioId}/portfolio` → `PortfolioResponse`: `saldo` y `posiciones: [{ jugador, cantidad, precioPromedio, ... }]`.
- `POST /api/ordenes/compra` y `/api/ordenes/venta` con `OrdenRequest { jugadorId, cantidad }` (cantidad de 1 a 100); el usuario sale del JWT. Respuesta `201` con `OperacionResponse { id, tipo, jugador, cantidad, precioUnitario, total, saldoPosterior, contraparte, fecha, estrategia, version }`.
- Errores: `409` si el superusuario no tiene tokens suficientes; `422` si el usuario no tiene saldo o tokens; `404` si el jugador no existe. Se muestra el `mensaje` del backend con su `correlationId`.
- El saldo inicial sigue a confirmar con el docente (lo define el backend al registrar).
