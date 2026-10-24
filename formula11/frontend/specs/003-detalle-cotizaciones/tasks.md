---

description: "Task list for Formula11 player detail with quote history"
---

# Tasks: Detalle de jugador con evoluciÃ³n de cotizaciones

**Input**: Design documents from `/specs/003-detalle-cotizaciones/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/frontend-api.md, quickstart.md

**Tests**: Obligatorios; Vitest, React Testing Library, user-event y MSW.

**Organization**: Tasks are grouped by user story so each story can be implemented and tested independently.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirmar la estructura existente y preparar los puntos de integraciÃ³n de la feature.

- [X] T001 [P] Documentar en `specs/003-detalle-cotizaciones/contracts/frontend-api.md` las formas canÃ³nicas de jugador, cotizaciones, estrategias y errores usadas por las tareas.
- [X] T002 [P] Auditar `src/api/clienteApi.js` y `src/context/SesionContext.jsx` para reutilizar `solicitarProtegida`, `X-Correlation-Id`, limpieza ante `401` y el rol de sesiÃ³n sin duplicar transporte.
- [X] T003 [P] Verificar que `recharts` estÃ© declarado en `package.json` y que `src/router/AppRouter.jsx` mantenga la carga diferida de `src/pages/DetalleJugadorPage.jsx`.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Completar contratos, normalizaciÃ³n y fixtures compartidos antes de implementar las historias.

**âš ï¸ CRITICAL**: No iniciar trabajo de historias hasta completar esta fase.

- [X] T004 Agregar `estrategias()` a `src/api/endpoints.js` para centralizar `GET /api/estrategias` segÃºn el contrato de Entrega 2.
- [X] T005 [P] Definir en `src/components/cotizaciones/cotizaciones.js` la validaciÃ³n compartida de cÃ³digos `RENDIMIENTO_POSICION` y `CONSISTENCIA`, la semÃ¡ntica de `activa` y el error explÃ­cito cuando no existe exactamente una estrategia activa.
- [X] T006 [P] Ajustar `normalizarCotizaciones` en `src/components/cotizaciones/cotizaciones.js` para consumir Ãºnicamente `{ jugadorId, estrategia, cotizaciones }`, conservar `jugadorId`, `estrategia`, `version`, `score` y `variacion`, y descartar entradas sin fecha o sin precio numÃ©rico.
- [X] T007 [P] Agregar en `src/test/handlers.js` el handler MSW de `GET /api/estrategias` con ambas estrategias y exactamente una propiedad `activa: true`.
- [X] T008 [P] Crear fixtures y helpers de sesiÃ³n con roles `USUARIO` y `SUPERUSUARIO` en `tests/pages/detalleJugador.test.jsx` o un helper de pruebas compartido, sin exponer tokens en aserciones ni logs.
- [ ] T009 [P] Agregar pruebas unitarias de contrato y normalizaciÃ³n para `GET /api/estrategias`, respuesta `{ jugadorId, estrategia, cotizaciones }`, fechas `YYYY-MM-DD`, porcentajes y errores en `tests/components/cotizaciones.test.js`.

**Checkpoint**: contratos y fixtures compartidos listos; las historias pueden implementarse en paralelo.

---

## Phase 3: User Story 1 - Ver la ficha y la cotizaciÃ³n vigente (Priority: P1) ðŸŽ¯ MVP

**Goal**: Permitir que un usuario autenticado vea la ficha y la cotizaciÃ³n de la estrategia activa, mientras el superusuario puede consultar la alternativa.

**Independent Test**: Con MSW, la pantalla `/jugadores/7` muestra la ficha y una tarjeta de la estrategia activa para `USUARIO`; para `SUPERUSUARIO` permite la consulta adicional; el retorno por botÃ³n o `Escape` vuelve a `/catalogo`.

### Tests for User Story 1

- [X] T010 [P] [US1] Agregar pruebas de `GET /api/jugadores/:id` y `GET /api/jugadores/:id/cotizaciones` en `tests/pages/detalleJugador.test.jsx`, comprobando headers protegidos y la forma contractual de la respuesta.
- [X] T011 [P] [US1] Agregar pruebas de autorizaciÃ³n en `tests/pages/detalleJugador.test.jsx` que exijan una sola estrategia activa para `USUARIO`, ambas estrategias para `SUPERUSUARIO` y `403` para una estrategia alternativa no permitida.
- [X] T012 [P] [US1] Agregar pruebas de la tarjeta en `tests/pages/detalleJugador.test.jsx` para precio, score, fecha, variaciÃ³n positiva/negativa y el texto `Sin cotizaciÃ³n anterior` cuando `variacion` es `null`.

### Implementation for User Story 1

- [X] T013 [US1] Actualizar `src/hooks/useDetalleJugador.js` para conservar la consulta protegida de ficha y cotizaciones en paralelo, pedir solo la activa para `USUARIO`, pedir ambas para `SUPERUSUARIO` y mantener errores separados.
- [X] T014 [US1] Integrar en `src/hooks/useDetalleJugador.js` la consulta protegida de `GET /api/estrategias` para `SUPERUSUARIO`, validando una estrategia activa y exponiendo un error especÃ­fico sin ocultar la ficha.
- [X] T015 [US1] Completar `src/components/cotizaciones/ResumenCotizacion.jsx` para mostrar una tarjeta por estrategia permitida, variaciÃ³n porcentual con color semÃ¡ntico y `Sin cotizaciÃ³n anterior` cuando corresponda.
- [X] T016 [US1] Actualizar `src/pages/DetalleJugadorPage.jsx` para mostrar la ficha, el panel de cotizaciones y el indicador de estrategia activa, conservar `Cargando jugador...`, `mensaje` y `correlationId`, y volver a `/catalogo` con botÃ³n o `Escape`.
- [X] T017 [US1] Mantener en `src/components/catalogo/FichaJugador.jsx` el enlace `Ver cotizaciones` hacia `/jugadores/:id` sin introducir llamadas de API en el componente.

**Checkpoint**: US1 es funcional y comprobable de forma independiente para ambos roles.

---

## Phase 4: User Story 2 - Ver la evoluciÃ³n en el tiempo (Priority: P1)

**Goal**: Mostrar la evoluciÃ³n histÃ³rica en grÃ¡fico y tabla, con comparaciÃ³n restringida al superusuario y estrategia activa identificada.

**Independent Test**: Con historiales de `RENDIMIENTO_POSICION` y `CONSISTENCIA`, el superusuario obtiene dos lÃ­neas y una tabla descendente; el usuario comÃºn ve Ãºnicamente la serie activa.

### Tests for User Story 2

- [X] T018 [P] [US2] Agregar pruebas de series por fecha, estrategias, porcentajes y formateo sin desplazamiento de zona horaria en `tests/components/cotizaciones.test.js`.
- [ ] T019 [P] [US2] Agregar pruebas de `GraficoEvolucion` en `tests/components/cotizaciones.test.js` que exijan una lÃ­nea por estrategia permitida, leyenda/tooltip en crÃ©ditos y marca de estrategia activa en el superusuario.
- [X] T020 [P] [US2] Agregar pruebas del historial ordenado, versiÃ³n, score y alternativa textual en `tests/pages/detalleJugador.test.jsx`.

### Implementation for User Story 2

- [X] T021 [US2] Completar `src/components/cotizaciones/GraficoEvolucion.jsx` con una serie por estrategia autorizada, colores estables, leyenda cuando haya comparaciÃ³n, tooltip en crÃ©ditos y estado visual de la estrategia activa.
- [X] T022 [US2] Completar `src/components/cotizaciones/TablaCotizaciones.jsx` como historial desplegable ordenado de mÃ¡s reciente a mÃ¡s antiguo, mostrando fecha, estrategia, versiÃ³n, precio y score.
- [X] T023 [US2] Revisar `src/components/cotizaciones/cotizaciones.js` para mantener series ascendentes para el grÃ¡fico, tabla descendente para lectura y fechas `YYYY-MM-DD` formateadas como `dd/mm/aaaa` sin usar `Date`.
- [X] T024 [US2] Verificar en `src/router/AppRouter.jsx` y `src/pages/DetalleJugadorPage.jsx` que Recharts permanezca en carga diferida y no se incluya en el bundle inicial.

**Checkpoint**: US1 y US2 son funcionales de forma independiente y la tabla cubre la alternativa textual del grÃ¡fico.

---

## Phase 5: User Story 3 - Estados de la pantalla (Priority: P1)

**Goal**: Cubrir carga, vacÃ­o, errores parciales, jugador inexistente y ausencia de sesiÃ³n con mensajes trazables.

**Independent Test**: Cada estado puede provocarse con handlers MSW aislados y produce el mensaje, panel y acceso esperados sin filtrar el token.

### Tests for User Story 3

- [X] T025 [P] [US3] Agregar pruebas de carga, jugador sin cotizaciones, error de cotizaciones, error `404` del jugador y ausencia de sesiÃ³n en `tests/pages/detalleJugador.test.jsx`.
- [X] T026 [P] [US3] Agregar pruebas de `401`, `403` y error de `GET /api/estrategias` en `tests/pages/detalleJugador.test.jsx`, verificando limpieza de sesiÃ³n, `mensaje` y `correlationId`.

### Implementation for User Story 3

- [X] T027 [US3] Ajustar `src/pages/DetalleJugadorPage.jsx` y `src/components/comun/AvisoError.jsx` para distinguir error de ficha, error de cotizaciones y error de estrategia activa, manteniendo la ficha cuando el fallo es parcial.
- [X] T028 [US3] Ajustar `src/hooks/useDetalleJugador.js` para cancelar actualizaciones despuÃ©s de desmontar o cambiar de jugador y para no iniciar consultas protegidas cuando `usuario` es nulo.
- [X] T029 [US3] Completar `src/test/handlers.js` con respuestas controlables para `401`, `403`, `404`, errores de proveedor y ausencia de cotizaciones, manteniendo `correlationId` en cada error.

**Checkpoint**: Las tres historias tienen estados de Ã©xito, vacÃ­o y excepciÃ³n cubiertos por MSW.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Validar calidad, accesibilidad, contrato, rendimiento y documentaciÃ³n de la entrega.

- [X] T030 [P] Revisar `src/styles/screens.css` y `src/styles/components.css` para responsive a 760px, foco visible, contraste de variaciones y consistencia con `design/formula11-mockup-retro-v3.html`.
- [X] T031 [P] Revisar `src/pages/DetalleJugadorPage.jsx`, `src/components/cotizaciones/TablaCotizaciones.jsx` y `src/components/cotizaciones/GraficoEvolucion.jsx` para navegaciÃ³n por teclado, labels asociados y alternativa textual accesible.
- [X] T032 [P] Actualizar `specs/003-detalle-cotizaciones/quickstart.md` si cambian comandos, fixtures o escenarios despuÃ©s de implementar T018 y T029.
- [X] T033 Ejecutar `npm run test`, `npm run lint` y `npm run build` desde `frontend/` y resolver cualquier regresiÃ³n de la feature.
- [ ] T034 Ejecutar manualmente los escenarios de `specs/003-detalle-cotizaciones/quickstart.md` y registrar cualquier desviaciÃ³n contractual o visual en el plan antes de cerrar la feature.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sin dependencias; T001-T003 pueden ejecutarse en paralelo.
- **Foundational (Phase 2)**: Depende de Phase 1; T004-T009 preparan endpoint, normalizaciÃ³n y fixtures compartidos.
- **User Story 1 (Phase 3)**: Depende de Phase 2; es el MVP y habilita la ficha/cotizaciÃ³n vigente.
- **User Story 2 (Phase 4)**: Depende de Phase 2; puede ejecutarse en paralelo con US1, aunque reutiliza sus datos y componentes.
- **User Story 3 (Phase 5)**: Depende de Phase 2; puede ejecutarse en paralelo con US1/US2 y endurece sus estados.
- **Polish (Phase 6)**: Depende de las historias que se quieran entregar; T033-T034 son la puerta de cierre.

### User Story Dependencies

- **US1 (P1)**: Sin dependencias de otras historias despuÃ©s de Phase 2.
- **US2 (P1)**: Sin dependencia funcional de US1 despuÃ©s de Phase 2; comparte normalizadores y contratos.
- **US3 (P1)**: Sin dependencia funcional de US1/US2 despuÃ©s de Phase 2; valida estados transversales.

### Parallel Opportunities

- **Phase 1**: T001, T002 y T003.
- **Phase 2**: T005-T009 despuÃ©s de T004 cuando una tarea requiera el nuevo endpoint.
- **US1**: T010-T012 en paralelo; luego T013-T017 con coordinaciÃ³n sobre el hook/pÃ¡gina.
- **US2**: T018-T020 en paralelo; T021-T024 pueden dividirse por componente.
- **US3**: T025-T026 en paralelo; T027-T029 pueden dividirse por pÃ¡gina, hook y handlers.
- **Polish**: T030-T032 en paralelo antes de T033.

## Implementation Strategy

1. **MVP**: completar Phase 1, Phase 2 y US1 para entregar ficha, cotizaciÃ³n activa,
   roles, navegaciÃ³n y errores bÃ¡sicos.
2. **Incremental delivery**: aÃ±adir US2 para historial/grÃ¡fico y luego US3 para todos
   los estados de pantalla.
3. **Hardening**: ejecutar la fase Polish, validar el contrato de Entrega 2 y cerrar
   solo cuando tests, lint, build y quickstart pasen.
