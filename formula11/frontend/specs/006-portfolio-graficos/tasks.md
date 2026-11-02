---

description: "Task list for Formula11 user portfolio and charts"
---

# Tasks: Portfolio y gráficos del usuario

**Input**: Design documents from `/specs/006-portfolio-graficos/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/frontend-api.md

**Tests**: Requeridos por la constitución (principio V). Vitest + React Testing Library + MSW; los casos de error se arman con `server.use(...)` dentro de cada test y los tests renderizan la app. Los tests existentes no se modifican.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)

## Path Conventions

Frontend único en `formula11/frontend`: código en `src/`, pruebas en `tests/`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Rutas, mocks y estilos compartidos.

- [X] T001 [P] Verificar que `src/api/endpoints.js` exponga `portfolio(usuarioId)` y `evolucionPortfolio(usuarioId)` y que la evolución se pida sin `desde` ni `hasta` (FR-001).
- [X] T002 [P] Verificar en `src/test/handlers.js` respuestas por defecto con las formas de `PortfolioResponse` y `EvolucionPortfolioResponse`, sin ids ni parámetros especiales.
- [X] T003 [P] Verificar que `src/components/comun/estiloGraficos.js` concentre ejes, grilla y tooltip, y que `src/pages/PortfolioPage.jsx` se cargue con `lazy` en `src/router/AppRouter.jsx` (FR-005).

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Normalización y orden que usan todas las historias.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T004 Agregar en `src/components/portfolio/portfolio.js` un orden común de tenencias (mayor `valorActual` primero; a igual valor, por nombre) y usarlo en `composicion` (FR-008).
- [X] T005 [P] Agregar en `tests/components/portfolio.test.js` los casos del orden común y del desempate por nombre.
- [X] T006 Verificar en `src/components/portfolio/portfolio.js` y `src/hooks/usePortfolio.js` que los totales y los valores por fila salgan del backend sin recalcular y que el valor total sea `saldo` + `valorTenencias` (FR-002).

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 - Ver mi posición (Priority: P1) 🎯 MVP

**Goal**: El `USUARIO` ve sus totales, su fecha de valuación y sus tenencias ordenadas.

**Independent Test**: Con MSW, `/portfolio` muestra las cinco tarjetas con los valores del backend, "Valuado al ...", y la tabla ordenada por valor con "Vender" hacia `?operacion=venta`.

### Tests for User Story 1

- [X] T007 [P] [US1] Verificar en `tests/pages/portfolio.test.jsx` el resumen, la tabla, el estado sin tenencias, la navegación desde el menú y que el portfolio se pida con el `usuario.id` de la sesión (FR-007).
- [X] T008 [P] [US1] Agregar en `tests/pages/portfolio.test.jsx` un test de orden de la tabla por valor actual cuando el backend manda las posiciones desordenadas (FR-008).
- [X] T009 [P] [US1] Agregar en `tests/pages/portfolio.test.jsx` un test con ganancia negativa (tarjeta "Pérdida") y otro con `invertido` en 0 ("Sin inversión") (FR-002).
- [X] T010 [P] [US1] Agregar en `tests/pages/portfolio.test.jsx` un test con `rol: 'SUPERUSUARIO'`: el menú no muestra "Mi portfolio", `/portfolio` muestra el aviso y no se consulta la API (FR-006).

### Implementation for User Story 1

- [X] T011 [US1] Ordenar las tenencias en `src/hooks/usePortfolio.js` con el orden común de T004, para que la tabla las reciba ya ordenadas (FR-008).
- [X] T012 [US1] Ocultar "Mi portfolio" para el `SUPERUSUARIO` en `src/components/navegacion/MenuPrincipal.jsx` (FR-006).
- [X] T013 [US1] Mostrar en `src/pages/PortfolioPage.jsx` el aviso del superusuario y no consultar la API en `src/hooks/usePortfolio.js` para ese rol (FR-006).

**Checkpoint**: El resumen y la tabla funcionan de forma independiente.

---

## Phase 4: User Story 2 - Gráficos (Priority: P1)

**Goal**: Los tres gráficos, con alternativa textual y comportamiento con poca historia o error.

**Independent Test**: Con MSW, con 2 o más puntos se ve el gráfico de evolución y su tabla oculta; con 0 o 1 punto, el aviso; con error en la evolución, el error en ese lugar y el resto visible.

### Tests for User Story 2

- [X] T014 [P] [US2] Verificar en `tests/pages/portfolio.test.jsx` los tres gráficos y que un error de la evolución no oculta el resto.
- [X] T015 [P] [US2] Agregar en `tests/pages/portfolio.test.jsx` un test con un solo punto de evolución que muestre el aviso de poca historia.
- [X] T016 [P] [US2] Agregar en `tests/pages/portfolio.test.jsx` un test de la tabla oculta "Evolución semanal" con fecha, valor e invertido.
- [X] T017 [P] [US2] Agregar en `tests/pages/portfolio.test.jsx` un test con más de 7 tenencias: la tabla las lista todas y la composición agrupa "Otros (n)".

### Implementation for User Story 2

- [X] T018 [US2] Mostrar en `src/pages/PortfolioPage.jsx` la tarjeta con el aviso de poca historia cuando la evolución tenga menos de 2 puntos.
- [X] T019 [US2] Agregar en `src/components/portfolio/GraficosPortfolio.jsx` la tabla oculta visualmente "Evolución semanal" (fecha, valor, invertido) dentro de la tarjeta de evolución.

**Checkpoint**: Gráficos completos y accesibles.

---

## Phase 5: User Story 3 - Estados (Priority: P1)

**Goal**: Carga, errores y sesión.

**Independent Test**: Con MSW, se ven "Cargando portfolio...", el error del portfolio con `correlationId`, el acceso denegado ante `401` y ninguna consulta sin sesión.

### Tests for User Story 3

- [X] T020 [P] [US3] Verificar en `tests/pages/portfolio.test.jsx` el error del portfolio y la ausencia de consultas sin sesión.
- [X] T021 [P] [US3] Agregar en `tests/pages/portfolio.test.jsx` un test del estado de carga.
- [X] T022 [P] [US3] Agregar en `tests/pages/portfolio.test.jsx` un test de `401` en el portfolio que limpie la sesión, y otro donde falla el portfolio pero la evolución responde y solo se ve el error.

### Implementation for User Story 3

- [X] T023 [US3] Verificar en `src/pages/PortfolioPage.jsx` los estados de carga, error y sin sesión según el spec.

**Checkpoint**: Todas las historias funcionan de forma independiente.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [X] T024 [P] Agregar en `tests/pages/portfolio.test.jsx` una verificación de que el token de la sesión no aparece en la página (FR-007).
- [X] T025 [P] Verificar en `src/styles/screens.css` las columnas ocultas hasta 760px y `min-width: 0` en las tarjetas de gráficos (FR-004).
- [X] T026 [P] Verificar en `src/components/portfolio/portfolio.js` los colores de FR-003 y que las etiquetas de ganancia lleven signo.
- [X] T027 Correr `npm test`, `npm run lint` y `npm run build` desde `formula11/frontend`.
- [ ] T028 Ejecutar manualmente los escenarios de `specs/006-portfolio-graficos/quickstart.md` con el backend y registrar desviaciones en `plan.md`.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias.
- **Foundational (Phase 2)**: depende de Setup; T004 lo usan T011 y la composición.
- **User Stories (Phase 3–5)**: dependen de Foundational.
- **Polish (Phase 6)**: depende de las historias.

### User Story Dependencies

- **US1**: tras Foundational.
- **US2**: tras Foundational; usa el orden de T004 en la composición.
- **US3**: tras Foundational.

### Within Each User Story

- Los tests nuevos se escriben antes de la implementación y deben fallar primero.
- Los cambios en `PortfolioPage.jsx` de distintas historias se aplican en secuencia.

### Parallel Opportunities

- T001, T002 y T003; T005 con T006.
- Los tests marcados [P] de cada historia.
- T024, T025 y T026.

---

## Parallel Example: User Story 1

```bash
Task: "T008 Test de orden de la tabla en tests/pages/portfolio.test.jsx"
Task: "T009 Test de pérdida y sin inversión en tests/pages/portfolio.test.jsx"
Task: "T010 Test del superusuario en tests/pages/portfolio.test.jsx"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 y Phase 2.
2. Phase 3 (resumen y tabla ordenada; superusuario sin portfolio).
3. Validar con `npm test`.

### Incremental Delivery

1. Setup + Foundational.
2. US1 → validar.
3. US2 → validar.
4. US3 → validar.
5. Polish y validación manual con el backend (T028).
