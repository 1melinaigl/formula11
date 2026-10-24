---

description: "Task list for Formula11 token market (buy/sell)"
---

# Tasks: Mercado — compra y venta de tokens

**Input**: Design documents from `/specs/005-mercado-compra-venta/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/frontend-api.md

**Tests**: Requeridos por la constitución (principio V) y por los "Independent Test" del spec. Vitest + React Testing Library + MSW; los casos de error se arman con `server.use(...)` dentro de cada test y los tests renderizan la app.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)

## Path Conventions

Frontend único en `formula11/frontend`: código en `src/`, pruebas en `tests/`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Rutas y respuestas por defecto del contrato.

- [ ] T001 [P] Verificar que `src/api/endpoints.js` exponga `mercadoJugador(id)`, `comprar`, `vender` y `portfolio(usuarioId)` según `contracts/frontend-api.md`.
- [ ] T002 [P] Verificar en `src/test/handlers.js` respuestas por defecto con las formas del contrato para `/api/jugadores/:id/mercado`, `/api/usuarios/:id/portfolio` y las dos órdenes, sin ids ni parámetros especiales.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Normalización, validación y coordinación de estado que usan todas las historias.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T003 Verificar en `src/components/mercado/mercado.js` `normalizarMercado`, `normalizarPortfolio`, `tenenciaDe` y `describirResultado` contra `MercadoJugadorResponse`, `PortfolioResponse` y `OperacionResponse`.
- [ ] T004 Implementar en `validarOperacion` de `src/components/mercado/mercado.js` el orden de validación de `data-model.md`, incluido el mensaje "No quedan tokens disponibles de este jugador." cuando `tokensDisponibles` es 0.
- [ ] T005 [P] Agregar en `tests/components/mercado.test.js` los casos del orden de validación y del jugador agotado.
- [ ] T006 Corregir `src/hooks/useMercado.js` para que limpiar el resultado de una operación no modifique `enviando`, y exponer `reintentar()` que solo vuelve a consultar portfolio y `/mercado`.

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 - Comprar tokens (Priority: P1) 🎯 MVP

**Goal**: Un `USUARIO` compra tokens desde el detalle con el precio de `/mercado` y ve saldo, tenencia y disponibilidad actualizados.

**Independent Test**: Con MSW, comprar 5 tokens envía `POST /api/ordenes/compra` con `{ jugadorId, cantidad }`, muestra la confirmación de `OperacionResponse` y vuelve a pedir portfolio y `/mercado`.

### Tests for User Story 1

- [ ] T007 [P] [US1] Verificar en `tests/pages/mercado.test.jsx` la compra completa: datos del panel, cuerpo de la orden, confirmación, recarga y que el portfolio se pide con el `usuario.id` de la sesión (FR-006).
- [ ] T008 [P] [US1] Agregar en `tests/pages/mercado.test.jsx` un test donde, con la orden en curso, cambiar de tipo, editar la cantidad o reenviar no habilita un segundo envío (una sola orden recibida).
- [ ] T009 [P] [US1] Agregar en `tests/pages/mercado.test.jsx` un test con `rol: 'SUPERUSUARIO'` que verifique que el detalle se ve y el panel "Operar" no, y otro con una sesión sin `rol` que sí lo muestre (FR-005).
- [ ] T010 [P] [US1] Agregar en `tests/pages/mercado.test.jsx` un test del estado "Cargando tu saldo..." y de los valores iniciales (Comprar, cantidad 1) y del reinicio de la cantidad a 1 tras una operación exitosa.

### Implementation for User Story 1

- [ ] T011 [US1] Deshabilitar en `src/components/mercado/PanelOperacion.jsx` el selector Comprar/Vender y el campo de cantidad mientras `enviando` sea verdadero, e ignorar el envío del formulario en ese estado.
- [ ] T012 [US1] Mostrar el panel en `src/pages/DetalleJugadorPage.jsx` solo cuando el rol de la sesión no sea `SUPERUSUARIO` (una sesión sin `rol` cuenta como `USUARIO`).
- [ ] T013 [US1] Verificar en `src/components/mercado/PanelOperacion.jsx` el estado de carga, los datos visibles (saldo, tenencia con promedio, "N de M" disponibles, precio con fecha o "precio inicial" (FR-007), total estimado con el formato de montos de FR-010) y el reinicio de la cantidad tras operar.

**Checkpoint**: La compra funciona de punta a punta y no admite doble envío.

---

## Phase 4: User Story 2 - Vender tokens (Priority: P1)

**Goal**: El usuario vende tokens que tiene y puede llegar con la venta preseleccionada.

**Independent Test**: Con MSW, elegir "Vender" y confirmar envía `POST /api/ordenes/venta` y muestra "Vendiste ..."; `?operacion=venta` abre en "Vender".

### Tests for User Story 2

- [ ] T014 [P] [US2] Verificar en `tests/pages/mercado.test.jsx` la venta y la preselección con `?operacion=venta`.
- [ ] T015 [P] [US2] Agregar en `tests/pages/mercado.test.jsx` un caso con `?operacion=COMPRA` (mayúsculas) y uno con un valor desconocido que abra en "Comprar".

### Implementation for User Story 2

- [ ] T016 [US2] Verificar en `src/pages/DetalleJugadorPage.jsx` y `PanelOperacion.jsx` que `?operacion=` no distinga mayúsculas y que un valor desconocido abra en "Comprar".

**Checkpoint**: Compra y venta funcionan de forma independiente.

---

## Phase 5: User Story 3 - Validaciones y errores (Priority: P1)

**Goal**: Errores claros, sin órdenes inválidas y sin reenvíos.

**Independent Test**: Con MSW, las validaciones del cliente no envían órdenes; `409`/`422` muestran `mensaje` y `correlationId`; un fallo de recarga tras `201` conserva la confirmación y "Reintentar" no reenvía la orden.

### Tests for User Story 3

- [ ] T017 [P] [US3] Verificar en `tests/pages/mercado.test.jsx` las validaciones del cliente sin envío, los errores `409`/`422`, el fallo de `/mercado` y la marca "precio inicial" (FR-007).
- [ ] T018 [P] [US3] Agregar en `tests/pages/mercado.test.jsx` un test donde la orden devuelve `201`, falla la recarga, se conserva la confirmación, se muestra el error y "Reintentar" vuelve a pedir los datos con una sola orden enviada en total.
- [ ] T019 [P] [US3] Agregar en `tests/pages/mercado.test.jsx` un test donde un error de la orden habilita de nuevo el envío y conserva la cantidad ingresada.
- [ ] T020 [P] [US3] Agregar en `tests/pages/mercado.test.jsx` un test de `401` en la orden que limpie la sesión y muestre el acceso denegado.
- [ ] T021 [P] [US3] Agregar en `tests/pages/mercado.test.jsx` un test del fallo del portfolio (sin formulario y con `correlationId`).

### Implementation for User Story 3

- [ ] T022 [US3] Agregar en `src/components/mercado/PanelOperacion.jsx` el botón "Reintentar" junto al error de actualización, conectado a `reintentar()` de `useMercado`.
- [ ] T023 [US3] Verificar en `src/components/mercado/PanelOperacion.jsx` que un error de la orden conserve la cantidad, habilite el envío y no muestre confirmación.

**Checkpoint**: Todas las historias funcionan de forma independiente.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Accesibilidad, responsive y cierre.

- [ ] T024 [P] Verificar en `src/components/mercado/PanelOperacion.jsx` el grupo "Tipo de operación" con `aria-pressed`, el campo con label, `aria-invalid` y `aria-describedby`, la confirmación con `role="status"` y los errores con `role="alert"`.
- [ ] T025 [P] Agregar en `tests/pages/mercado.test.jsx` una verificación de que el token de la sesión no aparece en el panel ni en los mensajes de error (FR-012).
- [ ] T026 [P] Verificar en `src/styles/screens.css` que el panel ocupe el ancho completo debajo de la ficha hasta 820px sin desborde horizontal.
- [ ] T027 Correr `npm test`, `npm run lint` y `npm run build` desde `formula11/frontend`.
- [ ] T028 Ejecutar manualmente los escenarios de `specs/005-mercado-compra-venta/quickstart.md` con el backend y registrar desviaciones en `plan.md`.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias.
- **Foundational (Phase 2)**: depende de Setup; bloquea las historias (T006 lo usan T011 y T022).
- **User Stories (Phase 3–5)**: dependen de Foundational; pueden avanzar en paralelo.
- **Polish (Phase 6)**: depende de las historias.

### User Story Dependencies

- **US1**: tras Foundational.
- **US2**: tras Foundational; comparte el panel con US1 pero se prueba por separado.
- **US3**: tras Foundational; T022 necesita `reintentar()` de T006.

### Within Each User Story

- Los tests nuevos se escriben antes de la implementación y deben fallar primero.
- Los cambios en `PanelOperacion.jsx` de distintas historias se aplican en secuencia (mismo archivo).

### Parallel Opportunities

- T001 y T002; T005 con T006.
- Los tests marcados [P] de cada historia (mismo archivo de test, casos independientes).
- T024, T025 y T026.

---

## Parallel Example: User Story 1

```bash
# Tests de US1 que pueden escribirse juntos:
Task: "T008 Test de doble envío en tests/pages/mercado.test.jsx"
Task: "T009 Test de superusuario sin panel en tests/pages/mercado.test.jsx"
Task: "T010 Test de carga y valores iniciales en tests/pages/mercado.test.jsx"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 y Phase 2.
2. Phase 3 (compra sin doble envío y sin panel para el superusuario).
3. Validar con `npm test` antes de seguir.

### Incremental Delivery

1. Setup + Foundational.
2. US1 → validar.
3. US2 → validar.
4. US3 → validar.
5. Polish y validación manual con el backend (T028).
