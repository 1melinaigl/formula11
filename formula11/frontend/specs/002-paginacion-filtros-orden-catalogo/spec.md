# Feature Specification: Paginación, filtros y orden del catálogo conectado al backend

**Feature Branch**: `002-paginacion-filtros-orden-catalogo`

**Created**: 2026-10-07

**Status**: Draft

**Input**: User description: "Conectar el catálogo de jugadores del frontend al endpoint paginado del backend, enviando filtros y orden mediante query params, usando siempre nueve elementos por página y mostrando solo la navegación de páginas."

**Backend contract source**: `backend/specs/003-paginacion-catalogo-jugadores/contracts/openapi.yaml` y su `spec.md` asociado. El frontend no redefine ese contrato.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar el catálogo por páginas (Priority: P1)

Como usuario autenticado, quiero consultar una página del catálogo y navegar entre páginas para revisar los jugadores sin descargar toda la colección.

**Why this priority**: El endpoint ya no devuelve una lista completa; la interfaz debe consumir sus metadatos y permitir recorrer el catálogo.

**Independent Test**: Se puede montar un mock paginado con varias páginas, abrir el catálogo y comprobar la primera consulta, la navegación anterior/siguiente, la página actual y el total de páginas.

**Acceptance Scenarios**:

1. **Given** una sesión válida y una respuesta paginada del backend, **When** se abre el catálogo, **Then** el frontend solicita `page=0` con `size=9` y muestra `content`, la página actual y el total de páginas, sin mostrar `totalElements`.
2. **Given** una página posterior a la primera, **When** el usuario pulsa "Siguiente", **Then** se solicita únicamente la página siguiente y se actualiza la lista sin descargar el catálogo completo; la interfaz muestra la numeración iniciando en 1.
3. **Given** la primera página, **When** el usuario pulsa "Anterior", **Then** el control está deshabilitado y no se envía una página negativa.
4. **Given** la última página, **When** el usuario pulsa "Siguiente", **Then** el control está deshabilitado y no se realiza una solicitud fuera del rango conocido.
5. **Given** el catálogo abierto, **When** el usuario navega entre páginas, **Then** todas las solicitudes usan `size=9` y no existe un control para cambiar ese tamaño.
6. **Given** que el backend responde una página fuera de rango con `content` vacío y `totalElements>0`, **When** el frontend recibe esa respuesta, **Then** solicita una sola vez la última página válida (`totalPages - 1`) y muestra sus resultados.

---

### User Story 2 - Filtrar el catálogo desde el backend (Priority: P1)

Como usuario autenticado, quiero filtrar por liga, equipo y posición para que los resultados y sus totales provengan del catálogo real filtrado por el backend.

**Why this priority**: El filtrado local sobre una página incompleta produciría resultados y conteos incorrectos.

**Independent Test**: Se puede cambiar cada filtro y una combinación de filtros, inspeccionar las query params enviadas y comprobar que la respuesta paginada reemplaza el contenido visible sin aplicar un segundo filtrado local.

**Acceptance Scenarios**:

1. **Given** el catálogo abierto, **When** el usuario selecciona una liga, **Then** se envía `liga` con exactamente uno de los cinco valores del dominio y se vuelve a `page=0`.
2. **Given** el catálogo abierto, **When** el usuario selecciona una posición, **Then** se envía `posicion` y se vuelve a `page=0`.
3. **Given** el catálogo abierto, **When** el usuario escribe un equipo, **Then** el valor se envía como `equipo` después de un debounce o una acción de confirmación, sin realizar una request por cada tecla.
4. **Given** más de un filtro activo, **When** el usuario cambia cualquiera de ellos, **Then** todos los filtros activos se envían juntos, se vuelve a `page=0` y se reemplaza la lista con la respuesta del backend.
5. **Given** filtros activos, **When** el usuario pulsa "Limpiar", **Then** se eliminan `liga`, `equipo` y `posicion`, se vuelve a `page=0` y se solicita el catálogo sin esos filtros.
6. **Given** una respuesta paginada filtrada, **When** se determina el estado visual, **Then** `totalElements` se usa internamente para distinguir resultados de catálogo vacío o sin coincidencias, pero no se muestra como contador.

