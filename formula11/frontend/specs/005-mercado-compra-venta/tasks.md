---

description: "Task list for Formula11 token market (buy/sell)"
---

# Tasks: Mercado — compra y venta de tokens

**Input**: `spec.md` y `plan.md` de `/specs/005-mercado-compra-venta/`.

**Tests**: Obligatorios; Vitest, React Testing Library, user-event y MSW.

**Scope**: Solo frontend, contra mocks de MSW. Depende de 003 y 004.

## Phase 1: Fundamentos

- [X] T001 Agregar `comprar`, `vender` y `portfolio(usuarioId)` a `src/api/endpoints.js`.
- [X] T002 [P] Crear helpers en `src/components/mercado/mercado.js`.
- [X] T003 [P] Handlers por defecto de portfolio y órdenes en `src/test/handlers.js`.
- [X] T004 [P] Tests unitarios de los helpers en `tests/components/mercado.test.js`.

## Phase 2: User Story 1 - Comprar

- [X] T005 Crear `src/hooks/useMercado.js` (portfolio, `operar`, recarga tras operar).
- [X] T006 Crear `PanelOperacion.jsx` con saldo, tenencia, precio vigente, total estimado y confirmación.
- [X] T007 Integrar el panel en la columna lateral de `DetalleJugadorPage.jsx`.
- [X] T008 Tests de compra, actualización de saldo/tenencia y botón deshabilitado durante el envío.

## Phase 3: User Story 2 - Vender

- [X] T009 Selector Comprar/Vender con `aria-pressed` y `?operacion=` en la URL.
- [X] T010 Tests de venta y preselección desde la URL.

## Phase 4: User Story 3 - Validaciones y errores

- [X] T011 Validaciones de cantidad, saldo, disponibilidad y tenencia.
- [X] T012 Errores del backend con `mensaje` y `correlationId`; sin formulario si no hay cotización.
- [X] T013 Tests de validaciones sin enviar órdenes, error 409 y falta de cotización.

## Phase 5: Cierre

- [X] T014 Estilos en `src/styles/screens.css`; verificado sin desborde a 375px y con una compra de punta a punta en el navegador (API simulada).
- [X] T015 `npm test`, `npm run lint` y `npm run build` en verde.
- [ ] T016 Alinear rutas y formas de request/response con el contrato `005-contratos-entrega2` cuando esté en `main`.
- [ ] T017 Confirmar con el backend cuál es el precio vigente (estrategia activa) y si el detalle expone `tokensDisponibles`.
