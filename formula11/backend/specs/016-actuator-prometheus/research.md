# Research: Actuator y Prometheus

**Feature**: [spec.md](spec.md) | **Fecha**: 2026-11-26

## R1. Qué se expone y con qué configuración

- **Decision**: `management.endpoints.web.exposure.include: health,info,metrics,prometheus,loggers`.
  Se agrega `micrometer-registry-prometheus` (versión manejada por Spring Boot 3.5.5), que
  activa `/actuator/prometheus` solo. El detalle del health pasa de `when_authorized` a
  `when_authorized` con `roles: SUPERUSUARIO`.
- **Rationale**: Es la lista de la consigna. Sin `roles`, `when_authorized` le muestra el
  detalle a cualquier usuario autenticado. Con `roles`, Spring Boot pregunta
  `isUserInRole("SUPERUSUARIO")`, que con el prefijo `ROLE_` coincide con la autoridad que
  arma `JwtAuthenticationFilter`.
- **Alternatives considered**: exponer todo con `*`. Se descarta: `env`, `beans`,
  `configprops` y `heapdump` muestran secretos y no los pide la consigna.

## R2. Seguridad de `/actuator/**`

- **Decision**: En `SecurityConfig`, un bloque aparte y comentado (acuerdo del grupo), antes
  de `anyRequest()`:
  `EndpointRequest.to(HealthEndpoint.class).permitAll()` y
  `EndpointRequest.toAnyEndpoint().hasRole("SUPERUSUARIO")`. Se quita
  `/actuator/health` de la lista de rutas públicas, porque el bloque nuevo lo reemplaza.
  Se agrega un `accessDeniedHandler` que responde 403 con el mismo `ErrorResponse` JSON
  que el `authenticationEntryPoint`.
- **Rationale**: `EndpointRequest` sigue el `base-path` y los ids de los endpoints, así que
  no hay rutas escritas a mano. `to(HealthEndpoint)` incluye `/actuator/health/**`, que
  solo muestra estados (sin detalle para quien no es superusuario). El
  `accessDeniedHandler` solo cambia lo que ve quien queda bloqueado por URL, y hoy ninguna
  ruta bloquea a un usuario autenticado salvo las de Actuator. Los 403 de `@PreAuthorize`
  siguen pasando por `GlobalExceptionHandler`, sin cambios.
- **Alternatives considered**: matchers de texto `/actuator/**`. Funcionan, pero se rompen
  si cambia el `base-path`. Una segunda `SecurityFilterChain` solo para Actuator duplica la
  configuración del JWT.

## R3. Información de la aplicación

- **Decision**: En `application.yml`, `management.info.env.enabled: true` e
  `info.app.nombre: Formula11` / `info.app.version: @project.version@`.
- **Rationale**: El parent de Spring Boot filtra `application.yml` con delimitadores `@`,
  así que la versión sale del `pom.xml` sin tocarlo. En Spring Boot 3 el contribuidor
  `env` del info viene apagado.
- **Alternatives considered**: el goal `build-info` del `spring-boot-maven-plugin`. Implica
  cambiar la configuración del plugin en el `pom.xml`, y el acuerdo es que cada integrante
  solo agrega su dependencia.

## R4. Health indicator de la carga de métricas

- **Decision**: `CargaMetricasHealthIndicator implements HealthIndicator` en
  `com.formula11.observabilidad`; el nombre del bean da el componente `cargaMetricas`. Le
  pide a `CargaMetricasService.ultimaTerminada()` (método nuevo) la última ejecución en
  `COMPLETADA`, `PARCIAL` o `FALLIDA`, por la consulta derivada
  `EjecucionMetricasRepository.findFirstByEstadoInAndFinNotNullOrderByFinDescIdDesc(...)`. `FALLIDA` da
  `DOWN`, y el resto o la ausencia de ejecuciones, `UP`. Los detalles son `estado`, `fin`
  y `ejecucionId`, más `error` si falló. Si no hay ejecuciones, `detalle: "Sin cargas de métricas"`.
