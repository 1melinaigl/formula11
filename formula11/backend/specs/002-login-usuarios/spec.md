# Feature Specification: Login de usuarios

**Feature Branch**: `002-login-usuarios`

**Created**: 2026-09-23

**Status**: Draft

**Input**: User description: "Login de usuarios: permitir que un usuario ya registrado ingrese con email y contraseña y obtenga una credencial JWT equivalente a la del registro. POST /api/usuarios/login, público. Recibe email y contraseña. Devuelve id, nombre, email y token. El email se compara sin distinguir mayúsculas ni espacios externos, igual que en el registro. Credenciales inválidas (email inexistente o contraseña incorrecta) responden 401 con el mismo mensaje genérico, sin revelar cuál falló. Datos ausentes o con formato inválido responden 400. Actualizar OpenAPI y la colección de Postman. Fuera de alcance: recuperación de contraseña, bloqueo por intentos fallidos, refresh tokens."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ingreso de usuario registrado (Priority: P1)

Un usuario que ya tiene una cuenta debe poder ingresar con su email y contraseña y recibir una credencial JWT equivalente a la que obtiene al registrarse.

**Why this priority**: Permite volver a acceder a la aplicación después del registro y habilita el uso de los endpoints protegidos.

**Independent Test**: Se puede registrar un usuario, ejecutar el login con sus credenciales válidas y comprobar que la respuesta contiene su identidad y una credencial utilizable en un endpoint protegido.

**Acceptance Scenarios**:

1. **Given** un usuario registrado con email y contraseña válidos, **When** envía `POST /api/usuarios/login` con esas credenciales, **Then** el sistema responde `200` con `id`, `nombre`, `email` y `token` JWT.
2. **Given** un usuario registrado cuyo email fue almacenado en una forma normalizada, **When** envía el mismo email con mayúsculas o espacios externos, **Then** el sistema identifica la cuenta y permite el ingreso si la contraseña es correcta.
3. **Given** un login exitoso, **When** el usuario utiliza el token devuelto para acceder a un endpoint protegido, **Then** el sistema lo reconoce como autenticado bajo las mismas reglas que la credencial emitida durante el registro.

---

### User Story 2 - Rechazo seguro de credenciales inválidas (Priority: P1)

El sistema debe rechazar intentos con email inexistente o contraseña incorrecta sin revelar qué dato falló.

**Why this priority**: Protege las cuentas y evita que el endpoint se convierta en una fuente de enumeración de usuarios.

**Independent Test**: Se pueden ejecutar intentos con un email inexistente y con una contraseña incorrecta para un email existente, y comparar que ambos devuelven `401` con el mismo mensaje genérico.

**Acceptance Scenarios**:

1. **Given** un email que no pertenece a ningún usuario, **When** se intenta iniciar sesión, **Then** el sistema responde `401` con el mensaje genérico de credenciales inválidas.
2. **Given** un email existente y una contraseña incorrecta, **When** se intenta iniciar sesión, **Then** el sistema responde `401` con exactamente el mismo mensaje genérico usado para un email inexistente.
3. **Given** credenciales inválidas, **When** se inspecciona la respuesta, **Then** no se indica si falló el email o la contraseña ni se devuelve un token.

---

### User Story 3 - Validación del formulario de login (Priority: P1)

El sistema debe informar como datos inválidos las solicitudes incompletas o con email mal formado antes de intentar autenticar al usuario.

**Why this priority**: Ofrece una respuesta clara para errores de entrada sin confundirlos con credenciales rechazadas.

**Independent Test**: Se puede enviar el endpoint sin email, sin contraseña y con un email inválido, comprobando que cada caso responde `400`.

**Acceptance Scenarios**:

1. **Given** una solicitud sin email o sin contraseña, **When** se envía al endpoint de login, **Then** el sistema responde `400` con el formato de error de validación del servicio.
2. **Given** una solicitud con email de formato inválido, **When** se envía al endpoint de login, **Then** el sistema responde `400` y no intenta autenticar credenciales.
3. **Given** una solicitud válida, **When** se procesa el login, **Then** el endpoint público no exige una credencial previa en la cabecera de autorización.

