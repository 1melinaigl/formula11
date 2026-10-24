---

description: "Task list for Formula11 player ranking"
---

# Tasks: Ranking de jugadores

**Input**: Design documents from `/specs/004-ranking/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/frontend-api.md, quickstart.md

**Tests**: Obligatorios; Vitest, React Testing Library, user-event y MSW.

**Scope**: Solo frontend, contra el contrato de Entrega 2 y mocks de MSW. Depende de la feature 003 para sesión, cliente protegido, detalle de jugador y formateadores compartidos.

## Phase 1: Setup

**Purpose**: Confirmar la estructura existente y preparar los puntos de integración del ranking.

- [ ] T001 [P] Revisar `src/api/endpoints.js`, `src/api/clienteApi.js` y `src/context/SesionContext.jsx` para reutilizar la ruta centralizada, `solicitarProtegida`, `Authorization`, `X-Correlation-Id` y limpieza de sesión ante `401`.
- [ ] T002 [P] Revisar `src/router/AppRouter.jsx`, `src/components/navegacion/MenuPrincipal.jsx` y `src/components/navegacion/Encabezado.jsx` para identificar la ruta `/ranking`, el acceso del menú y los breadcrumbs sin duplicar navegación.
- [ ] T003 [P] Revisar `src/styles/screens.css`, `src/styles/components.css` y `design/formula11-mockup-retro-v3.html` para documentar el patrón visual y el breakpoint responsive de 760px aplicable a la tabla.

## Phase 2: Foundational

**Purpose**: Fijar contrato, normalización y fixtures compartidos antes de implementar las historias.

**⚠️ CRITICAL**: No iniciar las historias hasta completar esta fase.

- [ ] T004 Agregar `ranking: '/api/jugadores/ranking'` a `src/api/endpoints.js` sin introducir rutas alternativas ni URLs directas en componentes.
- [ ] T005 [P] Crear `normalizarRanking` en `src/components/ranking/ranking.js` para consumir `RankingPageResponse` con `content`, `page`, `size`, `totalElements`, `totalPages`, `estrategia`, `version` y `fecha`, conservando el orden y `puesto` del backend.
- [ ] T006 [P] Crear `construirConsultaRanking` en `src/components/ranking/ranking.js` para enviar siempre `page` base cero y `size=20`, omitir filtros vacíos, enviar posiciones `POR|DEF|MED|DEL` y no enviar `estrategia`.
- [ ] T007 [P] Implementar en `src/components/ranking/ranking.js` la conversión de `variacion` contractual (`12.5` significa `12,5 %`) a la representación interna de los formateadores, sin recalcular `puesto` ni `score` y descartando filas sin `jugador.id` o con `precio` no numérico finito.
- [ ] T008 [P] Agregar en `src/test/handlers.js` un handler MSW contractual para `GET /api/jugadores/ranking` antes del handler `/api/jugadores/:id`, con `content`, paginación y metadatos de estrategia activa.
- [ ] T009 [P] Crear fixtures de `RankingPageResponse`, `RankingItem`, filtros y errores con `mensaje`, `timestamp` y `correlationId` en `tests/pages/ranking.test.jsx` sin exponer tokens en aserciones ni logs.

**Checkpoint**: contrato, consulta, normalización y fixtures están listos para las historias.

## Phase 3: User Story 1 - Ver el ranking (Priority: P1) 🎯 MVP

**Goal**: Permitir que un usuario autenticado vea el ranking de la estrategia activa, con puestos, datos de jugador, metadatos y navegación.

**Independent Test**: Con MSW, `GET /api/jugadores/ranking` devuelve filas anidadas ordenadas; `/ranking` muestra el orden y puesto recibidos, la estrategia activa, precio, variación, score y enlace al detalle sin enviar `estrategia`.

### Tests for User Story 1

- [ ] T010 [P] [US1] Agregar en `tests/pages/ranking.test.jsx` un test de la respuesta contractual que compruebe headers protegidos, `page=0`, `size=20`, ausencia de `estrategia`, filas anidadas, orden y puesto recibido.
- [ ] T011 [P] [US1] Agregar en `tests/pages/ranking.test.jsx` tests de formato para posición legible, precio en créditos, variación positiva/negativa/nula como `—` y score nulo como `—`, incluyendo `fecha` formateada como `dd/mm/aaaa`.
- [ ] T012 [P] [US1] Agregar en `tests/pages/ranking.test.jsx` un test de metadatos sin recálculo que exija `Estrategia activa: <nombre> · sin recálculo todavía` cuando `version` y `fecha` son `null`.
- [ ] T013 [P] [US1] Agregar en `tests/pages/ranking.test.jsx` un test de navegación desde el nombre del jugador a `/jugadores/:id` y otro de acceso al ranking desde `MenuPrincipal.jsx`.

### Implementation for User Story 1

- [ ] T014 [US1] Crear `src/hooks/useRanking.js` para consultar `endpoints.ranking` con `solicitarProtegida`, mantener `cargando`, `respuesta` y `error`, cancelar actualizaciones después de desmontar y no consultar cuando `usuario` es nulo.
- [ ] T015 [US1] Crear `src/components/ranking/TablaRanking.jsx` con tabla semántica, encabezados `scope="col"`, puesto contractual, posición, enlace al detalle, jugador, equipo, precio en créditos, variación semántica y score.
- [ ] T016 [US1] Crear `src/pages/RankingPage.jsx` con encabezado `Ranking de jugadores`, metadatos de estrategia activa, estado de carga `Cargando ranking...`, integración de `useRanking` y retorno al menú mediante botón y `Escape`.
- [ ] T017 [US1] Actualizar `src/router/AppRouter.jsx` para registrar `/ranking` con la protección de sesión existente y sin introducir fetch en la página.
- [ ] T018 [US1] Habilitar el acceso `Ranking` en `src/components/navegacion/MenuPrincipal.jsx` y actualizar breadcrumbs en `src/components/navegacion/Encabezado.jsx` para `/ranking` y `/jugadores/:id`.

**Checkpoint**: US1 muestra el ranking activo completo y es navegable de forma independiente.

## Phase 4: User Story 2 - Filtrar por posición (Priority: P2)

**Goal**: Permitir filtrar por posición o liga delegando filtrado, orden y puesto al backend, con paginación de 20 filas.

**Independent Test**: Al elegir una posición o liga se solicita la primera página con los códigos contractuales correctos; repetir un filtro no consulta de nuevo; una respuesta vacía muestra `Sin jugadores`; la paginación muestra páginas desde 1 mientras envía índices desde 0.

### Tests for User Story 2

- [ ] T019 [P] [US2] Agregar en `tests/pages/ranking.test.jsx` un test de filtros que compruebe `POR`, `DEF`, `MED`, `DEL` y el nombre exacto de liga en la query, sin filtrado local.
- [ ] T020 [P] [US2] Agregar en `tests/pages/ranking.test.jsx` tests de reinicio a `page=0`, `aria-pressed`, botón `Todos`, filtro repetido sin consulta adicional, preservación del puesto recibido por backend y descarte de una respuesta obsoleta cuando cambia el filtro o la página mientras hay una consulta pendiente.
- [ ] T021 [P] [US2] Agregar en `tests/pages/ranking.test.jsx` tests de `content: []` con filtro (`Sin jugadores`) y de más de una página, verificando `page=1` en la API y `Página 2` en la UI.

### Implementation for User Story 2

- [ ] T022 [US2] Extender `src/hooks/useRanking.js` con estado de consulta `{ page: 0, posicion: '', liga: '' }`, cambios de filtro que reinicien a `page=0`, cambios de página y prevención de consultas cuando el filtro ya está activo.
- [ ] T023 [US2] Completar `src/pages/RankingPage.jsx` con botones de posición que usen `aria-pressed`, opción `Todos`, selector de liga con label asociado y estado `Sin jugadores` cuando hay filtros activos sin resultados.
- [ ] T024 [US2] Integrar `src/components/catalogo/PaginacionCatalogo.jsx` en `src/pages/RankingPage.jsx` con etiqueta accesible, deshabilitado en extremos y numeración visible desde 1 a partir de páginas base cero.
- [ ] T025 [US2] Ajustar `src/components/ranking/TablaRanking.jsx` y `src/components/ranking/ranking.js` para preservar literalmente el orden y `puesto` del backend, sin ordenar ni filtrar filas en el frontend.

**Checkpoint**: US1 y US2 permiten consultar el ranking activo completo o filtrado con paginación backend.

## Phase 5: User Story 3 - Estados de la pantalla (Priority: P1)

**Goal**: Cubrir carga, ranking vacío, error trazable y ausencia de sesión sin exponer el JWT.

**Independent Test**: Cada estado puede provocarse con handlers MSW aislados y produce el texto, acceso y comportamiento de sesión definidos por el spec.

### Tests for User Story 3

- [ ] T026 [P] [US3] Agregar en `tests/pages/ranking.test.jsx` un test de carga que exija `Cargando ranking...` mientras la respuesta está pendiente.
- [ ] T027 [P] [US3] Agregar en `tests/pages/ranking.test.jsx` un test de respuesta vacía sin filtros que muestre `Ranking vacío` y el detalle de que no hubo recálculo.
- [ ] T028 [P] [US3] Agregar en `tests/pages/ranking.test.jsx` tests de errores `400`, `403` y `5xx` que exijan `mensaje` y `correlationId` y mantengan la pantalla de ranking; incluir también un error contractual incompleto que use el `X-Correlation-Id` generado por `clienteApi` cuando falte `correlationId` y muestre `No se pudo completar la solicitud.` cuando falte `mensaje`.
- [ ] T029 [P] [US3] Agregar en `tests/pages/ranking.test.jsx` un test de `401` que compruebe limpieza de sesión y otro sin sesión que compruebe ausencia de llamada protegida y pantalla de acceso denegado.

### Implementation for User Story 3

- [ ] T030 [US3] Completar `src/pages/RankingPage.jsx` con los estados `Cargando ranking...`, `Ranking vacío`, `Sin jugadores`, acceso denegado y `AvisoError` sin ocultar `mensaje` ni `correlationId`.
- [ ] T031 [US3] Ajustar `src/hooks/useRanking.js` para limpiar el error al cambiar filtros o página, conservar solo la respuesta de la consulta vigente, descartar actualizaciones obsoletas después de cambios de parámetros y evitar actualizar estado tras desmontaje.
- [ ] T032 [US3] Mantener en `src/test/handlers.js` solo respuestas por defecto contractuales y configurar `401`, `400`, `403` y `5xx` con `server.use(...)` dentro de cada test.

**Checkpoint**: las tres historias cubren éxito, carga, vacío, filtros, errores y ausencia de sesión.

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T033 [P] Revisar `src/styles/screens.css` y `src/styles/components.css` para mantener la paleta y jerarquía visual del mockup, ocultar Equipo y Score hasta 760px y evitar desborde a 375px.
- [ ] T034 [P] Revisar `src/pages/RankingPage.jsx`, `src/components/ranking/TablaRanking.jsx` y `src/components/catalogo/PaginacionCatalogo.jsx` para labels asociados, navegación por teclado, foco visible, `aria-pressed` y nombres accesibles.
- [ ] T035 [P] Actualizar `specs/004-ranking/quickstart.md`, `specs/004-ranking/contracts/frontend-api.md` y `specs/004-ranking/data-model.md` si la implementación requiere precisar escenarios, campos o estados, sin contradecir `spec.md`.
- [ ] T036 Ejecutar `npm test`, `npm run lint` y `npm run build` desde `frontend/`, resolver regresiones y confirmar que no se agrega `estrategia` a las consultas.
- [ ] T037 Ejecutar manualmente los escenarios de `specs/004-ranking/quickstart.md` y registrar en `specs/004-ranking/plan.md` cualquier desviación visual, contractual o de accesibilidad antes de cerrar la feature.

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin dependencias; T001-T003 pueden ejecutarse en paralelo.
- **Foundational (Phase 2)**: Depende de Phase 1; T004-T009 preparan endpoint, contrato,
  normalización y fixtures.
- **US1 (Phase 3)**: Depende de Phase 2; entrega el MVP visible.
- **US2 (Phase 4)**: Depende de US1 para reutilizar la página y tabla; T019-T021 pueden
  escribirse en paralelo antes de T022-T025.
- **US3 (Phase 5)**: Depende de US1 y puede endurecer US2; T026-T029 pueden ejecutarse
  en paralelo antes de T030-T032.
- **Polish (Phase 6)**: Depende de las tres historias; T033-T035 pueden ejecutarse en
  paralelo antes de T036-T037.

### User Story Dependencies

- **US1 (P1)**: Independiente después de Foundational; es el MVP recomendado.
- **US2 (P2)**: Depende de la composición de US1, pero no cambia el contrato base.
- **US3 (P1)**: Depende de US1 para estados de la pantalla y reutiliza el cliente de
  sesión; sus pruebas completan los caminos de excepción.

### Parallel Opportunities

- **Setup**: T001, T002 y T003.
- **Foundational**: T005-T009 después de T004, cuando no compartan el mismo archivo.
- **US1 tests**: T010-T013.
- **US2 tests**: T019-T021; implementación T023-T025 coordinada con T022.
- **US3 tests**: T026-T029; implementación T030-T032 coordinada por archivo.
- **Polish**: T033-T035.

## Implementation Strategy

1. **MVP**: completar Phase 1, Phase 2 y US1 para mostrar el ranking activo, sus
   metadatos y navegación.
2. **Filtrado incremental**: agregar US2 con códigos de posición, liga, reinicio de
   página y paginación backend.
3. **Hardening**: completar US3 con estados y errores trazables.
4. **Cierre**: ejecutar accesibilidad, revisión visual, quickstart, tests, lint y build
   antes de considerar la feature terminada.
