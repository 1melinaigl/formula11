# Research: Auditoría de web services

**Feature**: [spec.md](spec.md) | **Fecha**: 2026-11-26

## R1. Cómo interceptar las llamadas

- **Decision**: Un aspecto `AuditoriaWebServicesAspect` con `@Around` y el pointcut
  `within(com.formula11.controller..*) && @within(org.springframework.web.bind.annotation.RestController)`.
  Se agrega `spring-boot-starter-aop` (versión manejada por Spring Boot 3.5.5), que activa
  los proxies de AspectJ sin configuración extra.
- **Rationale**: Es lo que pide la consigna. Restringir el pointcut al paquete `controller`
  deja afuera los `@RestController` de springdoc (`/v3/api-docs`) y Actuator, que no son
  `@RestController`. Los controllers nuevos (por ejemplo, el `OfertaController` de A) quedan
  auditados solos (FR-008).
- **Alternatives considered**: un `OncePerRequestFilter` o un `HandlerInterceptor`, que
  verían también los 401, 403 y 400 de validación, pero no tienen acceso directo a los
  argumentos ya convertidos del método y la consigna pide el aspecto.

## R2. Llamadas que no llegan al controller

- **Decision**: No se auditan los pedidos que se rechazan antes de entrar al método del
  controller: 401 y 403 de Spring Security (se cortan en la cadena de filtros) y 400 de
  Bean Validation sobre `@Valid @RequestBody` (se lanzan al resolver los argumentos). Se
  documenta en la spec como caso borde.
- **Rationale**: Es una limitación propia de un aspecto sobre los controllers. Las
  excepciones que se lanzan dentro del método (404, 409, `DatoInvalidoException`, etc.)
  sí pasan por el aspecto y quedan con `resultado=ERROR`.
- **Alternatives considered**: sumar un filtro solo para esos casos. Se descarta para no
  duplicar la auditoría ni mezclar dos mecanismos; si el docente lo pide, se agrega en otra
  iteración.

## R3. Usuario

- **Decision**: `SecurityContextHolder.getContext().getAuthentication()`. Si es `null`, no
  está autenticada o es `AnonymousAuthenticationToken`, el usuario es `anónimo`; si no,
  `authentication.getName()`, que es el email (lo arma `JwtAuthenticationFilter`).
- **Rationale**: Es la misma fuente que usan los controllers (`authentication.getName()`).

## R4. Parámetros y enmascarado

- **Decision**:
  - Nombres desde `MethodSignature.getParameterNames()` (Spring Boot compila con
    `-parameters`).
  - Se omiten los argumentos de infraestructura: `Authentication`, `Principal`,
    `ServletRequest`, `ServletResponse`, `HttpSession`, `BindingResult`/`Errors`.
  - Cada valor se pasa a árbol JSON con el `ObjectMapper` de Spring y se reemplaza por
    `****` cualquier campo cuyo nombre (sin distinguir mayúsculas) sea `password`,
    `contrasena`, `token`, `accessToken`, `refreshToken` o `secret`, a cualquier
    profundidad. Si un valor no se puede serializar, se usa `String.valueOf`.
  - El texto final de los parámetros se recorta a 500 caracteres con la marca `…(recortado)`.
  - Lo hace una clase propia, `EnmascaradorParametros`, sin estado y testeable sin Spring.
- **Rationale**: Enmascarar por nombre de campo cubre `LoginRequest` y
  `RegistroUsuarioRequest` hoy y cualquier DTO futuro con los mismos nombres, sin listar
  clases. Se usan nombres exactos y no "contiene token" para no tapar campos como
  `tokensDisponibles`.
- **Alternatives considered**: `toString()` de los records, que mostraría la contraseña;
  anotar campos con una anotación `@Sensible`, que obliga a tocar los DTO de las demás.

## R5. Formato de la línea

