---

description: "Task list for Formula11 player ranking"
---

# Tasks: Ranking de jugadores

**Input**: `spec.md` y `plan.md` de `/specs/004-ranking/`.

**Tests**: Obligatorios; Vitest, React Testing Library, user-event y MSW.

**Scope**: Solo frontend, contra mocks de MSW. Depende de 003.

## Phase 1: Fundamentos

- [X] T001 Agregar `ranking` a `src/api/endpoints.js`.
- [X] T002 [P] Crear `normalizarRanking` y `construirConsultaRanking` en `src/components/ranking/ranking.js`.
- [X] T003 [P] Agregar el handler por defecto de `GET /api/jugadores/ranking` en `src/test/handlers.js`, antes del de `:id`.

## Phase 2: User Story 1 - Ver el ranking

- [X] T004 Crear `src/hooks/useRanking.js` con consulta protegida.
- [X] T005 Crear `TablaRanking.jsx` con puesto, posición, link al detalle, equipo, precio, variación y score.
- [X] T006 Crear `src/pages/RankingPage.jsx`, la ruta `/ranking` y la estrategia activa en el título.
- [X] T007 Habilitar "Ranking" en `MenuPrincipal.jsx` y agregar breadcrumbs de `/ranking` y `/jugadores/:id`.
- [X] T008 Tests de orden, formato, variación, filas anidadas/paginadas, navegación desde el menú y hacia el detalle.

## Phase 3: User Story 2 - Filtrar por posición

- [X] T009 Botones de posición con `aria-pressed` y selector de liga, filtrados en el backend, con paginación.
- [X] T010 Test de filtro, posición vacía, "Todos" y una sola consulta.

## Phase 4: User Story 3 - Estados

- [X] T011 Estados de carga, vacío, error y sin sesión.
- [X] T012 Tests de vacío, error con `correlationId` y sin sesión.

## Phase 5: Cierre

- [X] T013 Estilos responsive en `src/styles/screens.css` (Equipo y Score ocultos hasta 760px); verificado sin desborde a 375px.
- [X] T014 `npm test`, `npm run lint` y `npm run build` en verde.
- [X] T015 Alinear con el contrato `005-contratos-entrega2`: página del backend con `puesto`, filtros de posición y liga en el backend, paginación de 20 y `variacion` en %.
