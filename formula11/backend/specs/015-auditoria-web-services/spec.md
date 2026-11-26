# Feature Specification: Auditoría de web services

**Feature Branch**: `entrega3-observabilidad`

**Created**: 2026-11-26

**Status**: Draft

**Input**: Entrega 3, integrante B (Observabilidad): "Auditoría de web services con logback. Loguear timestamp, usuario, operación/método, parámetros y tiempo de ejecución en ms de cada llamada a la API. El usuario sale de la sesión; si no hay sesión, se pone 'anónimo'. No loguear contraseñas ni tokens. Archivo aparte con rotación diaria, sin perder el correlationId."

## Scope

Cada llamada a un endpoint propio de la API deja una línea en un registro de auditoría separado del log general. El registro sirve para saber quién llamó a qué, con qué datos y cuánto tardó, y se cruza con el log general por el correlation ID que ya existe. La feature no cambia ningún endpoint ni su respuesta: solo agrega el registro.

Quedan fuera: el endpoint de métricas, Actuator y Prometheus (otra feature de B), y la auditoría de operaciones de mercado que ya se guarda en la base (`AuditoriaMercado`), que no se toca.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Registrar cada llamada a la API (Priority: P1)

Como integrante del equipo que opera el sistema, quiero que cada llamada a la API quede registrada con fecha y hora, usuario, operación, parámetros y duración, para poder reconstruir qué pasó ante un error o un reclamo.

**Why this priority**: Es el núcleo del ítem de la consigna. Sin esto no hay auditoría.

**Independent Test**: Llamar a un endpoint autenticado (por ejemplo, el portfolio) y verificar que en el registro de auditoría aparece una línea con el email del usuario, la operación invocada, sus parámetros y el tiempo en ms.

**Acceptance Scenarios**:

1. **Given** un usuario autenticado, **When** consulta un endpoint de la API, **Then** el registro de auditoría tiene una línea con fecha y hora, el email del usuario, la operación (clase y método), los parámetros recibidos y la duración en milisegundos.
2. **Given** una llamada sin sesión a un endpoint público (registro o login), **When** se procesa, **Then** la línea de auditoría muestra el usuario como `anónimo`.
3. **Given** una llamada que termina en error (por ejemplo, un 404 o un 409 del dominio), **When** se procesa, **Then** igual queda la línea de auditoría, con la duración y la indicación de que terminó en error y el tipo de error.
4. **Given** una llamada con correlation ID, **When** se audita, **Then** la línea de auditoría lleva el mismo correlation ID que las líneas del log general de esa llamada.

---

### User Story 2 - No exponer datos sensibles (Priority: P1)

Como responsable de seguridad, quiero que el registro de auditoría nunca contenga contraseñas ni tokens, para que el archivo de auditoría no se convierta en una fuente de filtraciones.

**Why this priority**: Un registro que guarda contraseñas es peor que no tener registro. Va junto con la historia 1.

**Independent Test**: Hacer un login y un registro con una contraseña conocida y verificar que esa contraseña no aparece en el registro, pero el resto de los datos (por ejemplo, el email) sí.

**Acceptance Scenarios**:

1. **Given** un pedido de login, **When** se audita, **Then** la línea muestra el email y la contraseña aparece enmascarada (`****`).
2. **Given** un pedido de registro de usuario, **When** se audita, **Then** la línea muestra nombre y email, y la contraseña aparece enmascarada.
3. **Given** cualquier llamada autenticada, **When** se audita, **Then** el token de sesión no aparece en la línea.

---

### User Story 3 - Registro aparte y con rotación (Priority: P2)

Como integrante del equipo, quiero que la auditoría quede en un archivo propio que se rota por día, para revisarla sin mezclarla con el log general y sin que crezca sin límite.

**Why this priority**: Hace que el registro sea práctico de consultar y mantener, pero la historia 1 ya aporta valor sin esto.

**Independent Test**: Levantar la aplicación, hacer una llamada y verificar que la línea aparece en el archivo de auditoría y no en el log general; revisar que la configuración arma un archivo nuevo por día.

**Acceptance Scenarios**:

1. **Given** la aplicación levantada, **When** se llama a un endpoint, **Then** la línea de auditoría se escribe en el archivo `logs/auditoria.log` y no se duplica en el log general.
2. **Given** que cambia el día, **When** llega una nueva llamada, **Then** el archivo del día anterior queda guardado con su fecha en el nombre y se empieza uno nuevo.

---

### Edge Cases

- Parámetros que no son datos del pedido (por ejemplo, la sesión o el usuario autenticado inyectado): no se muestran como parámetros; el usuario ya tiene su campo.
- Parámetros nulos o ausentes (filtros opcionales sin valor): se muestran como `null`, sin romper la línea.
- Parámetros muy largos (por ejemplo, una lista o un cuerpo grande): se recortan a un largo máximo para que una línea no ocupe megas.
- Si falla la escritura del registro de auditoría, la llamada a la API igual responde normalmente: la auditoría nunca rompe un endpoint.
- Endpoints que no son de la API propia (Actuator, documentación OpenAPI, Swagger): no se auditan.
- Las tareas programadas (recálculo, carga de métricas) no son llamadas a la API y no se auditan.
- Los pedidos que se rechazan antes de llegar al endpoint (sin sesión o sin permiso, y cuerpos que no pasan la validación de formato) no generan línea de auditoría: no llegan a ejecutar la operación. Los errores que ocurren dentro de la operación sí se auditan.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST registrar una línea de auditoría por cada llamada a un endpoint de la API propia, termine bien o con error.
- **FR-002**: Cada línea MUST incluir: fecha y hora, usuario, operación (clase y método invocados), parámetros, duración en milisegundos, resultado (`OK` o `ERROR` con el tipo de error) y correlation ID.
- **FR-003**: El usuario MUST ser el email del usuario autenticado; si la llamada no tiene sesión, MUST registrarse `anónimo`.
- **FR-004**: El sistema MUST enmascarar las contraseñas de los pedidos de login y de registro, y nunca MUST registrar tokens de sesión.
- **FR-005**: Las líneas de auditoría MUST escribirse en un archivo propio (`logs/auditoria.log`), separado del log general, con rotación diaria.
- **FR-006**: Las líneas de auditoría MUST llevar el mismo correlation ID que el resto del log de esa llamada.
- **FR-007**: Un error al auditar MUST NOT cambiar la respuesta ni el código HTTP del endpoint.
- **FR-008**: Los endpoints nuevos que se agreguen después (por ejemplo, los del mercado P2P) MUST quedar auditados sin código extra en cada uno.

### Key Entities

- **Línea de auditoría**: un registro por llamada. Tiene fecha y hora, usuario, operación, parámetros (ya enmascarados), duración en ms, resultado y correlation ID. No se guarda en la base: vive en el archivo de auditoría.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100 % de las llamadas a la API propia, exitosas o con error, generan exactamente una línea de auditoría.
- **SC-002**: Ninguna contraseña ni token aparece en el archivo de auditoría, verificado con un test que busca una contraseña conocida después de un login y un registro.
- **SC-003**: A partir de un correlation ID se encuentran juntas la línea de auditoría y las del log general de esa misma llamada.
- **SC-004**: Ningún test existente cambia su resultado: las respuestas y los códigos HTTP de la API siguen iguales.

## Assumptions

- Se audita la entrada a los endpoints propios (los controllers de la API). No se registran los cuerpos de las respuestas: el resultado se resume como `OK` o `ERROR`.
- El "usuario" es el email, porque es lo que guarda hoy la sesión autenticada.
- Los archivos rotados se conservan 30 días; es un valor razonable para una demo y se puede cambiar por configuración.
- El largo máximo de los parámetros en una línea es de 500 caracteres; lo que sobra se recorta con una marca.
- Solo `LoginRequest` y `RegistroUsuarioRequest` tienen contraseñas hoy. Si una feature futura agrega otro dato sensible, lo suma al enmascarado.
- En el perfil de tests el archivo se puede redirigir o desactivar para no ensuciar el repo; el test de integración verifica la línea igual.
- `application.yml` y la configuración de logging solo los cambia B (acuerdo del grupo para la entrega 3).