- **Decision**: El aspecto arma un `LineaAuditoria` (record) y loguea su `formatear()` en
  el logger `AUDITORIA` con nivel INFO. La fecha y el correlation ID los pone el patrón de
  logback. Ver [contracts/linea-auditoria.md](contracts/linea-auditoria.md).
- **Rationale**: Formato `clave=valor` en una sola línea, fácil de buscar con `grep` y de
  testear sin logback.
- **Duración**: `System.nanoTime()` antes y después de `proceed()`, en ms. El principio 9
  (Clock) aplica a services; acá se mide un intervalo, no se usa la fecha actual.

## R6. Logback

- **Decision**: `src/main/resources/logback-spring.xml` que:
  - incluye `org/springframework/boot/logging/logback/defaults.xml` y
    `console-appender.xml`, así la consola sigue igual y respeta `logging.pattern.level`
    de `application.yml` (`[corr:%X{correlationId}]`);
  - define un `RollingFileAppender` `AUDITORIA_FILE` con `TimeBasedRollingPolicy`:
    archivo `${AUDITORIA_DIR}/auditoria.log`, patrón de rotación
    `${AUDITORIA_DIR}/auditoria.%d{yyyy-MM-dd}.log`, `maxHistory` 30;
  - patrón `%d{yyyy-MM-dd'T'HH:mm:ss.SSSXXX} [corr:%X{correlationId}] %msg%n`;
  - logger `AUDITORIA` con `additivity="false"`, para que no se repita en la consola.
  - `AUDITORIA_DIR` sale de `<springProperty source="app.auditoria.directorio" defaultValue="logs"/>`.
- **Rationale**: `logs/` ya está en el `.gitignore` del repo. Los tests apuntan el
  directorio a `target/` para no ensuciar el repo.
- **Alternatives considered**: configurarlo solo desde `application.yml` (Spring Boot no
  permite loggers con appender propio sin XML).

## R7. Fallas al auditar

- **Decision**: Toda la preparación de la línea (usuario, enmascarado, formato) va en un
  `try/catch` que, si falla, deja un `warn` en el log general y sigue. La excepción del
  controller se vuelve a lanzar sin tocarla, así `GlobalExceptionHandler` responde igual.
- **Rationale**: FR-007 y SC-004.

## R8. Paquete

- **Decision**: Paquete transversal nuevo `com.formula11.observabilidad`, al mismo nivel que
  `config`, `exception` y `security`. Ahí van el aspecto, `EnmascaradorParametros` y
  `LineaAuditoria`, y después el health indicator y las métricas de la feature 016.
- **Rationale**: La auditoría no es de ningún área funcional y tiene lógica testeable, que
  en `config` quedaría fuera de la cobertura de Sonar (`**/config/**` está excluido).
- **Coordinación**: avisar a C para que `ArquitecturaTest` trate `observabilidad` como
  transversal, igual que `config`.

## R9. Tests

- **Unit** (`unit/observabilidad/`): `EnmascaradorParametrosTest` (contraseñas, tokens,
  anidados, nulos, recorte, valores no serializables), `LineaAuditoriaTest` (formato OK y
  ERROR) y `AuditoriaWebServicesAspectTest` (con `ProceedingJoinPoint` de Mockito y un
  `ListAppender` en el logger `AUDITORIA`: usuario autenticado y anónimo, error relanzado,
  falla interna que no rompe).
- **Integración** (`integration/observabilidad/`): `@SpringBootTest` con perfil `test`, llama
  a beans de controllers (proxies con el aspecto) y, con un `ListAppender` en `AUDITORIA`,
  verifica la línea en un caso OK, uno con error y un registro con contraseña enmascarada.
  El archivo lo verifica el E2E.
- **E2E** (`e2e/observabilidad/`): login por HTTP con MockMvc y un `X-Correlation-Id` fijo;
  verifica que la línea del archivo tiene ese correlation ID y la contraseña enmascarada.
  Va en `e2e` porque la constitución exige que los tests con MockMvc vivan ahí.
