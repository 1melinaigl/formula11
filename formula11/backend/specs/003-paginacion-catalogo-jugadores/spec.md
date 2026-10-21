# Feature Specification: Paginación del catálogo de jugadores

**Feature Branch**: `003-paginacion-catalogo-jugadores`

**Created**: 2026-10-07

**Status**: Draft

**Input**: User description: "Paginación del catálogo de jugadores. GET /api/jugadores debe aceptar paginación, orden y filtros por liga, equipo y posición, y devolver metadatos de la página sin exponer directamente el tipo Page de Spring."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar una página del catálogo (Priority: P1)

Como usuario del catálogo, quiero consultar los jugadores por páginas para cargar solo una cantidad acotada de resultados y conocer el total disponible.

**Why this priority**: Evita devolver el catálogo completo en cada solicitud y proporciona al frontend la información necesaria para construir una navegación por páginas.

**Independent Test**: Se puede cargar un conjunto conocido de jugadores y consultar varias páginas con distintos tamaños, verificando el contenido y los metadatos sin depender del frontend.

**Acceptance Scenarios**:

1. **Given** un usuario con la misma credencial requerida actualmente y un catálogo con más de 20 jugadores, **When** consulta `GET /api/jugadores` sin parámetros, **Then** recibe `200` con la primera página, tamaño 20 y los campos `content`, `page`, `size`, `totalElements` y `totalPages`.
2. **Given** un catálogo paginado, **When** consulta una página válida con `page` base 0 y un `size` entre 1 y 100, **Then** recibe como máximo `size` jugadores y el número de página y tamaño solicitados en la respuesta.
3. **Given** una página cuyo índice es mayor o igual que el total de páginas, **When** consulta el catálogo, **Then** recibe `200` con `content` vacío y metadatos coherentes con la consulta.
4. **Given** un catálogo vacío, **When** consulta cualquier página válida, **Then** recibe `200` con `content` vacío, `totalElements` igual a 0 y `totalPages` igual a 0.

---

### User Story 2 - Ordenar y filtrar el catálogo (Priority: P1)

Como usuario del catálogo, quiero ordenar y filtrar los jugadores por sus atributos para encontrar resultados relevantes dentro de las páginas.

**Why this priority**: La paginación solo es útil para el catálogo si el usuario puede obtener resultados estables y acotar la búsqueda.

**Independent Test**: Se puede cargar un conjunto de jugadores con distintas ligas, equipos, posiciones y nombres, y comprobar cada filtro, orden y combinación con paginación.

**Acceptance Scenarios**:

1. **Given** jugadores con nombres y demás atributos distintos, **When** consulta con `sort=nombre,asc` o `sort=nombre,desc`, **Then** los resultados aparecen ordenados según el campo y dirección solicitados.
2. **Given** jugadores de varias ligas, **When** consulta con `liga`, **Then** solo recibe jugadores de esa liga.
3. **Given** jugadores de varios equipos o posiciones, **When** combina `equipo` y `posicion`, **Then** solo recibe jugadores que cumplen todos los filtros.
4. **Given** filtros, orden y paginación en la misma solicitud, **When** los parámetros son válidos, **Then** el filtrado y orden se aplican antes de calcular la página y los metadatos reflejan el conjunto filtrado.
5. **Given** un filtro válido que no coincide con ningún jugador, **When** consulta el catálogo, **Then** recibe `200` con `content` vacío y totales igual a 0.

---

### User Story 3 - Recibir errores de parámetros claros (Priority: P1)

Como consumidor de la API, quiero que los parámetros inválidos se rechacen usando el formato estándar de errores para poder corregir la solicitud.

**Why this priority**: Impide consultas ambiguas o potencialmente costosas y mantiene un contrato de errores consistente con el resto del backend.

**Independent Test**: Se pueden enviar solicitudes con cada clase de parámetro inválido y comprobar el estado `400` y la estructura `ErrorResponse`.

**Acceptance Scenarios**:

