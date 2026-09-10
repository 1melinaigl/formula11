# Research: Registro de usuarios y catálogo de jugadores

## Decisiones

### Spring Boot 3.5.x sobre Java 21

Se usará la última versión estable disponible de Spring Boot 3.5.x al momento de generar `pom.xml`, manteniendo Java 21 y Maven. Esto satisface el stack solicitado sin acoplar el plan a una versión futura de Spring Boot 4.x.

### JWT stateless con filtro propio

Spring Security configurará sesiones stateless y un `OncePerRequestFilter` que extrae `Authorization: Bearer <token>`, valida firma y expiración, y crea la autenticación en el `SecurityContext`. El filtro no debe ejecutar lógica de negocio ni consultar al controller. El endpoint de registro se permitirá sin autenticación; el resto se bloqueará por defecto.

### Contraseña y email

`PasswordEncoder` BCrypt se aplicará antes de persistir. El email se normaliza con trim y lowercase antes de validar unicidad; además habrá restricción única en PostgreSQL para proteger el caso de concurrencia. El password no formará parte de DTOs de respuesta ni de `toString`.

### Persistencia y migraciones

Se usará Spring Data JPA/Hibernate con PostgreSQL. Flyway será el mecanismo de versionado del esquema para evitar que el modelo cree silenciosamente tablas distintas entre entornos. El perfil `local` apunta a `jdbc:postgresql://localhost:5432/formula11` con `postgres/root`; el perfil `test` usa únicamente la URL dinámica del contenedor.

### Carga del catálogo

La administración/carga de jugadores queda fuera de esta feature. Se incorporará una migración de datos mínima solo si se necesitan fixtures reproducibles para quickstart; el catálogo vacío sigue siendo una respuesta válida. Toda escritura de `Jugador` debe validar `Liga` mediante el enum cerrado.

### Errores y observabilidad

`@RestControllerAdvice` devolverá un contrato único de error en español para validación, duplicados, JWT y recursos. Un filtro de correlation ID propagará `X-Correlation-Id` o generará uno, y añadirá el valor a MDC y a la respuesta. Actuator expondrá health y métricas; no se registrarán passwords ni tokens completos.

## Riesgos y mitigaciones

- **Carrera de emails duplicados**: normalización en service más índice único en DB y traducción de `DataIntegrityViolationException` a `409`.
- **JWT mal configurado**: secreto obligatorio en configuración y prueba de firma, expiración y ausencia de credencial.
- **Tests usando la DB local**: perfil `test` sin credenciales locales y `@ServiceConnection`/propiedades dinámicas de Testcontainers.
- **Catálogo sin datos**: fixture controlado en tests; el endpoint devuelve `200 []` cuando no hay jugadores.