---

### User Story 3 - Ordenar el catálogo (Priority: P1)

Como usuario autenticado, quiero elegir un campo y una dirección de orden para revisar los jugadores en el orden que necesito.

**Why this priority**: El orden debe resolverse en el backend para que sea estable entre páginas y no dependa del subconjunto cargado.

**Independent Test**: Se puede elegir cada campo permitido y `asc`/`desc`, comprobar el valor de `sort` enviado y verificar que cualquier cambio vuelve a la primera página.

**Acceptance Scenarios**:

1. **Given** el catálogo abierto, **When** el usuario elige un campo permitido y una dirección, **Then** el frontend envía `sort` con formato `campo,direccion`.
2. **Given** un orden seleccionado, **When** el usuario cambia el campo o la dirección, **Then** se vuelve a `page=0` y se solicita el catálogo con el nuevo orden.
3. **Given** los campos disponibles, **When** se muestran las opciones de orden, **Then** solo aparecen `nombre`, `equipo`, `liga` y `posicion`, con etiquetas legibles en español; `id` no aparece aunque el backend lo acepte.
4. **Given** una respuesta ordenada del backend, **When** se renderiza la lista, **Then** el frontend conserva el orden recibido y no lo reordena localmente.
5. **Given** una pantalla de escritorio, **When** se muestran Equipo, Liga, Posición, Ordenar por, Dirección y Limpiar, **Then** los seis controles permanecen alineados en una sola fila con sus campos alineados por la base.
6. **Given** una pantalla pequeña, **When** se muestra la barra de filtros, **Then** conserva el comportamiento responsive actual y permite que los controles se reacomoden sin desbordamiento.

---

### User Story 4 - Entender los estados del catálogo (Priority: P1)

Como usuario del catálogo, quiero distinguir carga, catálogo vacío, ausencia de coincidencias y error para saber qué ocurre y qué acción puedo tomar.

**Why this priority**: Una respuesta paginada vacía puede significar un catálogo sin jugadores o filtros sin coincidencias, y no debe confundirse con un fallo.

**Independent Test**: Se pueden mockear respuestas de carga, `totalElements=0` sin filtros, `totalElements=0` con filtros y errores HTTP con `mensaje` y `correlationId`.

**Acceptance Scenarios**:

1. **Given** una consulta en curso, **When** aún no llega la respuesta, **Then** se muestra un estado de carga diferenciado y no se presentan datos obsoletos como si fueran actuales.
2. **Given** una respuesta sin filtros con `totalElements=0`, **When** termina la consulta, **Then** se muestra "Catálogo vacío".
3. **Given** una respuesta con filtros activos y `totalElements=0`, **When** termina la consulta, **Then** se muestra "Sin coincidencias" y se conservan visibles los filtros aplicados.
4. **Given** un error del backend con `mensaje` y `correlationId`, **When** termina la consulta, **Then** se muestra el mensaje recibido y el `correlationId`, sin mostrar el token JWT.
5. **Given** un `401`, **When** termina la consulta, **Then** se conserva el comportamiento actual de sesión y protección de ruta, incluida la gestión de acceso denegado de la aplicación.
6. **Given** cualquier respuesta, **When** se renderiza la interfaz, **Then** se mantiene el estilo retro actual, el foco visible y la navegación existente.

## Edge Cases

