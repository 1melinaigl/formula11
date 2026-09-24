---

description: "Task list for Formula11 registration, login and player catalog"
---

# Tasks: Registro, login y catálogo de jugadores

**Input**: Design documents from `/specs/001-registro-login-catalogo/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`

**Tests**: Obligatorios según la especificación y la constitución; usar Vitest, React Testing Library, user-event y MSW.

**Organization**: Las tareas están agrupadas por historia de usuario para permitir validación independiente. Registro e ingreso forman el MVP; catálogo se entrega después.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Instalar dependencias y reemplazar la configuración de demo por la base del frontend Formula11.

- [X] T001 Actualizar `package.json` con `react-router-dom`, `@fontsource/bebas-neue` y `@fontsource/barlow`; instalar las dependencias de test `vitest`, `@testing-library/react`, `@testing-library/jest-dom`, `@testing-library/user-event`, `msw` y `jsdom`; agregar los scripts `"test": "vitest run"` y `"test:watch": "vitest"`, conservando React 19, Vite 8 y ESLint.
- [X] T002 [P] Configurar el proxy `/api` hacia `http://localhost:8080` en `vite.config.js` sin modificar el backend.
- [X] T003 [P] Configurar Vitest con entorno jsdom, setup de jest-dom y descubrimiento de tests en `vitest.config.js` y `src/test/setup.js`.
- [X] T004 [P] Crear `src/styles/base.css`, `src/styles/layout.css`, `src/styles/components.css` y `src/styles/screens.css`, importar fuentes locales y conectarlos desde `src/index.css`.
- [X] T005 [P] Eliminar imports y referencias visuales del scaffold de Vite en `src/App.jsx`, `src/App.css`, `src/index.css` y assets que ya no formen parte de Formula11.

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Construir los límites compartidos de API, sesión, routing, mocking y estilos que bloquean todas las historias.

- [X] T006 Crear `src/api/endpoints.js` con únicamente `POST /api/usuarios/registro`, `POST /api/usuarios/login` y `GET /api/jugadores`.
- [X] T007 Crear `src/api/erroresApi.js` para normalizar errores a `{ mensaje, timestamp, correlationId, status }`, tomando `correlationId` del cuerpo, luego del header y finalmente del UUID enviado.
- [X] T008 Crear `src/api/clienteApi.js` como único límite HTTP; cada request debe generar `X-Correlation-Id` con `crypto.randomUUID()`, agregar `Content-Type` cuando haya JSON y agregar `Authorization: Bearer` solo con sesión activa.
- [X] T009 [P] Crear `src/test/handlers.js` y `src/test/server.js` con handlers MSW base para éxito, `400`, `401`, `409` y catálogo vacío, sin cuentas demo ni jugadores hardcodeados en la aplicación.
- [X] T010 Crear `src/context/SesionContext.jsx` para estados `restaurando`, `anonima` y `autenticada`, persistencia de token y usuario mínimo en `sessionStorage`, restauración tras recarga y limpieza ante logout o `401` protegido.
- [X] T011 Crear `src/hooks/useSesion.js` como único hook público del Context, exponiendo usuario, estado de sesión, carga, ingreso, registro y cierre de sesión sin exponer el token a componentes.
- [X] T012 [P] Crear `src/components/comun/EstadoCarga.jsx`, `src/components/comun/AvisoError.jsx` y `src/components/comun/EstadoVacio.jsx` con labels, roles accesibles y mensajes en español.
- [X] T013 Crear `src/router/AppRouter.jsx` y `src/App.jsx` con rutas `/`, `/ingresar`, `/registro`, `/cuenta-creada` y `/catalogo`, guardando `/catalogo` para sesiones válidas y derivando sesiones ausentes o vencidas a acceso denegado.
- [X] T014 [P] Portar la cancha de fondo, scanlines, variables de paleta, Bebas Neue/Barlow, foco visible, paneles y botones inclinados del mockup a `src/styles/base.css`, `src/styles/layout.css`, `src/styles/components.css` y `src/styles/screens.css`, manteniendo el breakpoint de 760px.

**Checkpoint**: API, mocking, sesión, routing y base visual listos; las historias pueden implementarse sin tocar contratos backend.

## Phase 3: User Story 1 - Menú y estado de sesión (Priority: P1)

**Goal**: Permitir recorrer el menú con teclado y mostrar correctamente sesión anónima o activa.

**Independent Test**: Renderizar la aplicación con MSW, recorrer opciones con flechas, activar con Enter, volver con Esc y verificar `Sin sesión`, chip de nombre y opciones `PRÓXIMAMENTE`.

### Tests for User Story 1

