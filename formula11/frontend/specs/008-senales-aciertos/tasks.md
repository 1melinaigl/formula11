---

description: "Task list for Formula11 undervalued-player signals and accuracy panel"
---

# Tasks: Señales de jugadores subvaluados y panel de aciertos

**Input**: Design documents from `/specs/008-senales-aciertos/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/frontend-api.md

**Tests**: Los exigen la constitución (principio V) y SC-006. Se usan Vitest, React Testing Library y MSW; los casos se arman con `server.use(...)` dentro de cada test y los tests de páginas renderizan la app. Los tests de la alerta usan `vi.useFakeTimers({ shouldAdvanceTime: true })` y restauran los timers reales en `afterEach`. No se modifica ningún test existente.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)

## Path Conventions

Frontend único en `formula11/frontend`: el código va en `src/` y las pruebas, en `tests/`.

---

## Phase 1: Setup (Shared Infrastructure)

- [X] T001 [P] Agregar a `src/api/endpoints.js` las rutas `senales: '/api/senales'` y `aciertos: '/api/senales/aciertos'` (FR-001).
- [X] T002 [P] Agregar a `src/test/handlers.js` dos handlers por defecto: `GET /api/senales` devuelve `{ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }` y `GET /api/senales/aciertos` devuelve `{ semanas: [], tasaAcierto: 0, diferenciaPromedio: 0 }` (research §8). Solo se agregan handlers; no se cambia ninguno de los existentes.
- [X] T003 Correr `npm test` y confirmar que los 127 tests existentes siguen pasando sin cambios después de T001 y T002.

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T004 [P] Crear `tests/components/senales.test.js` con los tests de las reglas de `senales.js` (deben fallar antes de T005):
  - `normalizarSenales` descarta las señales con un `tipo` distinto de `ENTRE_ESTRATEGIAS` o `PRECIO_ATRASADO`, o sin `id` o `jugador`, y conserva el orden recibido.
  - `formatearPorcentaje(18)` → "+18,00%", `formatearPorcentaje(-1.43)` → "-1,43%" y `formatearPorcentaje(0)` → "0,00%".
  - `senalesNuevas(senales, vistas)` devuelve las no vistas en el mismo orden.
  - El registro de vistas lee y escribe `formula11.senales.vistas.{usuarioId}`, guarda "como máximo 500" ids descartando los más viejos, toma un JSON inválido como vacío y se borra con `borrarVistas`.
- [X] T005 Crear `src/components/senales/senales.js` con:
  - `TAMANO_PAGINA_SENALES = 20` y `TIPOS_SENAL`: `ENTRE_ESTRATEGIAS` → "Entre estrategias" / "Otra estrategia lo valora más"; `PRECIO_ATRASADO` → "Precio atrasado" / "Jugadores con rendimiento parecido valen más" (FR-008).
  - `normalizarSenales`, `formatearPorcentaje` (`Intl.NumberFormat('es-AR')` con 2 decimales y signo; FR-007), `senalesNuevas` y `construirConsultaSenales({ vigentes, page, size })`, sin `sort`.
  - `leerVistas(usuarioId)`, `agregarVistas(usuarioId, ids)` y `borrarVistas(usuarioId)` sobre `sessionStorage` (FR-003, data-model "Señales vistas").
  - Las fechas se formatean con `formatearFecha` de `cotizaciones.js` (FR-009).

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 - Recibir alertas de jugadores subvaluados (Priority: P1) 🎯 MVP

**Goal**: Con sesión, cualquier pantalla muestra una alerta agrupada con las señales vigentes que el usuario todavía no vio, y la consulta se repite cada 30 segundos.

**Independent Test**: Con MSW y timers falsos, después de iniciar sesión aparece la alerta en `/catalogo`. Cerrarla evita que reaparezca, y una señal nueva genera una alerta solo con esa señal.

### Tests for User Story 1

- [X] T006 [P] [US1] Crear `tests/pages/alertaSenales.test.jsx` con un test donde, con sesión y 5 señales vigentes, aparece en `/catalogo` la alerta "5 oportunidades nuevas" con las 3 primeras (jugador, tipo y brecha). La consulta enviada tiene que ser `vigentes=true&page=0&size=100` (FR-002).
- [X] T007 [P] [US1] Agregar a `tests/pages/alertaSenales.test.jsx` tests de cierre: "Cerrar" o `Escape` ocultan la alerta y, después de avanzar 30 segundos con las mismas señales, no reaparece; con una señal nueva reaparece como "1 oportunidad nueva". "Ver señales" navega a `/senales` y marca las señales como vistas.
- [X] T008 [P] [US1] Agregar a `tests/pages/alertaSenales.test.jsx` tests de bordes:
  - Una consulta con `500` no muestra error ni alerta, y la siguiente sí muestra la alerta.
  - Un `401` limpia la sesión y no se hacen más consultas.
  - Al cerrar la sesión se cortan las consultas y se borra `formula11.senales.vistas.{usuarioId}`.
  - Sin sesión no se consulta.
  - Con la lista vacía no hay alerta.
- [X] T009 [P] [US1] Agregar a `tests/pages/alertaSenales.test.jsx` un test donde, si la consulta anterior todavía no respondió, el ciclo siguiente no envía una consulta nueva.

### Implementation for User Story 1

- [X] T010 [US1] Crear `src/hooks/useAlertasSenales.js`:
  - Hace una consulta inicial y después una cada 30 segundos con `setInterval`, dependiente de `usuario.id`, y saltea el ciclo si hay una consulta pendiente.
  - Calcula las señales nuevas con `senalesNuevas`, sin mostrar errores (si la consulta falla, espera al ciclo siguiente).
  - Expone `nuevas`, `cerrar()` y `verSenales()`, que marcan como vistas con `agregarVistas`.
  - Llama a `borrarVistas` cuando `usuario` pasa a `null` (research §2 a §5).
- [X] T011 [US1] Crear `src/components/senales/AlertaSenales.jsx`:
  - Es una región `role="status"` con el título "N oportunidades nuevas" (singular para 1) y las 3 primeras con nombre, tipo legible y brecha.
  - Tiene los botones "Ver señales" (navega a `/senales`) y "Cerrar", y se cierra con `Escape` cuando el foco está adentro.
  - No mueve el foco al aparecer (FR-004).
- [X] T012 [US1] Montar `<AlertaSenales />` en `src/App.jsx`, dentro de `BrowserRouter` y debajo de `<Encabezado />`, para que haya una sola consulta periódica (research §1).
- [X] T013 [US1] Agregar a `src/styles/components.css` el estilo de la alerta: un `.panel` con borde de acento, fijo arriba a la derecha en escritorio y a todo el ancho debajo del encabezado hasta 760px, sin scroll horizontal a 375px (principio I).

**Checkpoint**: Las alertas funcionan en toda la app. "Ver señales" lleva a `/senales`, que se completa en US2.

---

## Phase 4: User Story 2 - Ver las señales y comprar en un click (Priority: P1)

**Goal**: `/senales` lista las señales vigentes o del historial, paginadas, y permite al `USUARIO` comprar 1 token desde cada fila.

**Independent Test**: Con MSW, el menú lleva a `/senales`, la tabla muestra las señales con su formato, Vigentes/Historial cambia la consulta y una compra muestra la confirmación o el error correspondiente.

### Tests for User Story 2

- [X] T014 [P] [US2] Crear `tests/pages/senales.test.jsx` con tests de acceso:
  - El menú principal muestra "Señales" (subtítulo "Jugadores subvaluados") con sesión de `USUARIO` y de `SUPERUSUARIO`, y no lo muestra sin sesión (FR-012).
  - Sin sesión, `/senales` muestra el acceso denegado y no consulta la API.
  - Un `401` en la lista o en el panel limpia la sesión y muestra el acceso denegado, y ningún mensaje de error contiene el JWT (FR-013).
- [X] T015 [P] [US2] Agregar a `tests/pages/senales.test.jsx` tests de la tabla:
  - La consulta enviada es `vigentes=true&page=0&size=20`, sin `sort` (FR-005).
  - La tabla tiene el `caption` "Señales de jugadores subvaluados" y muestra jugador con link a `/jugadores/{id}`, equipo, liga, posición, tipo legible, brecha "+18,00%", precio, precio estimado y "al dd/mm/aaaa".
  - Una señal con un tipo desconocido no se muestra.
  - Con la lista vacía se muestra "No hay señales esta semana".
- [X] T016 [P] [US2] Agregar a `tests/pages/senales.test.jsx` tests de navegación de la lista:
  - "Historial" consulta `vigentes=false&page=0` y los botones exponen `aria-pressed`.
  - Volver a pulsar la vista activa no consulta de nuevo.
  - La paginación ("Página N de M") conserva la vista elegida.
  - Una página fuera de rango vuelve una sola vez a la última válida.
  - Una respuesta vieja no reemplaza a la vigente.
- [X] T017 [P] [US2] Agregar a `tests/pages/senales.test.jsx` tests de compra:
  - Con `201`, el botón se deshabilita mientras envía, se envía `{ jugadorId, cantidad: 1 }` y aparece la confirmación de `describirResultado` con el link "Ver historial".
  - Con `422` y con `409` se muestran `mensaje` y `correlationId`, y el botón vuelve a habilitarse.
  - Con `rol: 'SUPERUSUARIO'` no hay botones de compra (FR-006).
- [X] T018 [P] [US2] Agregar a `tests/pages/senales.test.jsx` un test donde las señales vigentes que se muestran en `/senales` quedan marcadas como vistas y no generan alerta en la consulta siguiente.

### Implementation for User Story 2

- [X] T019 [P] [US2] Crear `src/hooks/useSenales.js`, siguiendo el patrón de `useHistorial`:
  - Consulta con `{ vigentes, page, size: 20 }`, `cambiarVista` y `cambiarPagina`; ninguna consulta si el valor no cambia.
  - Descarta las respuestas obsoletas y recupera una sola vez una página fuera de rango.
  - No consulta sin sesión.
- [X] T020 [P] [US2] Crear `src/hooks/useCompraSenal.js`:
  - `comprar(senal)` envía `POST endpoints.comprar` con `{ jugadorId: senal.jugador.id, cantidad: 1 }` mediante `solicitarProtegida`.
  - Guarda el estado por id de señal (`enviandoId`, `resultado` con `describirResultado`, `error`) y deja un solo resultado visible a la vez (research §6).
- [X] T021 [US2] Crear `src/components/senales/TablaSenales.jsx`:
  - Tabla con `caption` y encabezados, insignia de posición (`posiciones.js`), tipo con su ayuda y botón "Comprar 1 token" / "Comprando..." solo si `puedeComprar`.
  - Hasta 760px oculta equipo, posición y precio estimado (FR-010, FR-011).
- [X] T022 [US2] Crear `src/pages/SenalesPage.jsx` con la parte de la lista:
  - Encabezado "Señales", grupo "Ver señales" con Vigentes/Historial (`filter-chip`, `aria-pressed`), `TablaSenales` y `PaginacionCatalogo` de 20.
  - Estados de carga ("Cargando señales..."), vacío y error (`AvisoError`), y confirmación con `role="status"` y el link "Ver historial".
  - `Escape` vuelve al inicio, y sin sesión muestra `AccesoDenegadoPage`.
  - Marca como vistas las vigentes que muestra (`agregarVistas`).
- [X] T023 [US2] Agregar la ruta `/senales` a `src/router/AppRouter.jsx`.
- [X] T024 [US2] Agregar a `src/components/navegacion/MenuPrincipal.jsx` el ítem "Señales" (subtítulo "Jugadores subvaluados", acción `senales` → `/senales`), visible solo con sesión e incluido para el `SUPERUSUARIO`.

**Checkpoint**: US1 y US2 funcionan juntas: de la alerta a la lista y a la compra en 3 acciones (SC-003).

---

## Phase 5: User Story 3 - Ver si las señales acertaron (Priority: P2)

**Goal**: `/senales` muestra el panel "Aciertos" con la tasa, la diferencia contra el mercado y una tabla por semana.

**Independent Test**: Con MSW, el panel muestra los indicadores y la tabla con los valores recibidos. Con `semanas: []` muestra el estado vacío, y un error en el panel no oculta la lista.

### Tests for User Story 3

- [X] T025 [P] [US3] Agregar a `tests/pages/senales.test.jsx` un test del panel con 3 semanas:
  - Muestra los indicadores "Tasa de acierto" ("50,00%") y "Diferencia contra el mercado" ("-0,12%").
  - La tabla "Aciertos por semana" respeta el orden recibido, muestra variaciones con signo y "17 de 23", y el panel se consulta una sola vez (FR-007).
- [X] T026 [P] [US3] Agregar a `tests/pages/senales.test.jsx` tests de bordes del panel:
  - Con `semanas: []` se muestra "Todavía no hay semanas para medir: las señales se evalúan una semana después de generarse." y no los indicadores en 0.
  - Un `500` en el panel muestra el error con su `correlationId` y la lista sigue visible.
  - Un `500` en la lista no oculta el panel.

### Implementation for User Story 3

- [X] T027 [P] [US3] Crear `src/hooks/useAciertos.js`: hace una consulta al montar, con `cargando` y `error`, y no consulta sin sesión.
- [X] T028 [P] [US3] Crear `src/components/senales/PanelAciertos.jsx`:
  - Indicadores con el estilo de los paneles del portfolio y tabla con `caption` "Aciertos por semana".
  - Clases de ganancia y pérdida para las variaciones y la diferencia, y estado vacío.
  - Hasta 760px oculta la variación del mercado (FR-010).
- [X] T029 [US3] Integrar `PanelAciertos` en `src/pages/SenalesPage.jsx` con sus propios estados de carga y error, independientes de la lista, y agregar los estilos de la lista y del panel en `src/styles/screens.css`, junto a los del historial.

**Checkpoint**: Las tres historias funcionan de forma independiente y juntas.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T030 Revisar la accesibilidad con teclado: menú, alerta (`Escape`, sin robar el foco), Vigentes/Historial, paginación y botones de compra con foco visible (principio V).
- [X] T031 Validar visualmente contra el mockup, a 1280px, 760px y 375px: paleta, tipografías, paneles, sin scroll horizontal. Si aparece alguna desviación nueva, registrarla en `plan.md` (principio I).
- [X] T032 Correr `npm test` tres veces seguidas, `npm run lint` y `npm run build`, y anotar los resultados en la descripción del PR.
- [X] T033 Confirmar que `git diff dev -- formula11/frontend/tests` solo agrega archivos nuevos y que `src/test/handlers.js` solo suma los dos handlers de T002 (sin tests existentes modificados).
- [X] T034 Seguir la validación manual de `quickstart.md` contra el backend de la demo, con un usuario de prueba para la compra o restaurando el backup después.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin dependencias. T003 espera a T001 y T002.
- **Foundational (Phase 2)**: Depende de Setup. T005 implementa lo que T004 prueba. Bloquea todas las historias.
- **US1 (Phase 3)**: Depende de Foundational.
- **US2 (Phase 4)**: Depende de Foundational. Se puede hacer en paralelo con US1, salvo T018, que necesita la alerta de US1.
- **US3 (Phase 5)**: Depende de que exista `SenalesPage` (T022). Los tests y el hook se pueden escribir antes.
- **Polish (Phase 6)**: Depende de las historias elegidas.

### Within Each User Story

- Los tests se escriben primero y tienen que fallar antes de implementar.
- Primero los hooks, después los componentes, después la página o el montaje, después la ruta y el menú.

### Parallel Opportunities

- T001 y T002, en archivos distintos.
- T006 a T009: tests de US1, todos en `alertaSenales.test.jsx`, se escriben en conjunto.
- T019 y T020: hooks de US2 en archivos distintos.
- T027 y T028: hook y componente de US3 en archivos distintos.

---

## Parallel Example: User Story 2

```bash
# Hooks independientes:
Task: "Crear src/hooks/useSenales.js"
Task: "Crear src/hooks/useCompraSenal.js"
```

---

## Implementation Strategy

### MVP First (User Story 1 + User Story 2)

Las dos historias P1 juntas forman el MVP de la demo. La alerta sola lleva a una
ruta vacía, así que conviene cerrar US1 y US2 antes de mostrarla.

1. Phase 1 y Phase 2.
2. US1 (alerta) y validarla con sus tests.
3. US2 (lista y compra) y validar el recorrido alerta → lista → compra.
4. **STOP and VALIDATE**: correr la suite completa y probar a mano contra el backend.

### Incremental Delivery

1. Con US1 y US2 está el MVP de la demo.
2. US3 suma el panel de aciertos sin tocar lo anterior.
3. Polish: accesibilidad, revisión visual y definición de terminado.
