# Feature Specification: Registro de usuarios y catálogo de jugadores

**Feature Branch**: `001-registro-catalogo-jugadores`

**Created**: 2026-09-08

**Status**: Draft

**Input**: User description: "Permitir el registro de usuarios y la consulta autenticada del catálogo de jugadores de fútbol."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Registrar usuario y obtener credencial (Priority: P1)

Como visitante, quiero registrarme con mi nombre, email y contraseña para obtener una credencial de sesión y acceder al sistema.

**Why this priority**: El registro crea la identidad necesaria para todas las operaciones protegidas del sistema.

**Independent Test**: Se puede probar enviando datos válidos de registro y verificando la creación del usuario y la entrega de una credencial utilizable.

**Acceptance Scenarios**:

1. **Given** que el email no está registrado, **When** la persona envía su nombre, email y contraseña válidos, **Then** el sistema crea el usuario y devuelve una credencial de sesión.
2. **Given** que ya existe un usuario con ese email, **When** la persona intenta registrarse nuevamente, **Then** el sistema rechaza el registro y no crea otro usuario.
3. **Given** que falta un dato requerido o tiene un formato inválido, **When** la persona envía el registro, **Then** el sistema rechaza la solicitud indicando el error de validación.

---

### User Story 2 - Proteger el acceso a los endpoints (Priority: P1)

Como usuario registrado, quiero que los endpoints protegidos solo acepten mi credencial válida para que el acceso al sistema esté controlado.

**Why this priority**: La credencial es el único requisito de acceso posterior al registro y protege los datos del mercado.

**Independent Test**: Se puede probar el mismo endpoint protegido con una credencial válida, sin credencial, con una credencial inválida y con una credencial vencida.

**Acceptance Scenarios**:

1. **Given** un usuario registrado con una credencial vigente, **When** presenta la credencial ante un endpoint protegido, **Then** el sistema permite continuar con la acción autorizada.
2. **Given** una solicitud a un endpoint protegido sin credencial, **When** el usuario intenta acceder, **Then** el sistema rechaza la solicitud.
3. **Given** una credencial inválida o vencida, **When** el usuario intenta acceder a un endpoint protegido, **Then** el sistema rechaza la solicitud y no ejecuta la acción.

---

### User Story 3 - Consultar el catálogo de jugadores (Priority: P2)

Como usuario autenticado, quiero consultar el catálogo completo de jugadores para conocer los jugadores disponibles en el sistema.

**Why this priority**: El catálogo es la base informativa para las etapas posteriores del mercado de inversión.

**Independent Test**: Se puede probar cargando jugadores de las cinco ligas y consultando el catálogo con una credencial válida, sin depender de cotizaciones ni transacciones.

**Acceptance Scenarios**:

1. **Given** un usuario autenticado y un catálogo con jugadores, **When** consulta el catálogo, **Then** recibe todos los jugadores disponibles con nombre, equipo, posición y liga.
2. **Given** un jugador perteneciente a una liga soportada, **When** se consulta el catálogo, **Then** su liga es una de: Premier League, Bundesliga, La Liga, Serie A o Ligue 1.
3. **Given** un usuario sin credencial válida, **When** intenta consultar el catálogo, **Then** el sistema rechaza la solicitud por tratarse de un endpoint protegido.
4. **Given** un jugador del catálogo, **When** se consulta su información, **Then** no se exige ni se devuelve una cotización o valor monetario en esta entrega.

---

### Edge Cases

- Un email debe considerarse repetido aunque cambie únicamente su capitalización o tenga espacios externos que deban sanearse.
- Los campos de registro vacíos, con espacios únicamente o con tipos incorrectos deben rechazarse sin crear usuarios parciales.
- Una contraseña ausente o inválida no debe producir una credencial.
- El catálogo vacío debe devolver una respuesta válida sin inventar jugadores.
- Un jugador con una liga distinta de las cinco permitidas no debe incorporarse al catálogo.
- La ausencia, invalidez o vencimiento de la credencial debe bloquear cualquier endpoint protegido.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE permitir registrar usuarios con nombre, email y contraseña.
- **FR-002**: El sistema DEBE impedir más de un usuario con el mismo email, ignorando diferencias de mayúsculas y espacios externos.
- **FR-003**: El sistema DEBE validar la presencia, tipo, formato y saneamiento de los datos de registro antes de crear el usuario.
- **FR-004**: El sistema DEBE entregar una credencial de sesión al completar correctamente el registro.
- **FR-005**: Todos los endpoints, excepto el registro de usuario, DEBEN requerir una credencial de sesión válida y vigente.
- **FR-006**: El sistema DEBE rechazar solicitudes protegidas sin credencial, con credencial inválida o con credencial vencida.
- **FR-007**: El sistema DEBE mantener un catálogo de jugadores con nombre, equipo, posición y liga.
- **FR-008**: La liga de cada jugador DEBE pertenecer a exactamente una de estas opciones: Premier League, Bundesliga, La Liga, Serie A o Ligue 1.
- **FR-009**: El sistema DEBE permitir a cualquier usuario autenticado consultar el catálogo completo de jugadores.
- **FR-010**: En esta entrega, el sistema NO DEBE requerir ni calcular cotizaciones, precios monetarios o valores de los jugadores.
- **FR-011**: Las respuestas de error y los mensajes funcionales DEBEN estar en español.

### Key Entities *(include if feature involves data)*

- **Usuario**: Persona registrada en el sistema; contiene nombre, email y contraseña protegida, y se relaciona con su credencial de sesión.
- **Credencial de sesión**: Identificador temporal que permite autenticar al usuario frente a los endpoints protegidos.
- **Jugador**: Integrante del catálogo; contiene nombre, equipo, posición y una liga permitida. No tiene cotización ni valor en esta entrega.
- **Liga**: Clasificación del jugador, limitada a Premier League, Bundesliga, La Liga, Serie A o Ligue 1.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de los registros con datos válidos crea un único usuario y devuelve una credencial.
- **SC-002**: El 100% de los intentos de registro con un email ya existente es rechazado sin duplicar usuarios.
- **SC-003**: El 100% de las solicitudes protegidas sin credencial válida es rechazado antes de ejecutar la acción.
- **SC-004**: El 100% de los jugadores devueltos por el catálogo contiene nombre, equipo, posición y una de las cinco ligas permitidas.
- **SC-005**: Ningún jugador del catálogo requiere una cotización o valor monetario para ser consultado en esta entrega.

## Assumptions

- La credencial será el token JWT indicado por la constitución del proyecto; no se incorpora un mecanismo M2M separado.
- El registro es el único endpoint público de esta feature; no se agrega login independiente porque no fue solicitado.
- La contraseña se almacenará protegida y nunca se devolverá en respuestas.
- El catálogo puede contar con datos iniciales administrados por el sistema; la carga y administración de jugadores no forman parte de esta feature.
- La cotización, el precio, los tokens, las compras y las ventas quedan fuera de alcance y se resolverán en entregas posteriores.