- [X] T015 [P] [US1] Probar navegación de menú con flechas, Enter y Esc, foco visible y bloqueo de Ranking/Mercado en `tests/pages/inicio.test.jsx`.
- [X] T016 [P] [US1] Probar encabezado con `Sin sesión`, nombre de sesión activa y acción `Cerrar sesión` en `tests/components/navegacion.test.jsx`.

### Implementation for User Story 1

- [X] T017 [P] [US1] Crear `src/components/navegacion/Encabezado.jsx` con logo, breadcrumb, chip de sesión y acción de cierre sin renderizar credenciales.
- [X] T018 [P] [US1] Crear `src/components/navegacion/MenuPrincipal.jsx` con selección por teclado, opciones condicionadas por sesión y etiquetas `PRÓXIMAMENTE` no activables.
- [X] T019 [US1] Crear `src/pages/InicioPage.jsx` e integrar `Encabezado` y `MenuPrincipal` con `/`, `/ingresar`, `/registro`, `/catalogo` y logout.
- [X] T020 [US1] Completar estilos de menú, encabezado y estados seleccionados en `src/styles/components.css` y `src/styles/screens.css`, incluyendo foco visible y comportamiento hasta 760px.

**Checkpoint**: El menú es demostrable sin autenticación y con una sesión simulada; no depende todavía de una cuenta real.

## Phase 4: User Story 2 - Ingreso de usuario (Priority: P1) 🎯 MVP

**Goal**: Permitir ingreso válido, mostrar errores `400/401` con mensaje y `correlationId`, persistir sesión y dirigir al catálogo protegido.

**Independent Test**: Con handlers MSW, enviar credenciales válidas, inválidas y con formato incorrecto; verificar estado de carga, errores, redirección, headers, persistencia y que un `401` de login no limpie otra sesión.

### Tests for User Story 2

- [X] T021 [P] [US2] Probar `POST /api/usuarios/login`, body `{ email, password }`, headers `X-Correlation-Id` y `Authorization` condicional en `tests/api/cliente-login.test.js`.
- [X] T022 [P] [US2] Probar validación de email válido y password de 8-72 caracteres, bloqueo de envío inválido y errores `400/401` con `mensaje` y `correlationId` en `tests/pages/ingreso.test.jsx`.
- [X] T023 [P] [US2] Probar restauración desde `sessionStorage`, redirección posterior al ingreso, ausencia del token en el DOM y manejo de carga en `tests/hooks/sesion.test.jsx`.

### Implementation for User Story 2

- [X] T024 [P] [US2] Crear `src/components/formularios/CampoFormulario.jsx`, `src/components/formularios/AvisoServicio.jsx` y `src/components/formularios/FormularioIngreso.jsx` con labels asociados, errores bajo campo y password nunca visible en texto.
- [X] T025 [US2] Implementar `ingresar` en `src/context/SesionContext.jsx` y `src/hooks/useSesion.js` usando `POST /api/usuarios/login`; mostrar carga, conservar `401` como aviso local y guardar solo la sesión interna necesaria.
- [X] T026 [US2] Crear `src/pages/AccesoPage.jsx` para la pestaña Ingresar, navegación a Registro, redirección válida al catálogo y retorno con Esc.
- [X] T027 [US2] Actualizar `src/router/AppRouter.jsx` y `src/main.jsx` para montar `SesionContext` y garantizar que un reload restaure antes de resolver rutas protegidas.
- [X] T028 [US2] Completar estilos de formulario, tabs, avisos y estados de carga en `src/styles/components.css` y `src/styles/screens.css` conforme al mockup retro.

**Checkpoint MVP ingreso**: Una cuenta existente puede ingresar, conservar sesión tras recarga y ver errores trazables sin exponer el JWT.

## Phase 5: User Story 3 - Registro de cuenta (Priority: P1) 🎯 MVP

**Goal**: Permitir crear una cuenta con validación DTO, mostrar confirmación sin token y dejar la sesión iniciada.

**Independent Test**: Enviar registro válido, límites inválidos y email duplicado; verificar errores bajo campos, aviso general con `correlationId`, datos de cuenta creada y botón `Ir al catálogo`.

### Tests for User Story 3

- [X] T029 [P] [US3] Probar `POST /api/usuarios/registro`, body `{ nombre, email, password }`, respuesta `201`, headers de correlación y errores `400/409` en `tests/api/cliente-registro.test.js`.
- [X] T030 [P] [US3] Probar nombre de 1-100 caracteres, email válido de máximo 254, password de 8-72, errores por campo y bloqueo de envío inválido en `tests/pages/registro.test.jsx`.
- [X] T031 [P] [US3] Probar confirmación con id, nombre, email y fecha de registro, ausencia del token y navegación `Ir al catálogo` en `tests/pages/cuenta-creada.test.jsx`.

### Implementation for User Story 3