1. **Given** `page` negativo, no numérico, `size` menor que 1 o mayor que 100, **When** consulta el catálogo, **Then** recibe `400` con `mensaje`, `timestamp` y `correlationId`.
2. **Given** un campo de orden inexistente o una dirección distinta de `asc`/`desc`, **When** consulta el catálogo, **Then** recibe `400` con el `ErrorResponse` estándar.
3. **Given** un nombre de filtro inexistente o un valor de `liga` que no pertenece a las ligas permitidas, **When** consulta el catálogo, **Then** recibe `400` con el `ErrorResponse` estándar.
4. **Given** una solicitud válida, **When** se procesa, **Then** el endpoint conserva la protección de autenticación que tiene actualmente y no se vuelve público por incorporar paginación.

---

### Edge Cases

- La ausencia de `page` equivale a `page=0` y la ausencia de `size` equivale a `size=20`.
- `size=1` y `size=100` son válidos; `size=0`, valores negativos y valores mayores que 100 son inválidos.
- La numeración de páginas comienza en 0.
- `page` fuera de rango no es un error: devuelve una lista vacía con `200`.
- Un valor de filtro de `equipo` o `posicion` sin coincidencias produce una respuesta vacía, no un `400`.
- Los filtros de texto se comparan parcialmente después de quitar espacios externos y sin distinguir mayúsculas/minúsculas; no se aceptan valores vacíos o compuestos solo por espacios.
- `liga` acepta únicamente la representación pública de las cinco ligas permitidas: `Premier League`, `Bundesliga`, `La Liga`, `Serie A` y `Ligue 1`.
- `sort` acepta el formato `campo,direccion`; los campos válidos son `id`, `nombre`, `equipo`, `liga` y `posicion`, y la dirección válida es `asc` o `desc`.
- Si no se proporciona `sort`, se conserva el orden predeterminado estable por `nombre` ascendente y, ante empate, por `id` ascendente.
- La respuesta no debe exponer el tipo `Page` de Spring ni campos internos adicionales.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE mantener `GET /api/jugadores` en la misma ruta y con la misma protección de autenticación que tiene actualmente.
- **FR-002**: El endpoint DEBE aceptar `page` como entero base 0, con valor predeterminado `0`.
- **FR-003**: El endpoint DEBE aceptar `size` como entero positivo, con valor predeterminado `20` y máximo `100`.
- **FR-004**: El sistema DEBE aplicar la paginación después de aplicar los filtros y el orden solicitados.
- **FR-005**: La respuesta exitosa DEBE ser un objeto propio del contrato con exactamente estos datos funcionales: `content`, `page`, `size`, `totalElements` y `totalPages`.
- **FR-006**: `content` DEBE contener jugadores con los campos públicos actuales `id`, `nombre`, `equipo`, `liga` y `posicion`.
- **FR-007**: `page` DEBE indicar el índice base 0 solicitado, `size` el tamaño solicitado, `totalElements` el total de resultados después de filtrar y `totalPages` el número de páginas calculado para ese conjunto.
- **FR-008**: Una página fuera de rango DEBE responder `200` con `content` vacío y sin alterar `totalElements` ni `totalPages`.
- **FR-009**: El sistema DEBE aceptar el parámetro opcional `sort` con formato `campo,direccion`, permitiendo los campos `id`, `nombre`, `equipo`, `liga` y `posicion`, y las direcciones `asc` y `desc`.
- **FR-010**: Si no se envía `sort`, el sistema DEBE ordenar por `nombre` ascendente y usar `id` ascendente como desempate estable.
- **FR-011**: El sistema DEBE aceptar los filtros opcionales `liga`, `equipo` y `posicion`, y combinarlos entre sí y con `page`, `size` y `sort`.
- **FR-012**: Los filtros de texto DEBEN buscar coincidencias parciales ignorando espacios externos y diferencias de mayúsculas/minúsculas, mientras que un filtro sin coincidencias DEBE producir una respuesta vacía válida.
- **FR-013**: Un valor de `liga` que no corresponda a una liga permitida DEBE considerarse inválido.
- **FR-014**: Un parámetro de paginación inválido, un campo o dirección de orden inválidos, un filtro inexistente, un filtro vacío o un valor de liga inválido DEBE responder `400` usando el `ErrorResponse` estándar del proyecto, con `mensaje`, `timestamp` y `correlationId`.
- **FR-015**: El sistema NO DEBE devolver directamente una instancia ni la representación serializada del tipo `Page` de Spring.
- **FR-016**: La documentación Swagger/OpenAPI DEBE describir `page`, `size`, `sort`, `liga`, `equipo`, `posicion`, sus valores predeterminados y límites, la respuesta paginada y los errores `400` y de autenticación aplicables.
- **FR-017**: La implementación DEBE incluir tests unitarios del service, tests de integración del repository y tests E2E del endpoint, cubriendo paginación, orden, filtros, combinaciones, página fuera de rango y parámetros inválidos.
- **FR-018**: Los cambios de esta feature NO DEBEN modificar el frontend.

