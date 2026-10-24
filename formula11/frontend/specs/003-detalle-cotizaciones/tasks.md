---

description: "Task list for Formula11 player detail with quote history"
---

# Tasks: Detalle de jugador con evolución de cotizaciones

**Input**: `spec.md` y `plan.md` de `/specs/003-detalle-cotizaciones/`.

**Tests**: Obligatorios; Vitest, React Testing Library, user-event y MSW.

**Scope**: Solo frontend, contra mocks de MSW.

## Phase 1: Fundamentos

- [x] T001 Agregar `jugador(id)` y `cotizacionesJugador(id)` a `src/api/endpoints.js`.
- [x] T002 Agregar la dependencia `recharts`.
- [x] T003 [P] Crear helpers en `src/components/cotizaciones/cotizaciones.js` (normalizar, estrategias presentes, series por fecha, resumen con variación, formateo de fecha/precio/porcentaje).
- [x] T004 [P] Agregar handlers por defecto de `GET /api/jugadores/:id` y `GET /api/jugadores/:id/cotizaciones` en `src/test/handlers.js`.
- [x] T005 [P] Tests unitarios de los helpers en `tests/components/cotizaciones.test.js`.

## Phase 2: User Story 1 - Ficha y cotización vigente

- [x] T006 Crear `src/hooks/useDetalleJugador.js` con consultas protegidas en paralelo y errores separados.
- [x] T007 Crear `ResumenCotizacion.jsx` con una tarjeta por estrategia y variación semanal.
- [x] T008 Crear `src/pages/DetalleJugadorPage.jsx` y la ruta `/jugadores/:id` (carga diferida).
- [x] T009 Agregar el link "Ver cotizaciones" en `FichaJugador.jsx`.
- [x] T010 Tests de navegación catálogo → detalle → catálogo y de la cotización vigente.

## Phase 3: User Story 2 - Evolución

- [x] T011 Crear `GraficoEvolucion.jsx` con una línea por estrategia, leyenda y tooltip en créditos.
- [x] T012 Crear `TablaCotizaciones.jsx` como historial desplegable y alternativa accesible.
- [x] T013 Test del historial ordenado de más reciente a más antiguo.

## Phase 4: User Story 3 - Estados

- [x] T014 Estados de carga, vacío, error de cotizaciones con ficha visible, error de jugador y sin sesión.
- [x] T015 Tests de cada estado.

## Phase 5: Cierre

- [x] T016 Estilos responsive en `src/styles/screens.css`.
- [ ] T017 Alinear rutas y forma de la respuesta con `backend/specs/005-contratos-entrega2/contracts/openapi.yaml` cuando esté en `main`.
- [ ] T018 Marcar la estrategia activa del mercado cuando el contrato la exponga.
