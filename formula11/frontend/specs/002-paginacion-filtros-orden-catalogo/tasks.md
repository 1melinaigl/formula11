---

description: "Task list for Formula11 paginated player catalog"
---

# Tasks: Paginación, filtros y orden del catálogo conectado al backend

**Input**: Design documents from `/specs/002-paginacion-filtros-orden-catalogo/`

**Prerequisites**: `plan.md` y `spec.md`; contrato fuente `backend/specs/003-paginacion-catalogo-jugadores/contracts/openapi.yaml`.

**Tests**: Obligatorios según la especificación; usar Vitest, React Testing Library, user-event, fake timers y MSW.

**Scope**: Solo frontend. No modificar backend, autenticación compartida, detalle remoto, mercado de tokens ni gráficos.

## Phase 1: Foundational contract and test fixtures

**Purpose**: Dejar disponible el contrato paginado en el cliente de pruebas y fijar constantes compartidas antes de cambiar la UI.

- [ ] T001 [P] Crear constantes del catálogo para ligas fijas, tamaño fijo `10`, campos de orden visibles sin `id`, direcciones y valores iniciales en `frontend/src/hooks/` o un módulo de dominio del catálogo, sin derivarlas de `content`.
- [ ] T002 [P] Actualizar `frontend/src/test/handlers.js` para que `GET /api/jugadores` devuelva `{ content, page, size, totalElements, totalPages }` y permita inspeccionar query params en escenarios MSW.
- [ ] T003 [P] Crear fixtures paginados reutilizables para primera página, página posterior, catálogo vacío, sin coincidencias, error con `mensaje`/`correlationId` y página fuera de rango en `frontend/tests/`.
- [ ] T004 Confirmar que `frontend/src/api/clienteApi.js` conserva Bearer, `X-Correlation-Id`, normalización de `ErrorResponse` y limpieza ante `401`; agregar solo tests de regresión si la migración del catálogo lo necesita.

**Checkpoint**: El mock y las constantes representan el contrato backend paginado sin cambiar el backend real.

## Phase 2: User Story 1 - Consultar y navegar páginas (Priority: P1)

**Goal**: El hook consulta una sola página, conserva sus metadatos y permite navegar con índices internos base 0 y numeración visible base 1.

**Independent Test**: Montar el hook o la página con MSW, verificar la consulta inicial `page=0&size=10`, navegar a una página posterior y comprobar controles y metadatos sin contador total visible.

### Tests for User Story 1

- [ ] T005 [P] [US1] Probar en `frontend/tests/hooks/useCatalogo.test.jsx` la consulta inicial con `page=0`, `size=10`, `content` y metadatos paginados.
- [ ] T006 [P] [US1] Probar en `frontend/tests/pages/catalogo.test.jsx` que la UI muestra página visible 1, total de páginas, no muestra conteo total y ofrece botones anterior/siguiente con estados disabled correctos.
- [ ] T007 [P] [US1] Probar en `frontend/tests/pages/catalogo.test.jsx` la navegación a la siguiente y anterior página, conservando el contenido recibido y sin cargar un array completo.
- [ ] T008 [P] [US1] Probar que la consulta inicial y toda navegación envían siempre `size=10`, sin selector de tamaño.

### Implementation for User Story 1

- [ ] T009 [US1] Refactorizar `frontend/src/hooks/useCatalogo.js` para mantener `respuesta` paginada, `page`, `jugadores=content`, `totalElements`, `totalPages`, carga y error sin filtrado/ordenamiento local, fijando `size=10` fuera del estado.
- [ ] T010 [US1] Implementar en `frontend/src/hooks/useCatalogo.js` la serialización de `page`, `size=10` y parámetros activos sobre `endpoints.jugadores`, omitiendo valores vacíos y usando `solicitarProtegida`.
- [ ] T011 [US1] Implementar cambio de página en el hook, manteniendo filtros y orden vigentes y enviando siempre `size=10`.
- [ ] T012 [US1] Crear `frontend/src/components/catalogo/PaginacionCatalogo.jsx` o equivalente con botones anterior/siguiente, página visible base 1, total de páginas y estados disabled accesibles, sin contador de jugadores.
- [ ] T013 [US1] Integrar paginación fija de 10 en `frontend/src/pages/CatalogoPage.jsx` sin alterar la ficha, selección de jugador, navegación al menú ni responsive retro.

**Checkpoint**: El catálogo consulta y muestra una página del backend, navega entre páginas y usa el total del backend como contador.

## Phase 3: User Story 2 - Filtrar y ordenar desde el backend (Priority: P1)

