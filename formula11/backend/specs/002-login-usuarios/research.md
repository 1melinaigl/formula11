# Research: Login de usuarios

## Decisión 1: Reutilizar la normalización del modelo

- **Decisión**: El service aplicará `Usuario.normalizarEmail(request.email())` antes de llamar a `UsuarioRepository.findByEmail`.
- **Rationale**: El registro ya usa esa función; elimina espacios externos, convierte a minúsculas con `Locale.ROOT` y valida el formato. Reutilizarla mantiene simétricos registro y login y aprovecha el índice único de la base.
- **Alternativas consideradas**: Normalizar en el repositorio o con una consulta `LOWER/TRIM`. Se descartan porque duplican la regla del modelo y pueden hacer que registro y login diverjan.

## Decisión 2: Fallo de autenticación indistinguible

- **Decisión**: El service lanzará una única `CredencialesInvalidasException` tanto cuando `findByEmail` no encuentre usuario como cuando `passwordEncoder.matches` devuelva falso. `GlobalExceptionHandler` la mapeará a `401` usando `ErrorResponse`.
- **Rationale**: Un único tipo y mensaje evita enumeración de usuarios y conserva el formato existente (`mensaje`, `timestamp`, `correlationId`). La excepción se registra antes del handler genérico.
- **Alternativas consideradas**: Devolver `Optional` o dos excepciones distintas. Se descartan porque permiten diferencias observables o trasladan lógica de seguridad al controller.

## Decisión 3: Emisión del token

- **Decisión**: El login llamará a `JwtService.generarToken(usuario.getId(), usuario.getEmail())`, igual que el registro.
- **Rationale**: Produce la misma estructura, subject, claims, firma y expiración que ya consume `JwtAuthenticationFilter`.
- **Alternativas consideradas**: Crear un JWT específico de login o reutilizar el token del registro. Se descartan porque romperían la equivalencia solicitada o no permitirían una nueva sesión.

## Decisión 4: Validación y exposición HTTP

- **Decisión**: Crear records `LoginRequest` y `LoginResponse`; el request usará Bean Validation equivalente al registro y el controller delegará en `UsuarioService`.
- **Rationale**: Spring MVC ya transforma errores de `@Valid` mediante `GlobalExceptionHandler`; el response dedicado evita exponer `fechaRegistro` y cualquier dato sensible.
- **Alternativas consideradas**: Reutilizar `RegistroUsuarioRequest` o devolver `RegistroUsuarioResponse`. Se descartan porque registro requiere `nombre` y su respuesta incluye `fechaRegistro`, que no pertenecen al login.

## Decisión 5: Seguridad y documentación

- **Decisión**: Añadir `requestMatchers(HttpMethod.POST, "/api/usuarios/login").permitAll()` y documentar `security: []` para el endpoint. Mantener `.anyRequest().authenticated()`.
- **Rationale**: Cumple la constitución: solo registro y login son públicos. Las anotaciones de `UsuarioController` alimentan Swagger; el contrato YAML versionado y Postman se actualizan en paralelo.
- **Alternativas consideradas**: Permitir toda la ruta `/api/usuarios/**`. Se descarta porque abriría endpoints futuros o existentes sin autenticación.

## Decisión 6: Estrategia de pruebas

- **Decisión**: Ampliar el test unitario de `UsuarioService`, agregar cobertura de normalización y búsquedas reales en la integración de repositorio/servicio con PostgreSQL Testcontainers, y crear un E2E separado en `com.formula11.e2e`.
- **Rationale**: Coincide con la constitución y con la infraestructura existente: JUnit 5, MockMvc, `@SpringBootTest`, `@DataJpaTest` y PostgreSQL 16.
- **Alternativas consideradas**: Probar solo con mocks o H2. Se descartan porque no verifican el índice/normalización real de PostgreSQL ni el filtro de seguridad completo.