- [X] T032 [P] [US3] Crear `src/components/formularios/FormularioRegistro.jsx` reutilizando `CampoFormulario` con las reglas exactas: nombre 1-100, email válido máximo 254 y password 8-72.
- [X] T033 [US3] Implementar `registrar` en `src/context/SesionContext.jsx` y `src/hooks/useSesion.js` usando `POST /api/usuarios/registro`, guardando sesión tras `201` y propagando errores normalizados.
- [X] T034 [US3] Completar `src/pages/AccesoPage.jsx` para la pestaña Crear cuenta y crear `src/pages/CuentaCreadaPage.jsx` con id, nombre, email, fecha y botón `Ir al catálogo`, sin token.
- [X] T035 [US3] Integrar `/registro` y `/cuenta-creada` en `src/router/AppRouter.jsx`, manteniendo la protección de `/catalogo` y el retorno por Esc.
- [X] T036 [US3] Completar estilos de registro, confirmación y avisos de conflicto en `src/styles/screens.css` y `src/styles/components.css`.

**Checkpoint MVP registro**: Una persona nueva puede registrarse, recibir confirmación segura, conservar sesión y acceder al catálogo protegido.

## Phase 6: User Story 4 - Catálogo de jugadores (Priority: P1)

**Goal**: Consultar una vez el catálogo protegido y permitir búsqueda, filtros, ficha, estados vacíos y vista responsive.

**Independent Test**: Con MSW devolver jugadores, colección vacía, `401` y datos desconocidos; verificar una sola petición, filtrado local, chips, contador, ficha y alternancia lista/ficha a 760px.

### Tests for User Story 4

- [ ] T037 [P] [US4] Probar `GET /api/jugadores` con Bearer y `X-Correlation-Id`, catálogo vacío, `401`, limpieza de sesión y pantalla de acceso denegado en `tests/api/cliente-catalogo.test.js`.
- [ ] T038 [P] [US4] Probar búsqueda por nombre sin mayúsculas, filtros combinados por liga/posición/equipo, chips, contador y `Limpiar` en `tests/hooks/catalogo.test.js`.
- [ ] T039 [P] [US4] Probar lista, ficha, posiciones `POR/DEF/MED/DEL`, posición desconocida neutra y estados catálogo vacío/sin coincidencias en `tests/pages/catalogo.test.jsx`.
- [ ] T040 [P] [US4] Probar alternancia lista/ficha con ancho de 760px, botón volver, teclado y foco visible en `tests/components/catalogo-responsive.test.jsx`.

### Implementation for User Story 4

- [ ] T041 [P] [US4] Crear `src/hooks/useCatalogo.js` para cargar una sola vez `GET /api/jugadores`, mantener carga/error/datos, filtrar en memoria y llamar a limpieza de sesión ante `401`.
- [ ] T042 [P] [US4] Crear `src/components/catalogo/BarraFiltros.jsx` con buscador, filtros de liga/posición/equipo, chips activos y acción `Limpiar`.
- [ ] T043 [P] [US4] Crear `src/components/catalogo/ListaJugadores.jsx`, `src/components/catalogo/FichaJugador.jsx` y `src/components/catalogo/PosicionJugador.jsx` con datos solo del backend y estilo neutro para posiciones desconocidas.
- [ ] T044 [P] [US4] Crear `src/components/catalogo/EstadoCatalogo.jsx` y `src/pages/AccesoDenegadoPage.jsx` para diferenciar carga, error, catálogo vacío, sin coincidencias y `401` con mensaje/correlationId.
- [ ] T045 [US4] Crear `src/pages/CatalogoPage.jsx` usando `useCatalogo`, contador, filtros locales, selección de jugador y alternancia lista/ficha responsive.
- [ ] T046 [US4] Integrar guardia, redirección a acceso denegado, botón para ingresar y cleanup de sesión en `src/router/AppRouter.jsx` y `src/context/SesionContext.jsx`.
- [ ] T047 [US4] Portar layout de catálogo, chips, ficha, estados vacíos y media query de 760px a `src/styles/layout.css`, `src/styles/components.css` y `src/styles/screens.css`.

**Checkpoint**: El usuario autenticado puede consultar y filtrar el catálogo real sin endpoints inventados ni datos demo.

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Validar fidelidad, accesibilidad, seguridad y definición de terminado de toda la entrega.