**Goal**: Los controles envían `liga`, `equipo`, `posicion` y `sort` al backend, reiniciando la página a 0 sin filtrar la página en memoria.

**Independent Test**: Cambiar cada filtro, combinarlos y modificar campo/dirección de orden; inspeccionar las requests MSW y comprobar el reset de página.

### Tests for User Story 2

- [ ] T014 [P] [US2] Probar en `frontend/tests/hooks/useCatalogo.test.jsx` que liga, posición y equipo aparecen como query params, que los vacíos se omiten y que se combinan con paginación.
- [ ] T015 [P] [US2] Probar con fake timers en `frontend/tests/hooks/useCatalogo.test.jsx` que el equipo aplica debounce y no dispara una request por cada tecla.
- [ ] T016 [P] [US2] Probar en `frontend/tests/pages/catalogo.test.jsx` que cambiar liga, posición, equipo u orden reinicia a `page=0`, conserva los demás valores y mantiene `size=10`.
- [ ] T017 [P] [US2] Probar en `frontend/tests/pages/catalogo.test.jsx` que el selector de orden solo ofrece `nombre`, `equipo`, `liga`, `posicion` y direcciones `asc`/`desc`, sin opción `ID`, enviando `sort=campo,direccion`.
- [ ] T018 [P] [US2] Probar que la lista conserva el orden y el contenido devueltos por MSW, sin aplicar filtros, sort o paginación local; verificar chips y acción `Limpiar`.

### Implementation for User Story 2

- [ ] T019 [US2] Actualizar `frontend/src/components/catalogo/BarraFiltros.jsx` para retirar búsqueda por nombre y opciones de equipo derivadas de `content`, usar la lista fija de cinco ligas y mantener posición del dominio visual.
- [ ] T020 [US2] Añadir a `BarraFiltros.jsx` el input libre de equipo y selectores de campo/dirección de orden sin `ID` ni selector de tamaño, con labels y foco visible.
- [ ] T021 [US2] Implementar debounce aproximado de 300 ms para equipo en `frontend/src/hooks/useCatalogo.js` o un hook auxiliar, cancelando timers y efectos obsoletos al desmontar o cambiar el valor.
- [ ] T022 [US2] Implementar en el hook el reset a `page=0` para filtros y orden, y exponer `actualizarFiltro`, `actualizarOrden` y `limpiarFiltros` sin modificar el tamaño fijo.
- [ ] T023 [US2] Integrar filtros, chips y orden en `frontend/src/pages/CatalogoPage.jsx`, conservando filtros al cambiar de página y evitando requests duplicadas innecesarias.

**Checkpoint**: Todas las consultas del catálogo reflejan los controles actuales en el backend y ningún resultado se filtra u ordena localmente.

## Phase 4: User Story 3 - Estados de respuesta y página fuera de rango (Priority: P1)

**Goal**: Representar correctamente carga, catálogo vacío, sin coincidencias, errores y recuperación de una página inválida.

**Independent Test**: Responder con cada fixture paginado y verificar el estado visible, `mensaje`, `correlationId` y número de requests de recuperación.

### Tests for User Story 3

- [ ] T024 [P] [US3] Probar en `frontend/tests/pages/catalogo.test.jsx` el estado de carga mientras la respuesta está pendiente, sin mostrar datos anteriores como actuales.
- [ ] T025 [P] [US3] Probar catálogo vacío sin filtros (`totalElements=0`) y sin coincidencias con filtros activos (`totalElements=0`) en estados visuales separados.
- [ ] T026 [P] [US3] Probar error HTTP en `frontend/tests/pages/catalogo.test.jsx`, mostrando `mensaje` y `correlationId` y sin mostrar el token JWT.
- [ ] T027 [P] [US3] Probar respuesta fuera de rango con `content=[]` y `totalElements>0`: una sola recuperación a `totalPages-1`, contenido de la última página y ausencia de bucle.
- [ ] T028 [P] [US3] Probar `401` conservando el comportamiento actual de sesión, ruta protegida y acceso denegado.

### Implementation for User Story 3

- [ ] T029 [US3] Adaptar `frontend/src/components/catalogo/EstadoCatalogo.jsx` para distinguir carga, error, catálogo vacío y sin coincidencias según filtros activos y metadatos, no por longitud de un array completo.
- [ ] T030 [US3] Asegurar que `frontend/src/components/comun/AvisoError.jsx` renderiza `error.api.mensaje` y `error.api.correlationId` de forma visible, conservando el estilo y sin exponer tokens.
- [ ] T031 [US3] Implementar en `useCatalogo.js` la recuperación única a `totalPages - 1` ante página fuera de rango, guardando página válida conocida y evitando reintentos infinitos.
- [ ] T032 [US3] Actualizar `CatalogoPage.jsx` para usar `content`, `totalElements` y `totalPages` al decidir estado y renderizado de lista/ficha, sin mostrar `totalElements`.