- La primera consulta usa `page=0` y `size=9` aunque no haya estado previo.
- El frontend usa siempre `size=9`; no ofrece selector ni mantiene tamaño configurable en su estado.
- Cambiar filtro u orden siempre reinicia la página a 0, incluso si la respuesta anterior estaba en una página posterior.
- El valor de `equipo` se sanea quitando espacios externos antes de enviarse; no se dispara una request por cada tecla.
- La lista fija de ligas contiene exactamente `Premier League`, `Bundesliga`, `La Liga`, `Serie A` y `Ligue 1`, y no se genera a partir de `content`.
- Una página con `content=[]` y `totalElements>0` se trata como posible página fuera de rango; la interfaz vuelve a la última página válida conocida o presenta un vacío coherente sin bucle de requests.
- La interfaz muestra la página actual y el total de páginas con numeración humana base 1, pero transforma internamente la página mostrada a la query param `page` base 0.
- Una respuesta con `content=[]` y `totalElements=0` no debe mostrar controles de navegación activos.
- Un error no debe reemplazarse por "Catálogo vacío" ni por "Sin coincidencias".
- El cambio de página conserva filtros y orden actuales, y vuelve a enviar siempre `size=9`.
- El frontend no filtra, ordena ni pagina localmente el array recibido; `content` se muestra tal como lo entrega el backend.
- La búsqueda por nombre existente en la entrega anterior se retira de esta vista porque el backend paginado no acepta `nombre`; no se debe aplicar localmente sobre una sola página.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El catálogo DEBE consultar `GET /api/jugadores` usando el contrato paginado del backend y DEBE enviar una sola página por solicitud.
- **FR-002**: La consulta inicial DEBE usar `page=0` y `size=9`.
- **FR-003**: La interfaz DEBE mostrar controles "Anterior" y "Siguiente", la página actual con numeración base 1 y el total de páginas con numeración base 1; NO DEBE mostrar la cantidad total de jugadores.
- **FR-004**: "Anterior" DEBE estar deshabilitado en la primera página y "Siguiente" DEBE estar deshabilitado en la última página conocida.
- **FR-005**: El frontend DEBE enviar siempre `size=9` y NO DEBE ofrecer un selector de tamaño de página.
- **FR-006**: Al cambiar `page`, filtros u orden, la consulta DEBE construir sus query params con los valores vigentes, `size=9` y sin enviar parámetros vacíos innecesarios.
- **FR-007**: Al cambiar cualquier filtro u orden, el estado de página DEBE volver a 0 antes de consultar; la acción "Limpiar" NO DEBE modificar el tamaño fijo.
- **FR-008**: El filtro de liga DEBE usar una lista fija con exactamente las cinco ligas del dominio: `Premier League`, `Bundesliga`, `La Liga`, `Serie A` y `Ligue 1`.
- **FR-009**: Los filtros de liga, equipo y posición DEBEN enviarse como `liga`, `equipo` y `posicion`; no DEBEN aplicarse como filtrado posterior en el cliente.
- **FR-010**: El filtro de equipo DEBE ser texto libre y DEBE aplicar debounce o confirmación para no generar una solicitud por cada tecla.
- **FR-011**: La interfaz DEBE ofrecer orden por `nombre`, `equipo`, `liga` y `posicion`, con dirección `asc` o `desc`, enviando un único parámetro `sort=campo,direccion`; NO DEBE mostrar `id` como opción.
- **FR-012**: El catálogo DEBE conservar filtros y orden al navegar entre páginas, y enviar siempre `size=9`.
- **FR-013**: El catálogo DEBE representar por separado los estados de carga, catálogo vacío, sin coincidencias y error.
- **FR-014**: Catálogo vacío DEBE corresponder a una respuesta sin filtros con `totalElements=0`; sin coincidencias DEBE corresponder a una respuesta filtrada con `totalElements=0`.
- **FR-015**: Ante un error del backend, la interfaz DEBE mostrar `mensaje` y `correlationId` recibidos en el `ErrorResponse` estándar.
- **FR-016**: Ante `content` vacío en una página fuera de rango con `totalElements>0`, la interfaz DEBE solicitar una sola vez la página `totalPages - 1` y mostrarla, sin reintentos infinitos.
- **FR-017**: El frontend DEBE mantener la sesión, la protección de la ruta y el tratamiento actual de `401`.
- **FR-018**: El JWT DEBE permanecer oculto en pantalla, mensajes, controles y estados del catálogo.
- **FR-019**: El cambio DEBE preservar la estética retro actual y la accesibilidad existente: etiquetas asociadas, foco visible y controles operables por teclado.
- **FR-020**: Los tests con Vitest y MSW DEBEN cubrir hook y componentes para paginación, filtros, orden, tamaño fijo 9, carga, vacío, sin coincidencias y error, usando respuestas con `content`, `page`, `size`, `totalElements` y `totalPages`.
- **FR-021**: La implementación NO DEBE modificar el backend, la ficha/detalle del jugador, el mercado de tokens ni los gráficos.
- **FR-022**: En escritorio, la barra de filtros DEBE usar seis columnas para mantener en una sola fila los cinco campos y el botón "Limpiar", con alineación inferior; en pantallas pequeñas DEBE conservar el reacomodo responsive existente.