---

### Edge Cases

- Email válido con espacios al inicio o al final.
- Email válido escrito con una combinación distinta de mayúsculas y minúsculas.
- Email vacío, nulo o compuesto solo por espacios.
- Contraseña vacía, nula o con el formato no permitido por las reglas de entrada.
- Solicitud con campos adicionales no requeridos.
- Solicitud sin cuerpo o con contenido que no puede interpretarse como datos de login.
- Email inexistente y contraseña incorrecta deben conservar el mismo estado HTTP y mensaje.
- Un login exitoso no debe devolver la contraseña ni datos sensibles del usuario.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST exponer `POST /api/usuarios/login` como endpoint público.
- **FR-002**: El endpoint MUST aceptar email y contraseña como datos de entrada.
- **FR-003**: El sistema MUST normalizar el email eliminando espacios externos y sin distinguir mayúsculas/minúsculas, usando la misma regla aplicada durante el registro.
- **FR-004**: Para credenciales válidas, el sistema MUST responder `200` con `id`, `nombre`, `email` y `token`.
- **FR-005**: El token devuelto MUST ser una credencial JWT equivalente, en sus permisos de acceso, a la emitida durante el registro.
- **FR-006**: Para un email inexistente, el sistema MUST responder `401`.
- **FR-007**: Para una contraseña incorrecta, el sistema MUST responder `401`.
- **FR-008**: Los casos de email inexistente y contraseña incorrecta MUST usar el mismo mensaje genérico y MUST NOT revelar cuál credencial falló.
- **FR-009**: El sistema MUST responder `400` cuando falte email o contraseña, o cuando el email tenga un formato inválido.
- **FR-010**: Las respuestas de error de autenticación MUST NOT incluir un token ni datos sensibles del usuario.
- **FR-011**: La documentación OpenAPI MUST incluir el endpoint, su solicitud, respuestas `200`, `400` y `401`, y declarar que el login no requiere autenticación previa.
- **FR-012**: La colección de Postman MUST incluir una solicitud funcional para el login con variables o ejemplos suficientes para ejecutarlo y conservar el token devuelto.
- **FR-013**: El alcance MUST excluir recuperación de contraseña, bloqueo por intentos fallidos y refresh tokens.

### Key Entities *(include if feature involves data)*

- **Usuario**: Cuenta existente identificada por email normalizado, con nombre, contraseña protegida e identificador que se devuelve tras un login exitoso.
- **Credencial JWT**: Token emitido después de validar correctamente las credenciales y utilizable para acceder a los endpoints protegidos.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de los intentos con credenciales válidas de usuarios registrados devuelve `200` y los cuatro campos de identidad solicitados junto con un token.
- **SC-002**: El 100% de los intentos con email inexistente o contraseña incorrecta devuelve `401` y exactamente el mismo mensaje genérico entre ambos casos.
- **SC-003**: El 100% de las solicitudes con datos ausentes o email inválido devuelve `400` sin emitir token.
- **SC-004**: Un token obtenido mediante login permite el acceso a los mismos endpoints protegidos que un token obtenido durante el registro, sujeto a su vigencia.
- **SC-005**: OpenAPI y Postman contienen una definición ejecutable y coherente de `POST /api/usuarios/login` antes de considerar terminada la feature.
- **SC-006**: Ningún flujo de esta feature incorpora recuperación de contraseña, bloqueo por intentos fallidos o refresh tokens.

## Assumptions

- El registro ya persiste el email con la misma normalización de espacios externos y mayúsculas/minúsculas que se aplicará al login.
- La contraseña se valida contra la representación protegida existente y nunca se compara ni se devuelve como texto almacenado.
- El mensaje genérico por credenciales inválidas será una única cadena estable definida por el contrato de errores del backend; su contenido exacto no forma parte de esta solicitud.
- La respuesta exitosa reutiliza los nombres de campos del registro (`id`, `nombre`, `email`, `token`) y no incluye `fechaRegistro`.
- Los demás endpoints permanecen sujetos a la regla constitucional de autenticación; únicamente registro y login son públicos.
