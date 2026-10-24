---

description: "Task list for Formula11 operations history"
---

# Tasks: Historial de operaciones

**Input**: Design documents from `/specs/007-historial-operaciones/`

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

- [X] T001 [P] Verificar que `src/api/endpoints.js` exponga `transacciones(usuarioId)` y que la consulta no envíe `sort` ni `jugadorId` (FR-001, FR-002).
- [X] T002 [P] Verificar en `src/test/handlers.js` la respuesta por defecto con la forma de `TransaccionesPageResponse`, sin ids ni parámetros especiales.
- [X] T003 [P] Verificar que `src/components/catalogo/PaginacionCatalogo.jsx` acepte la prop `etiqueta` y muestre "Página N de M".

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T004 Verificar en `src/components/historial/historial.js` la normalización de `OperacionResponse`, el descarte de tipos desconocidos, el formato de fechas de FR-004 y `efectoEnSaldo` con el `total` del backend (FR-003, FR-004, FR-007).
- [X] T005 Agregar en `src/hooks/useHistorial.js` la recuperación única de una página fuera de rango (`totalElements` > 0, `content` vacío y `page` ≥ `totalPages`).
- [X] T006 Agregar en `src/hooks/useHistorial.js` la opción `habilitado` para no consultar la API cuando el rol es `SUPERUSUARIO` (FR-006).

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 - Ver mis operaciones (Priority: P1) 🎯 MVP

**Goal**: El `USUARIO` ve sus operaciones paginadas con fecha, tipo, jugador, tokens, precio, movimiento, saldo final y contraparte.

**Independent Test**: Con MSW, `/operaciones` muestra la tabla con los datos de `OperacionResponse`, la paginación y la marca de precio inicial; el superusuario ve el aviso sin consulta.

### Tests for User Story 1

- [X] T007 [P] [US1] Verificar en `tests/pages/historial.test.jsx` la tabla, la consulta enviada (`page`, `size`, sin `sort`) con el `usuario.id` de la sesión y la navegación portfolio ↔ historial (FR-002, FR-009).
- [X] T008 [P] [US1] Agregar en `tests/pages/historial.test.jsx` un test de la marca "(precio inicial)" para una operación con `estrategia` en null y su ausencia en las demás.
- [X] T009 [P] [US1] Agregar en `tests/pages/historial.test.jsx` un test con `rol: 'SUPERUSUARIO'` que muestre el aviso y no consulte la API (FR-006).
- [X] T010 [P] [US1] Agregar en `tests/pages/historial.test.jsx` un test de página fuera de rango que vuelva una sola vez a la última página válida.
- [X] T025 [P] [US1] Agregar en `tests/pages/historial.test.jsx` un test donde, desde una compra confirmada en el detalle, el link "Ver historial" lleva a `/operaciones`, y otro donde `Escape` en el historial vuelve al portfolio.

### Implementation for User Story 1

- [X] T011 [US1] Mostrar en `src/components/historial/TablaHistorial.jsx` la marca "(precio inicial)" junto al precio unitario cuando `estrategia` es null.
- [X] T012 [US1] Mostrar en `src/pages/HistorialPage.jsx` el aviso del superusuario y pasarle `habilitado` a `useHistorial`.
- [X] T013 [US1] Verificar en `src/components/historial/TablaHistorial.jsx` el `caption`, los encabezados, el tipo como texto y el movimiento con signo y color (FR-007, FR-008).

**Checkpoint**: La tabla funciona de forma independiente.

---

## Phase 4: User Story 2 - Filtrar por tipo (Priority: P2)

**Goal**: Filtros Todas/Compras/Ventas en el backend, sin consultas repetidas ni respuestas obsoletas.

**Independent Test**: Con MSW, cambiar el filtro consulta con `tipo` y vuelve a la página 1; repetirlo no consulta; una respuesta lenta del filtro anterior no pisa la actual.

### Tests for User Story 2

- [X] T014 [P] [US2] Verificar en `tests/pages/historial.test.jsx` los filtros, la vuelta a la página 1 y que repetir el filtro activo no consulta.
- [X] T015 [P] [US2] Agregar en `tests/pages/historial.test.jsx` un test donde la respuesta del filtro anterior llega tarde y no reemplaza la del filtro vigente.
- [X] T016 [P] [US2] Agregar en `tests/pages/historial.test.jsx` un test de que pasar de página conserva el filtro activo.

### Implementation for User Story 2

- [X] T017 [US2] Verificar en `src/hooks/useHistorial.js` que la página conserve el filtro y que las respuestas de consultas no vigentes se descarten.

**Checkpoint**: Filtro y paginación funcionan juntos.

---

## Phase 5: User Story 3 - Estados (Priority: P1)

**Goal**: Carga, vacío según el filtro, errores y sesión.

**Independent Test**: Con MSW, se ven "Cargando operaciones...", los textos de vacío por filtro, el error con `correlationId` y el acceso denegado ante `401`.

### Tests for User Story 3

- [X] T018 [P] [US3] Verificar en `tests/pages/historial.test.jsx` el estado vacío, el error del backend y la ausencia de consultas sin sesión.
- [X] T019 [P] [US3] Agregar en `tests/pages/historial.test.jsx` un test del estado de carga y uno de `401` que limpie la sesión.

### Implementation for User Story 3

- [X] T020 [US3] Verificar en `src/pages/HistorialPage.jsx` los textos de carga y vacío por filtro del spec.

**Checkpoint**: Todas las historias funcionan de forma independiente.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [X] T021 [P] Agregar en `tests/pages/historial.test.jsx` una verificación de que el token de la sesión no aparece en la página (FR-009).
- [X] T022 [P] Verificar en `src/styles/screens.css` las columnas ocultas hasta 760px (FR-005).
- [X] T023 Correr `npm test`, `npm run lint` y `npm run build` desde `formula11/frontend`.
- [ ] T024 Ejecutar manualmente los escenarios de `specs/007-historial-operaciones/quickstart.md` con el backend y registrar desviaciones en `plan.md`.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sin dependencias.
- **Foundational (Phase 2)**: depende de Setup; T005 lo usa T010 y T006 lo usa T012.
- **User Stories (Phase 3–5)**: dependen de Foundational.
- **Polish (Phase 6)**: depende de las historias.

### User Story Dependencies

- **US1**, **US2** y **US3**: tras Foundational; comparten `useHistorial.js` y `HistorialPage.jsx`, así que sus cambios en esos archivos van en secuencia.

### Within Each User Story

- Los tests nuevos se escriben antes de la implementación y deben fallar primero.

### Parallel Opportunities

- T001, T002 y T003.
- Los tests marcados [P] de cada historia.
- T021 y T022.

---

## Parallel Example: User Story 1

```bash
Task: "T008 Test de precio inicial en tests/pages/historial.test.jsx"
Task: "T009 Test del superusuario en tests/pages/historial.test.jsx"
Task: "T010 Test de página fuera de rango en tests/pages/historial.test.jsx"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 y Phase 2.
2. Phase 3 (tabla, precio inicial, superusuario y página fuera de rango).
3. Validar con `npm test`.

### Incremental Delivery

1. Setup + Foundational.
2. US1 → validar.
3. US2 → validar.
4. US3 → validar.
5. Polish y validación manual con el backend (T024).