**Checkpoint**: Todos los resultados y fallos del backend tienen una representación coherente, trazable y diferenciada.

## Phase 5: Polish & Cross-Cutting Validation

**Purpose**: Validar contrato, regresiones visuales, accesibilidad y calidad del frontend.

- [ ] T033 [P] Actualizar los tests de `frontend/tests/pages/catalogo.test.jsx` existentes que esperan arrays o búsqueda por nombre para usar respuestas paginadas y el alcance de filtros soportado por backend; no eliminar cobertura útil sin reemplazo.
- [ ] T034 [P] Auditar que ninguna modificación del catálogo expone el JWT en DOM, mensajes, atributos, logs o snapshots.
- [ ] T035 [P] Auditar labels, foco visible, teclado, botones disabled, `aria-live` de página/estados, ausencia de contador total y navegación responsive hasta 760px.
- [ ] T036 [P] Revisar que `GET /api/jugadores` conserve `Authorization` y `X-Correlation-Id` mediante el cliente API y que los errores usen el normalizador existente.
- [ ] T037 Ejecutar `npm test` desde `frontend/` y corregir fallos de la feature sin modificar backend.
- [ ] T038 Ejecutar `npm run lint` desde `frontend/` y corregir errores introducidos por la feature.
- [ ] T039 Ejecutar `npm run build` desde `frontend/` y verificar que la aplicación compila con el contrato paginado.
- [ ] T040 [P] Verificar en `frontend/src/styles/components.css` que `.catalog-toolbar` mantiene cinco columnas de campos más la columna de "Limpiar" en una sola fila de escritorio, con `align-items: end`, y conserva el reacomodo responsive hasta 760px.

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 Foundational**: Sin dependencias; fija constantes y mocks.
- **Phase 2 US1**: Depende de Phase 1; establece respuesta paginada, navegación y tamaño fijo 10.
- **Phase 3 US2**: Depende del estado de consulta de US1; agrega filtros, orden y debounce.
- **Phase 4 US3**: Depende de la respuesta paginada de US1 y filtros activos de US2.
- **Phase 5 Polish**: Depende de US1-US3; la suite, lint y build son el cierre.

### User Story Dependencies

- **US1**: Puede comenzar después de T001-T003; no requiere nuevos cambios de sesión.
- **US2**: Requiere T009-T013 para reiniciar y conservar página/metadata correctamente.
- **US3**: Requiere la forma de respuesta de US1 y el estado de filtros de US2.

### Parallel Opportunities

- Phase 1: T001-T003 pueden ejecutarse en paralelo; T004 es una verificación independiente.
- US1: T005-T008 pueden escribirse en paralelo; T009-T011 comparten `useCatalogo` y deben integrarse en secuencia; T012 puede avanzar en paralelo con T009-T011.
- US2: T014-T018 pueden prepararse en paralelo; T019-T021 pueden trabajarse en paralelo si se coordina la API de callbacks; T022-T023 integran el estado.
- US3: T024-T028 pueden escribirse en paralelo; T029-T031 pueden implementarse en paralelo; T032 integra la página.
- Polish: T033-T036 son auditorías paralelizables; T037-T039 deben ejecutarse después de los cambios.

## Implementation Strategy

### MVP

1. Completar Phase 1.
2. Completar US1: primera página, metadatos, navegación y tamaño fijo 10.
3. Completar US2: liga, posición, equipo con debounce, orden y reset a página 0.
4. Completar US3: estados y recuperación fuera de rango.
5. Ejecutar tests, lint y build.

### Definition of Done

La feature está terminada cuando el catálogo usa exclusivamente el formato paginado del backend, todas las requests envían `size=10`, los filtros y el orden viajan como query params, la UI muestra navegación con numeración base 1 sin conteo total de jugadores, el equipo no genera una request por tecla, los estados están diferenciados, la recuperación fuera de rango no entra en bucle, los tests Vitest/MSW pasan y `npm run lint` y `npm run build` son correctos.

## Notes

- `[P]` indica que la tarea puede ejecutarse en paralelo sin depender de trabajo incompleto en el mismo archivo.
- `[US1]` a `[US3]` enlazan las tareas con historias de la especificación.
- Las query params del backend usan índices base 0; solo la presentación de la UI usa numeración base 1.
- La búsqueda por nombre se retira porque no existe en el contrato backend paginado; no se filtra localmente sobre una sola página.
