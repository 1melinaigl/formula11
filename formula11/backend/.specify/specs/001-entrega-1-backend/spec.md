# Feature Specification: Entrega 1 - Backend del mercado de jugadores

**Feature Branch**: `001-entrega-1-backend`

**Created**: 2026-08-31

**Status**: Draft

**Input**: User description: "Crear la especificación de requerimientos para la Entrega 1 del backend. Modelo de datos mínimo a implementar en PostgreSQL: User (id, username, email, password, apiKey), Player (id, name, league, team, position, baseValue). Funcionalidades a especificar (Endpoints): creación de usuario y generación de ApiKEY; GET /players para devolver el catálogo de jugadores. Fuera de alcance: no especificar el mercado de compra/venta de tokens ni los cálculos de cotización en esta iteración."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Registro de usuario y generación de API key (Priority: P1)

Un nuevo usuario debe poder crear una cuenta con sus datos básicos y recibir automáticamente una ApiKey válida para utilizar el resto de los endpoints protegidos del backend.

**Why this priority**: Este flujo es el punto de entrada del sistema y habilita el acceso seguro al catálogo de jugadores y al resto de funcionalidades futuras.

**Independent Test**: Se puede probar creando un usuario con username, email y password válidos y verificando que el sistema devuelve la ApiKey generada y persiste la cuenta correctamente.

**Acceptance Scenarios**:

1. **Given** un cliente sin usuario registrado, **When** envía una petición POST para crear un usuario con username, email y password, **Then** el sistema crea la cuenta, genera una ApiKey única y responde con la información del usuario y la ApiKey.
2. **Given** un email o username ya existente, **When** se intenta registrar un usuario duplicado, **Then** el sistema rechaza la operación y devuelve un error de validación o conflicto.
3. **Given** un usuario recién creado, **When** se consulta su contraseña almacenada, **Then** la contraseña no se guarda en texto plano y el sistema protege el dato sensible.

---

### User Story 2 - Consulta del catálogo de jugadores (Priority: P1)

Un usuario autenticado con ApiKey válida debe poder consultar el catálogo completo de jugadores con sus datos básicos.

**Why this priority**: El catálogo de jugadores es la funcionalidad principal de esta entrega y permite validar que el backend expone un conjunto de datos útil para la aplicación.

**Independent Test**: Se puede probar con una ApiKey válida y verificar que el endpoint devuelve una lista de jugadores con los campos esperados.

**Acceptance Scenarios**:

1. **Given** un usuario autenticado y con ApiKey válida, **When** solicita GET /players, **Then** el sistema responde con el listado de jugadores disponibles.
2. **Given** una ApiKey inválida o ausente, **When** se ejecuta GET /players, **Then** el sistema rechaza la petición con error de autenticación.
3. **Given** la base de datos no tiene jugadores cargados, **When** se consulta el catálogo, **Then** el sistema responde con una lista vacía y un estado correcto o con el mensaje correspondiente según la decisión de diseño.

---

### User Story 3 - Delimitación del alcance de la entrega (Priority: P2)

El equipo debe mantener esta iteración centrada en registro, autenticación por ApiKey/JWT y catálogo de jugadores, sin incorporar operaciones de mercado ni cotización.

**Why this priority**: Esto evita la expansión de alcance y asegura que la Entrega 1 se entregue con un objetivo claro y verificable.

**Independent Test**: Se valida revisando la especificación y la implementación para confirmar que no existen endpoints ni reglas de negocio asociadas a compra/venta ni cálculo de cotización.

**Acceptance Scenarios**:

1. **Given** la Entrega 1 especificada, **When** se revisa el alcance, **Then** la funcionalidad de mercado de compra/venta y cotización queda fuera del alcance.
2. **Given** un desarrollador quiere agregar lógica de cotización, **When** intenta incorporarla en esta iteración, **Then** debe rechazarse o postergarse para una entrega posterior.

---

### Edge Cases

- What happens when a user attempts to register with an email that already exists?
- How does the system handle a request with no ApiKey or with a malformed ApiKey?
- What happens when a player list is empty?
- How does the system handle invalid or missing required fields during user creation?
- What happens when the username or email exceeds the expected storage limits?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST allow the creation of a new user with username, email, and password.
- **FR-002**: The system MUST validate that username and email are provided and meet the minimum required rules for this iteration.
- **FR-003**: The system MUST reject duplicate usernames or duplicate emails during registration.
- **FR-004**: The system MUST generate a unique ApiKey automatically when a user is created.
- **FR-005**: The system MUST persist the user information together with the generated ApiKey in PostgreSQL.
- **FR-006**: The system MUST store the password securely using a hashed representation and MUST NOT persist it in plain text.
- **FR-007**: The system MUST expose an endpoint to retrieve the player catalog via GET /players.
- **FR-008**: The GET /players endpoint MUST return the player collection with the fields defined for the Player entity: id, name, league, team, position, and baseValue.
- **FR-009**: The system MUST require valid authentication for protected endpoints, based on JWT and/or ApiKey enforcement as defined by the backend security model.
- **FR-010**: The system MUST support the ApiKey generated at user creation as the access credential for the endpoints of this iteration.
- **FR-011**: The system MUST define the minimal data model required for this release in PostgreSQL: User and Player.
- **FR-012**: The system MUST exclude from this iteration any feature related to token trading, purchase/sale operations, and quota/cotization calculations.
- **FR-013**: The system MUST keep the data model and endpoints scoped to the first delivery, without introducing marketplace logic beyond registration and catalog retrieval.

### Key Entities *(include if feature involves data)*

- **User**: Represents a registered application user. It includes the identifier, username, email, password, and ApiKey. The password is stored in a protected form and the ApiKey is generated for access control.
- **Player**: Represents a football player in the catalog. It includes the identifier, name, league, team, position, and base value used for representation in this first iteration.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can successfully register and receive a unique ApiKey in a single request flow.
- **SC-002**: The system rejects registration attempts with duplicate email or username values.
- **SC-003**: A valid ApiKey allows access to GET /players and returns the complete player catalog according to the data model.
- **SC-004**: A request without a valid ApiKey is rejected through the authentication layer.
- **SC-005**: The first iteration includes only user registration and player listing, with no trading or quote calculation behavior.

## Assumptions

- Users interact with the backend through HTTP API requests and are not expected to use a graphical interface in this first delivery.
- The PostgreSQL schema will include the minimal entities required for this iteration: User and Player.
- The system will use JWT and ApiKey-based protection for endpoints as part of the required security architecture.
- The product backlog for later iterations will cover market operations and cotization logic, which are explicitly excluded from this specification.
- Initial player data can be loaded via seed scripts, SQL inserts, or repository initialization for the first working version.