- **Rationale**: El indicador expone información, igual que un controller, así que pasa por
  el service y no lee el repositorio. Se ordena por `fin` y no por `id`, porque la
  secuencia de `ejecucion_metricas` tiene `allocationSize = 50`. Con varios contextos de
  Spring en la misma base (como en la suite de tests), un id más alto no garantiza una carga
  más nueva. Las que no tienen `fin` quedan afuera, porque PostgreSQL pone los `NULL`
  primero en un orden descendente, y `id` desempata. `EN_CURSO` no cuenta (caso borde de
  la spec). Si la consulta
  falla, Spring Boot ya convierte la excepción en `DOWN` con el error, sin romper el health.
- **Alternatives considered**: `OUT_OF_SERVICE` o un estado propio `DEGRADED`. Se descartan:
  el ejemplo de la consigna es "si falló la última sincronización", y `DOWN` es lo que
  entienden Prometheus y cualquier monitor. Un `HealthIndicator` reactivo no aplica.
- **Nota**: Al arrancar, `CargaMetricasService` marca como `FALLIDA` las cargas que
  quedaron `EN_CURSO` por un reinicio. Después de un reinicio en medio de una carga, el
  health da `DOWN` hasta la próxima carga que salga bien. Es correcto: esa carga no se
  terminó.

## R5. Cómo se cuentan las operaciones y se mide el recálculo

- **Decision**: Un aspecto `MetricasNegocioAspect` en `com.formula11.observabilidad`:
  - `@AfterReturning` sobre `MercadoService.comprar(..)` y `MercadoService.vender(..)`.
    Toma el tipo de la `Transaccion` del `ResultadoOperacion` devuelto y llama a
    `MetricasNegocio.registrarOperacionMercado(tipo)`.
  - `@Around` sobre `RecalculoService.recalcular(..)`. Mide con un `Timer.Sample` y lo
    cierra con `resultado=ok` o `resultado=error`.
- **Rationale**: `MercadoServiceTest`, `RecalculoServiceTest` y `RecalculoServiceSenalesTest`
  construyen esos services con `new`. Sumarles un `MeterRegistry` al constructor obligaría
  a cambiar tests existentes, y la constitución lo prohíbe sin permiso. El aspecto no toca
  los services y cubre también el recálculo del `CotizacionScheduler`, que llama al mismo
  método. Las compras rechazadas lanzan excepción y `@AfterReturning` no corre (FR-006).
  `comprar(a, b, c)` no llama al otro `comprar` por el proxy, así que no hay doble conteo.
- **Alternatives considered**: llamar a `MeterRegistry` dentro de los services (lo más
  directo, pero rompe los tests existentes). `@Timed` con `TimedAspect` sirve para el
  timer, pero no para un contador con el tipo sacado del resultado. Se prefiere un único
  aspecto propio para las dos cosas.

## R6. Contar solo lo confirmado

- **Decision**: `MetricasNegocio.registrarOperacionMercado(tipo)` incrementa después del
  commit si hay una transacción activa (`TransactionSynchronizationManager.registerSynchronization`
  con `afterCommit`). Si no hay transacción, incrementa en el momento. El aspecto se
  ordena por fuera de `@Transactional`, con `@Order(Ordered.HIGHEST_PRECEDENCE)`.
- **Rationale**: Una operación que falla en el commit no debe sumar (SC-003). Con el aspecto
  por fuera de la transacción, el commit ya ocurrió cuando corre `@AfterReturning`. El
  chequeo de transacción protege a quien llame directo desde un método transaccional, como
  el P2P de A.
- **Alternatives considered**: `@TransactionalEventListener`. Pide publicar un evento desde
  el service y vuelve al problema de R5.

## R7. Métrica por tipo, con los tres tipos desde el arranque