- [ ] T048 [P] Auditar que ningún componente haga `fetch`, que todas las requests pasen por `src/api/clienteApi.js` y que los únicos endpoints estén documentados en `src/api/endpoints.js`.
- [ ] T049 [P] Auditar ausencia de token en DOM, textos de UI, errores serializados y logs mediante tests en `tests/security/session-token.test.js`.
- [ ] T050 [P] Ejecutar revisión de accesibilidad de labels, foco, teclado, roles de avisos y navegación Esc en `tests/accessibility/navigation.test.jsx`.
- [ ] T051 [P] Comparar desktop y responsive con `design/formula11-mockup-retro-v3.html`, registrar cualquier desviación justificada en `specs/001-registro-login-catalogo/plan.md` y ajustar `src/styles/*.css`.
- [ ] T052 Ejecutar `npm run lint`, `npm run build` y `npm run test`; corregir errores de la feature sin modificar ni borrar tests existentes en `package.json`, `src/` y `tests/`.
- [ ] T053 [P] Ejecutar todos los escenarios de `specs/001-registro-login-catalogo/quickstart.md` con backend local y documentar cualquier dependencia externa pendiente en `specs/001-registro-login-catalogo/quickstart.md`.

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 Setup**: No dependencies; instala librerías y reemplaza la base del scaffold.
- **Phase 2 Foundational**: Depende de Phase 1 y bloquea todas las historias.
- **Phase 3 US1**: Depende de Phase 2; habilita navegación y estados de sesión.
- **Phase 4 US2**: Depende de Phase 2 y usa el Context/router; puede iniciar tras T011-T013, pero el MVP se valida junto con US1.
- **Phase 5 US3**: Depende de Phase 2 y reutiliza los formularios de US2; completar US2 primero reduce duplicación.
- **Phase 6 US4**: Depende de la sesión de US2/US3 y de Phase 2; se entrega después del MVP.
- **Phase 7 Polish**: Depende de las historias que se quieran entregar; para la entrega completa depende de US1-US4.

### User Story Dependencies

- **US1**: Después de Foundational; no requiere backend real.
- **US2**: Después de Foundational; usa `SesionContext` y router, pero es validable con MSW.
- **US3**: Después de US2 para reutilizar `AccesoPage` y formularios; su endpoint se prueba independientemente.
- **US4**: Después de US2 o US3 porque `/catalogo` exige sesión; no añade dependencias de endpoint fuera del contrato.

### Parallel Opportunities

- Setup: T002, T003, T004 y T005 pueden ejecutarse en paralelo después de T001.
- Foundational: T006-T009 y T012-T014 pueden repartirse; T010-T011 dependen del cliente API T006-T008.
- US1: T015-T016 y T017-T018 son paralelizables; T019-T020 integran sus resultados.
- US2: T021-T023 y T024 pueden ejecutarse en paralelo; T025-T028 integran Context, página y estilos.
- US3: T029-T031 y T032 pueden ejecutarse en paralelo; T033-T036 integran registro y confirmación.
- US4: T037-T040 y T041-T044 pueden ejecutarse en paralelo; T045-T047 integran página, guardia y estilos.
- Polish: T048-T051 y T053 pueden ejecutarse en paralelo; T052 debe ejecutarse tras los ajustes.

## Parallel Example: User Story 2

```text
Task: "T021 [US2] Probar cliente de login en tests/api/cliente-login.test.js"
Task: "T022 [US2] Probar validación y errores en tests/pages/ingreso.test.jsx"
Task: "T023 [US2] Probar persistencia y ausencia del token en tests/hooks/sesion.test.jsx"
Task: "T024 [P] [US2] Crear componentes de ingreso en src/components/formularios/"
```

## Parallel Example: User Story 3

```text
Task: "T029 [US3] Probar contrato de registro en tests/api/cliente-registro.test.js"
Task: "T030 [US3] Probar límites del formulario en tests/pages/registro.test.jsx"
Task: "T031 [US3] Probar confirmación segura en tests/pages/cuenta-creada.test.jsx"
Task: "T032 [P] [US3] Crear FormularioRegistro.jsx"
```

## Implementation Strategy

### MVP: menú, ingreso y registro

1. Completar Phase 1: Setup.
2. Completar Phase 2: Foundational.
3. Completar US1 para hacer navegable la experiencia.
4. Completar US2 y validar ingreso real con MSW.
5. Completar US3 y validar registro, confirmación y sesión persistente.
6. Ejecutar T048-T052; detenerse para demo antes de iniciar catálogo.

### Incremental Delivery

1. Setup + Foundational: base ejecutable.
2. US1: menú y navegación.
3. US2 + US3: MVP de autenticación y registro.
4. US4: catálogo protegido con filtros locales.
5. Polish: accesibilidad, seguridad, fidelidad visual y quickstart.

## Notes

- `[P]` indica que la tarea puede ejecutarse en paralelo sin depender de trabajo incompleto en el mismo archivo.
- `[US1]` a `[US4]` enlazan cada tarea con una historia de la especificación.
- Los nombres de dominio deben conservar español sin acentos ni `ñ` en identificadores.
- No se modifica ni elimina ningún test existente sin permiso explícito.