### Key Entities

- **Jugador**: Registro del catálogo con `id`, `nombre`, `equipo`, `liga` y `posicion`; sus cinco atributos públicos pueden participar en el orden, pero solo `liga`, `equipo` y `posicion` se exponen como filtros.
- **Página de jugadores**: Contrato de respuesta que contiene el subconjunto `content` y los metadatos `page`, `size`, `totalElements` y `totalPages`.
- **ErrorResponse**: Contrato estándar de error del backend con `mensaje`, `timestamp` y `correlationId`.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las consultas sin parámetros devuelve como máximo 20 jugadores y los cinco campos de metadatos definidos.
- **SC-002**: El 100% de las consultas con `size` válido entre 1 y 100 respeta el tamaño solicitado, salvo que queden menos resultados.
- **SC-003**: El 100% de las consultas con filtros válidos devuelve únicamente jugadores que cumplen todos los filtros, y sus totales corresponden al conjunto filtrado.
- **SC-004**: El 100% de las consultas con orden válido respeta el campo y dirección solicitados, manteniendo un desempate estable.
- **SC-005**: El 100% de las páginas fuera de rango responde `200` con `content` vacío y sin error.
- **SC-006**: El 100% de los parámetros inválidos definidos por el contrato responde `400` con los tres campos del `ErrorResponse` estándar.
- **SC-007**: La documentación OpenAPI refleja el contrato paginado y los tests unitarios, de integración y E2E cubren los escenarios críticos antes de considerar terminada la feature.
- **SC-008**: Ningún cambio de esta feature modifica archivos o comportamiento del frontend.

## Assumptions

- El orden se expresa con la convención `sort=campo,direccion`; no se requiere soportar múltiples parámetros `sort` en esta entrega.
- La comparación de `liga` usa su representación pública legible, no el nombre interno del enum persistido.
- Los filtros de `equipo` y `posicion` son búsquedas parciales normalizadas, no coincidencias exactas obligatorias.
- `totalPages` se calcula como 0 cuando `totalElements` es 0; en los demás casos equivale al techo de `totalElements / size`.
- El `ErrorResponse` estándar conserva exactamente los nombres y campos ya definidos por el backend.
- La documentación contractual versionada de esta feature vivirá bajo `specs/003-paginacion-catalogo-jugadores/contracts/openapi.yaml` cuando se ejecute la fase de plan/contrato; esta solicitud crea únicamente la especificación funcional.

## Out of Scope

- Cambios en el frontend o en sus componentes de navegación.
- Nuevos atributos, altas, bajas o edición de jugadores.
- Búsqueda por relevancia y filtros por campos distintos de `liga`, `equipo` y `posicion`.
- Múltiples criterios `sort` en una misma solicitud.
- Exportación completa del catálogo, caché, cambios de autenticación o modificación del modelo de seguridad.