- **Decision**: El enum `TipoOperacionMercado { COMPRA, VENTA, P2P }` con la etiqueta en
  minúsculas. `MetricasNegocio` registra los tres `Counter` en el constructor, así que
  aparecen en cero desde el arranque (FR-006). Nombres en Micrometer:
  - `formula11.mercado.operaciones` (counter, tag `tipo`) → en Prometheus
    `formula11_mercado_operaciones_total{tipo="compra|venta|p2p"}`.
  - `formula11.cotizaciones.recalculo` (timer, tag `resultado`) → en Prometheus
    `formula11_cotizaciones_recalculo_seconds_count|sum|max{resultado="ok|error"}`.
- **Rationale**: Un solo nombre con etiqueta permite sumar o filtrar por tipo en Prometheus.
  El prefijo `formula11` separa las métricas de negocio de las de la plataforma.
  `Transaccion.Tipo.COMPRA/VENTA` se traduce al enum propio para que el P2P de A no
  dependa del modelo de mercado.
- **Alternatives considered**: tres contadores con nombres distintos (no se pueden agregar
  en una consulta) o crear el contador recién en el primer uso (el `p2p` no aparecería
  hasta que A lo integre).

## R8. Las métricas nunca rompen una operación

- **Decision**: El cuerpo de `@AfterReturning` y el registro del timer van en `try/catch`
  de `RuntimeException`, con un `warn` en el logger de la clase. El `@Around` del recálculo
  vuelve a lanzar la excepción del método tal cual.
- **Rationale**: FR-009. Es el mismo criterio que la auditoría (015).

## R9. Tests

- **Decision**:
  - Unit (sin Spring): `MetricasNegocioTest` con `SimpleMeterRegistry` (contadores en cero,
    incremento, incremento después del commit simulado con
    `TransactionSynchronizationManager.initSynchronization()`).
    `CargaMetricasHealthIndicatorTest` con el service mockeado (los cuatro casos de la US1).
    `MetricasNegocioAspectTest` con `ProceedingJoinPoint` mockeado (ok, error y relanzado).
  - Integración (perfil `test`, Testcontainers): `MetricasNegocioIntegrationTest`, donde una
    compra y una venta reales suben los contadores, una compra rechazada no y un recálculo
    suma al timer. `CargaMetricasHealthIntegrationTest` guarda una ejecución `FALLIDA` y
    después una `COMPLETADA`, y lee el `HealthIndicator`.
  - E2E (perfil `e2e`, MockMvc): `ActuatorSeguridadE2ETest`. Recorre health, info, metrics,
    prometheus y loggers con tres sesiones (sin sesión, usuario y superusuario) y prueba
    que el health público no muestra detalles. `ActuatorPrometheusE2ETest` usa
    `@AutoConfigureObservability` para tener el registry de Prometheus en el test, compra
    por HTTP y lee el texto. El cambio de nivel del logger `AUDITORIA` se hace por
    `POST /actuator/loggers/AUDITORIA` y se restaura al final.
- **Rationale**: En `@SpringBootTest`, Spring Boot apaga los exportadores de métricas y usa
  un `SimpleMeterRegistry`. Para ver `/actuator/prometheus` hace falta
  `@AutoConfigureObservability`. Los tests que dejan una ejecución `FALLIDA` la borran al
  final para no dejar el health en `DOWN` para otros tests del mismo contexto.

## R10. Prometheus local (opcional)

- **Decision**: `formula11/backend/docker/prometheus/` con `docker-compose.yml` (imagen
  `prom/prometheus` con versión fija), `prometheus.yml` (scrape cada 15 s de
  `host.docker.internal:8080/actuator/prometheus`, con `authorization.credentials_file`) y
  `token.example`. El token real va en `docker/prometheus/token`, que se agrega al
  `.gitignore`.
- **Rationale**: El endpoint pide superusuario (FR-004), así que Prometheus manda un JWT. El
  JWT vence (`app.jwt.expiration`, una hora por defecto) y la guía explica cómo renovarlo
  con un login. Prometheus relee el `credentials_file` en cada scrape, así que no hace falta
  reiniciarlo. Es una herramienta de demo, no se usa en los tests.
- **Alternatives considered**: abrir `/actuator/prometheus` sin autenticación (contradice la
  consigna) o una API key propia para Prometheus (la constitución dice que la credencial es
  el JWT).
