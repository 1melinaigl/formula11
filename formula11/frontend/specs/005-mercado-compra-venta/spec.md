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

1. **Given** el detalle de un jugador con cotización, **Then** el panel "Operar" muestra saldo, tokens del usuario (con precio promedio si existe), precio vigente y total estimado.
2. **Given** una cantidad válida, **When** el usuario confirma, **Then** se envía la orden con Bearer y `X-Correlation-Id`, se muestra "Compraste N tokens a P créditos. Total: T créditos." y se actualizan saldo y tenencia.
3. **Given** una orden en curso, **Then** el botón queda deshabilitado ("Enviando...") para evitar doble envío.

### User Story 2 - Vender tokens (Priority: P1)

1. **Given** tokens del jugador, **When** el usuario elige "Vender" y confirma, **Then** se envía `POST /api/ordenes/venta` y se muestra "Vendiste ...".
2. **Given** `?operacion=venta` en la URL, **Then** el panel abre con "Vender" seleccionado (lo usa la compra en un click del 008 con `?operacion=compra`).

### User Story 3 - Validaciones y errores (Priority: P1)

1. Cantidad vacía, no entera o menor a 1: mensaje y no se envía.
2. Compra con total mayor al saldo: "Saldo insuficiente: necesitás X créditos."
3. Compra mayor a `tokensDisponibles` (si el backend lo informa): "Solo quedan N tokens disponibles."
4. Venta mayor a la tenencia: "Solo tenés N tokens para vender." / "No tenés tokens de este jugador para vender."
5. Error del backend (p. ej. 409): se muestra `mensaje` y `correlationId`; no se muestra confirmación.
6. Sin cotización vigente: no se muestra el formulario.

## Requirements

- **FR-001**: Las rutas viven solo en `src/api/endpoints.js` (`comprar`, `vender`, `portfolio`).
- **FR-002**: El precio mostrado es referencial; el backend fija el precio final y la respuesta es la fuente de la confirmación.
- **FR-003**: Las validaciones del cliente no reemplazan las del backend; sus errores se muestran tal cual.
- **FR-004**: El campo de cantidad tiene label, `aria-invalid`, `aria-describedby` al total y al error; la confirmación usa `role="status"`.

## Assumptions (a confirmar con `backend/specs/005-contratos-entrega2`)

- `POST /api/ordenes/compra` y `/api/ordenes/venta` con `{ jugadorId, cantidad }`; respuesta `{ id, tipo, cantidad, precio, total? }`.
- `GET /api/usuarios/{id}/portfolio` → `{ saldo, tenencias: [{ jugadorId | jugador: { id }, cantidad, precioPromedio }] }`.
- Precio vigente: `jugador.precioActual` o `jugador.cotizacionActual.precio` si el backend lo manda; si no, la última cotización de `RENDIMIENTO` (estrategia activa por defecto) o la última disponible.
- `jugador.tokensDisponibles` es opcional.
- Saldo inicial: pendiente de definir con el docente (pregunta abierta del plan).