### Key Entities

- **Página de catálogo**: Respuesta del backend con `content`, `page`, `size`, `totalElements` y `totalPages`.
- **Consulta de catálogo**: Estado del frontend compuesto por página, tamaño fijo 9, orden, dirección y filtros `liga`, `equipo` y `posicion`.
- **Jugador**: Elemento recibido en `content` con `id`, `nombre`, `equipo`, `liga` y `posicion`.
- **Estado del catálogo**: Estado visual diferenciado de carga, datos, catálogo vacío, sin coincidencias o error.
- **ErrorResponse**: Error del backend con `mensaje`, `timestamp` y `correlationId`.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las consultas iniciales y de navegación envía exactamente una página con `page` válido y `size=9`, sin descargar la lista completa.
- **SC-002**: El 100% de los cambios de filtro u orden envía `page=0` y `size=9` en la siguiente consulta.
- **SC-003**: El 100% de las solicitudes con filtros usa los nombres y valores del contrato backend, y ninguna respuesta se vuelve a filtrar u ordenar localmente.
- **SC-004**: El 100% de las pruebas de equipo con varias teclas evita solicitudes intermedias por cada tecla y produce como máximo la consulta correspondiente al valor estabilizado.
- **SC-005**: El 100% de las respuestas paginadas usa `totalPages` para habilitar o deshabilitar navegación y no muestra `totalElements` como contador.
- **SC-006**: El 100% de las respuestas vacías distingue catálogo vacío de sin coincidencias según existan filtros activos.
- **SC-007**: El 100% de los errores mockeados muestra el `mensaje` y `correlationId` del backend sin exponer el JWT.
- **SC-008**: El 100% de los tests de hook y componentes usa el formato paginado del backend y cubre los estados y transiciones definidos.
- **SC-009**: Ninguna modificación de esta feature altera backend, autenticación, detalle del jugador, mercado de tokens o gráficos.

## Assumptions

- El endpoint backend exige la misma autenticación que el catálogo actual y el cliente API ya adjunta el JWT de la sesión sin exponerlo en la UI.
- El contrato paginado del backend es la única fuente de verdad para nombres de parámetros, ligas, campos de orden y forma de `ErrorResponse`.
- Se usará un debounce de aproximadamente 300 ms para `equipo`; el tiempo exacto puede ajustarse durante el plan si las pruebas del repositorio establecen otra convención.
- El estado de página válida conocida se conserva en el hook; si una respuesta fuera de rango no permite recuperar una página válida, se muestra un estado vacío sin bucle.
- La ficha del jugador continúa recibiendo el jugador seleccionado desde la página actual; no se agrega una consulta de detalle.
- La búsqueda por nombre de la entrega anterior se retira de la consulta para evitar filtrado incompleto del lado cliente, ya que `nombre` no es un filtro aceptado por el backend.
- Aunque el backend acepta ordenar por `id`, la interfaz no lo ofrece; el orden predeterminado continúa siendo `nombre,asc`.

## Out of Scope

- Cambios en el backend o en el contrato OpenAPI backend.
- Detalle remoto de un jugador o nuevas rutas de jugadores.
- Mercado de tokens, cotizaciones, transacciones y gráficos.
- Nuevas ligas o derivación dinámica de opciones desde los resultados.
- Filtrado, ordenamiento o paginación local sobre una colección completa.
