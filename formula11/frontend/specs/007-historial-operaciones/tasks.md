---

description: "Task list for Formula11 operations history"
---

# Tasks: Historial de operaciones

**Input**: `spec.md` y `plan.md` de `/specs/007-historial-operaciones/`.

**Tests**: Obligatorios; Vitest, React Testing Library, user-event y MSW.

**Scope**: Solo frontend, contra mocks de MSW. Depende de 005 y 006.

## Phase 1: Fundamentos

- [X] T001 Agregar `transacciones(usuarioId)` a `src/api/endpoints.js` y su handler por defecto en `src/test/handlers.js`.
- [X] T002 [P] Crear helpers en `src/components/historial/historial.js`.
- [X] T003 [P] Agregar la prop `etiqueta` a `PaginacionCatalogo.jsx`.
- [X] T004 [P] Tests unitarios de los helpers en `tests/components/historial.test.js`.

## Phase 2: User Story 1 - Ver mis operaciones

- [X] T005 Crear `src/hooks/useHistorial.js` con consulta paginada.
- [X] T006 Crear `TablaHistorial.jsx` con fecha, tipo, jugador, tokens, precio, efecto en el saldo y contraparte.
- [X] T007 Crear `src/pages/HistorialPage.jsx`, ruta `/operaciones` y breadcrumb.
- [X] T008 Links "Historial" en el portfolio y "Ver historial" en la confirmación de una operación.
- [X] T009 Tests de la tabla, la consulta enviada, la lista sin paginar y la navegación portfolio ↔ historial.

## Phase 3: User Story 2 - Filtrar por tipo

- [X] T010 Filtros Todas/Compras/Ventas con `aria-pressed`, consulta al backend y vuelta a la página 1.
- [X] T011 Test de filtros, paginación y que repetir el filtro activo no vuelve a consultar.

## Phase 4: User Story 3 - Estados

- [X] T012 Carga, vacío según filtro, error con `correlationId` y sin sesión, con tests.

## Phase 5: Cierre

- [X] T013 Estilos responsive; verificado en el navegador (API simulada) sin desborde a 375px.
- [X] T014 `npm test`, `npm run lint` y `npm run build` en verde.
- [ ] T015 Alinear ruta, parámetros (`tipo`, `sort`) y forma de la transacción con el contrato `005-contratos-entrega2`.
