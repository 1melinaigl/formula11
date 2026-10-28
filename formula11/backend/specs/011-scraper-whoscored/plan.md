# Implementation Plan: Scraper de métricas por partido de WhoScored

**Branch**: `011-scraper-whoscored` | **Date**: 2026-10-27 |
**Spec**: [spec.md](spec.md)

## Summary

Implementar la carga de métricas por partido de las cinco ligas soportadas,
separando el puerto externo, los adapters de descarga y archivos, el parser
puro y la orquestación transaccional. La carga manual será asincrónica,
exclusiva para `SUPERUSUARIO`, observable mediante el contrato de la Entrega 2
y reutilizable por el scheduler a través de `ActualizacionMetricasPort`.

La solución conservará la compatibilidad con las migraciones V1–V3 y V10–V12,
con las entidades y los cálculos de la feature 006 y con los tests existentes.
Las migraciones nuevas serán únicamente V13–V17; V18 y V19 quedan libres. Los tests automáticos no
abrirán navegador ni usarán red: el parser y el adapter de archivos usarán el
fixture local.

## Technical Context

**Language/Version**: Java 21, Spring Boot 3.5.5

**Persistence**: Spring Data JPA, PostgreSQL, Flyway, Hibernate batch inserts
con secuencias `allocationSize = 50`.

**Web/Security**: Spring MVC, Spring Security JWT, Springdoc OpenAPI,
`@PreAuthorize("hasRole('SUPERUSUARIO')")`, `GlobalExceptionHandler` y
`ErrorResponse`.

**External data**: Playwright para Java con Chrome persistente. El endpoint de
calendario se consulta desde una página WhoScored mediante `page.evaluate` +
`fetch` para conservar las cookies. `WhoScoredProperties` expone `pausaMs` y
`timeoutMs` como `double` en milisegundos, validados como positivos, porque
Playwright recibe esos valores directamente:

```text
https://www.whoscored.com/tournaments/{stageId}/data/?d={yyyyMM}&isAggregate=false
```

**New dependency**: agregar únicamente `com.microsoft.playwright:playwright`
con una versión fijada en una propiedad del `pom.xml`. No agregar clientes HTTP,
parseadores JSON ni frameworks adicionales.

**Existing constraints**:

- [Jugador.java](../../src/main/java/com/formula11/model/Jugador.java) ya tiene
  `externalProvider`/`externalId`; se agregará `whoscoredId` separado y nullable,
  porque el contrato de vinculación de esta feature exige unicidad específica
  para WhoScored sin alterar la identidad externa de Football-Data.
- [MetricaPartido.java](../../src/main/java/com/formula11/model/MetricaPartido.java)
  y V10 ya tienen la mayoría de las métricas. Se agregarán únicamente columnas
  de partido externo que necesite esta feature, nullable o con default.
- [ActualizacionMetricasPort.java](../../src/main/java/com/formula11/service/ActualizacionMetricasPort.java)
  hoy expone `void actualizar()`. Esta interfaz no se modifica: conserva
  exactamente ese único método porque la consume el scheduler de la feature
  006. La carga asincrónica, sus estados y el resumen por liga pertenecen a
  `CargaMetricasService`; la implementación real del port solo delega en la
  carga incremental sincrónica.
- [SincronizacionResponse.java](../../src/main/java/com/formula11/dto/SincronizacionResponse.java)
  es la respuesta existente de sincronización de Football-Data y no se
  modifica. Crear DTOs nuevos para los endpoints de métricas:
  `SincronizacionMetricasRequest`, `EjecucionMetricasResponse` y
  `PendientesPageResponse` con `PendienteVinculacion`.
- Regla de compatibilidad con las features 004, 006, 007 y 009: se permite
  agregar métodos de consulta nuevos en `JugadorRepository` y
  `MetricaPartidoRepository`, el campo nullable `whoscoredId` en `Jugador`, la
  relación nullable en `MetricaPartido` y quitar `@Component` a
  `NoOpActualizacionMetricasAdapter`. Está prohibido cambiar o borrar firmas,
  métodos, campos o comportamiento existentes que usen esas features. No se
  modifican sus DTOs ni tests existentes.
- [GlobalExceptionHandler.java](../../src/main/java/com/formula11/exception/GlobalExceptionHandler.java)
  ya traduce `IllegalStateException` a `409`, `AccessDeniedException` a `403`,
  validación a `400` y `NoSuchElementException` a `404`; reutilizar esos
  patrones y agregar excepciones específicas solo si mejoran el mensaje.
- [application-test.yml](../../src/test/resources/application-test.yml) debe
  fijar `app.whoscored.modo=archivos`, carpeta de fixture con subcarpetas por
  enum `Liga` y valores seguros,
  para que el contexto de test nunca cree el adapter de Playwright.
- Spring Flyway está configurado con `out-of-order` en tests porque el
  repositorio ya contiene V20+. V13–V17 deben ser compatibles con esa
  instalación y no modificar migraciones aplicadas.

## Constitution Check

- [x] **Arquitectura en capas**: controller delega en service; service
      orquesta persistencia y adapters; parser y reglas de transformación son
      independientes de Spring.
- [x] **Adapters externos**: WhoScored queda detrás de `WhoScoredPort`; el
      parser no conoce Playwright, red ni base de datos.
- [x] **Tiempo controlable**: services reciben `Clock`; no usan
      `LocalDate.now()`/`Instant.now()` directamente. `Clock` permanece UTC y
      las fechas se convierten en `ClockConfig.ZONA_NEGOCIO`.
- [x] **Seguridad**: las tres operaciones del contrato requieren JWT y rol
      `SUPERUSUARIO`; el mercado no dispara scraping.
- [x] **Observabilidad**: MDC/correlationId se propaga al executor asincrónico;
      los logs incluyen ejecución, liga, partido, estado y cantidades.
- [x] **Calidad**: parser unitario offline, integración con Testcontainers y
      controller/E2E con MockMvc; no se modifican ni eliminan tests existentes.
- [x] **Compatibilidad de datos**: V1–V3 y V10–V12 son inmutables; columnas
      nuevas nullable/default y migraciones nuevas V13–V17; V18 y V19 quedan
      libres.

## Architecture and Design

### Package layout

```text
src/main/java/com/formula11/
├── adapter/
│   ├── WhoScoredPort.java
│   ├── WhoScoredPlaywrightAdapter.java
│   ├── WhoScoredArchivoAdapter.java
│   └── WhoScoredException.java
├── config/
│   ├── AsyncConfig.java
│   └── WhoScoredProperties.java
├── controller/
│   └── MetricasController.java
├── dto/
│   ├── SincronizacionMetricasRequest.java
│   ├── EjecucionMetricasResponse.java
│   ├── LigaEjecucionResponse.java
│   ├── PendienteVinculacionResponse.java
│   └── PendientesPageResponse.java
├── model/
│   ├── PartidoExterno.java
│   ├── PendienteVinculacion.java
│   ├── EjecucionMetricas.java
│   ├── MetricaJugadorPartido.java
│   └── whoscored/
│       ├── MatchCentreParser.java
│       ├── PartidoWhoscored.java
│       ├── JugadorWhoscored.java
│       └── MetricaWhoscored.java
├── persistence/
│   ├── PartidoExternoRepository.java
│   ├── PendienteVinculacionRepository.java
│   └── EjecucionMetricasRepository.java
└── service/
    ├── CargaMetricasService.java
    ├── CargaPartidoService.java
    ├── MetricasExecutionService.java
    ├── JugadorVinculacionService.java
    └── ActualizacionMetricasAdapter.java
```

Los nombres pueden ajustarse a las convenciones del código al implementar,
pero se deben mantener las responsabilidades y los límites de cada paquete.
No poner lógica de parseo dentro del controller, repository o entity.

### External port and source adapters

Definir records simples para que el puerto no exponga tipos de Playwright:

- `PartidoWhoscoredResumen`: `idExterno`, `startTime`, `equipoLocal`,
  `equipoVisitante`, `elapsed`, `liga`.
- `DatosPartidoWhoscored`: `idExterno`, `matchCentreData`,
  `matchCentreEventTypeJson`.
- `WhoScoredPort`:
  - `List<PartidoWhoscoredResumen> listarPartidos(Liga liga, YearMonth mes)`.
  - `DatosPartidoWhoscored obtenerDatos(long partidoId)`.

`WhoScoredPlaywrightAdapter`:

1. Recibir `WhoScoredProperties` y no crear navegador en el constructor.
2. Exponer un scope de carga (`abrirCarga`/`cerrarCarga` o equivalente) para
   crear un único `BrowserContext` persistente por ejecución y reutilizarlo
   para todos los meses y partidos.
3. Usar `launchPersistentContext(Path.of(perfilDir), ...)`, `setChannel("chrome")`,
   `setHeadless(headless)`, `setIgnoreDefaultArgs(List.of("--enable-automation"))`
   y `--disable-blink-features=AutomationControlled`.
4. Registrar un init script que deje `navigator.webdriver` indefinido.
5. Navegar con `WaitUntilState.COMMIT`, timeout configurable y reintentos de
   `page.evaluate` mientras la página continúa navegando.
6. Para listar, abrir una página de `whoscored.com` y evaluar un `fetch` al
   calendario mensual. Parsear `tournaments[].matches[]` con Jackson y devolver
   `FT` junto con los demás estados para que el service pueda contabilizar
   omitidos.
7. Para el partido, leer desde JavaScript:

   ```javascript
   require.config.params.args.matchCentreData
   require.config.params.args.matchCentreEventTypeJson
   ```

8. Aplicar pausa configurable entre partidos y cerrar páginas, contexto y
   Playwright en `finally`. Un captcha, timeout o formato inesperado debe
   producir una excepción de adapter asociada al partido, no un fallback
   silencioso.

`WhoScoredArchivoAdapter`:

- Activarse con `app.whoscored.modo=archivos`.
- Leer `app.whoscored.carpeta-paginas/{LIGA}/`, donde `{LIGA}` es exactamente
  el nombre del enum `Liga`, por ejemplo `LA_LIGA`.
- Leer `.json` con las dos claves del fixture y `.html` extrayendo ambos
  objetos desde el contenido guardado.
- Tomar la liga de la subcarpeta y la fecha del `startTime` de
  `matchCentreData`; resolver el archivo por `idExterno` y fallar
  explícitamente si no existe o si falta una clave.
- No crear Playwright, contexto, página ni realizar red.

Usar `@ConditionalOnProperty` para que solo exista un adapter activo. El puerto
debe inyectarse sin `@Primary` ambiguo.

### Configuration and async execution

Crear `WhoScoredProperties` con
`@ConfigurationProperties(prefix = "app.whoscored")` y validación de:

- `modo`: `automatico` o `archivos`.
- `ligas`: mapa de `Liga` a `stageId`.
- `inicioTemporada`: `LocalDate`, default `2026-08-01`.
- `zonaFuente`: `ZoneId`, default `Europe/London`.
- `perfilDir`: directorio configurable, por defecto bajo el home del usuario.
- `headless`: default `false`.
- `pausa`, `timeout`, `reintentos`.
- `carpetaPaginas`.

Agregar valores documentados en `application.yml` sin credenciales ni rutas
locales obligatorias. En `application-test.yml`, seleccionar archivos y el
fixture local.

Crear `AsyncConfig`:

- `ThreadPoolTaskExecutor` con un único hilo, cola limitada y nombre
  identificable.
- `TaskDecorator` que copie y restaure el MDC completo, especialmente
  `correlationId`.
- `@EnableAsync`.

El lock de carga debe ser en memoria y proteger también el job semanal. La
persistencia de la ejecución evita perder el estado operativo. Un componente
de startup (`@EventListener(ApplicationReadyEvent)`) debe cambiar
`EN_CURSO` a `FALLIDA` antes de aceptar nuevas cargas.

La orquestación contra `WhoScoredPort` se implementa y prueba con el adapter de
archivos; no depende del adapter Playwright ni del parser de calendario para
ser integrada. T021 se verifica manualmente con red y navegador; T022 es una
clase pura y se valida offline mediante T023.

### Pure parser and player linking

`MatchCentreParser` debe ser una clase pura, sin anotaciones Spring/JPA, que
reciba los dos strings JSON y devuelva:

- equipos, marcador y `expandedMaxMinute`;
- jugadores que efectivamente jugaron;
- una salida por jugador con `playerId`, nombre, equipo, titularidad, minutos,
  métricas acumuladas, rating final y eventos derivados;
- información suficiente para calcular goles recibidos y valla invicta del
  arquero.

Reglas implementadas literalmente desde la spec:

- minutos desde `0` o `subbedInExpandedMinute`, hasta
  `subbedOutExpandedMinute` o `expandedMaxMinute`;
- suplente con `stats` vacío no genera salida;
- sumar por minuto `shotsTotal`, `passesKey`, `dribblesWon`,
  `tackleSuccessful`, `interceptions`, `totalSaves`;
- rating final = rating del minuto más alto;
- goles `Goal` excepto `goalOwn`;
- asistencias desde `satisfiedEventsTypes` traducido por
  `matchCentreEventTypeJson`;
- tarjetas `Yellow`, `Red`, `SecondYellow`;
- goles recibidos del rival mientras el arquero estuvo en cancha;
- valla invicta = arquero con cero goles recibidos y al menos 60 minutos.

Crear `NormalizadorNombre` puro con eliminación de diacríticos,
`Locale.ROOT`, trim y colapso de espacios. `JugadorVinculacionService`:

1. Filtra por liga y por nombre completo normalizado.
2. Si hay un único candidato, vincula.
3. Si hay varios, compara equipo normalizado por contención en cualquiera de
   los sentidos para resolver diferencias como `Arsenal`/`Arsenal FC`.
4. Si queda más de uno o ninguno, crea/actualiza un pendiente y no vincula.
5. Si el catálogo ya tiene `whoscored_id`, reutiliza esa relación antes de
   buscar por nombre.

No usar posición como desempate. Guardar `whoscored_id` solo cuando la
resolución es inequívoca y respetar la unicidad parcial.

### Persistence and migrations

Usar entidades JPA con secuencias y `allocationSize = 50` para tablas de
muchas filas. No llamar `deleteAll` indiscriminadamente en tests ni en la
aplicación.

Migraciones propuestas, todas nuevas y en orden V13–V17:

- **V13** `partido_externo`: `id`, `fuente`, `id_externo`, `liga`, `fecha`,
  equipos, `cargado_en`, estado/error opcionales; unique `(fuente,id_externo)`.
- **V14** `ALTER TABLE jugadores`: `whoscored_id` nullable y unique parcial
  `WHERE whoscored_id IS NOT NULL`.
- **V15** `ALTER TABLE metrica_partido`: `partido_externo_id` nullable y
  `whoscored_id` nullable si el modelo necesita conservar el valor histórico;
  foreign key e índice sin modificar la unique existente de 006.
- **V16** `ejecucion_metricas` y su resumen por liga: id, estado, inicio, fin
  nullable, correlation_id, error y tabla hija con liga, estado, cargados,
  omitidos, vinculados, pendientes y error nullable.
- **V17** `pendiente_vinculacion`: whoscored_id, nombre, equipo, liga,
  detectado_en, último error y estado/resuelto nullable; índices para listar
  pendientes y deduplicar el pendiente activo.

V18 y V19 quedan libres; no crear migraciones de relleno. No modificar V1–V3
ni V10–V12.

Revisar antes de implementar si el nombre real de la tabla y los tipos de
`Liga`/fechas requieren adaptar SQL. Mantener la unique actual
`(jugador_id, fecha_partido)` de V10, porque la idempotencia nueva se garantiza
por `partido_externo` y `partido_externo_id` nullable sin romper datos de 006.

### Load orchestration and transactions

`CargaMetricasService`:

1. Validar `desde` y obtener la fecha de negocio desde el `Clock`.
2. Seleccionar meses: mes actual/anterior para incremental; todos desde
   `inicioTemporada` para histórica; respetar `desde` inclusiva.
3. Recorrer las cinco ligas y sus `stageId`.
4. Abrir una única sesión del adapter automático por carga.
5. Listar, filtrar `elapsed = FT` y descartar ids existentes en
   `partido_externo`.
6. Para cada partido llamar a `CargaPartidoService` en su propia transacción.
7. Actualizar el resumen por liga y logs estructurados después de cada partido.
8. Clasificar la ejecución como `COMPLETADA`, `PARCIAL` o `FALLIDA` sin
   deshacer los partidos confirmados.

`CargaPartidoService`:

- obtener datos crudos, parsear y resolver vinculaciones fuera de la
  transacción;
- delegar a un componente/método transaccional únicamente la verificación de
  idempotencia y el guardado de métricas, pendientes y `partido_externo`;
- guardar las métricas vinculadas en batch y actualizar contadores dentro de la
  transacción de persistencia;
- dejar el error asociado al partido si falla fuera o dentro de esa operación,
  sin borrar partidos anteriores.

La inserción de un partido completo debe ser atómica. Un error de un partido
no debe borrar partidos anteriores. Si el parser no produce jugadores jugados,
marcar el partido como omitido/sin estadísticas según el estado definido en el
modelo.

### Manual endpoint and scheduler integration

Crear `MetricasController` con `@Operation` y `@ApiResponse` para:

- `POST /api/metricas/sincronizacion`: validar `SincronizacionMetricasRequest`,
  capturar/generar `X-Correlation-Id`, adquirir lock, persistir ejecución y
  devolver `202` con `EjecucionMetricasResponse`.
- `GET /api/metricas/sincronizacion/{ejecucionId}`: devolver estado y resumen o
  `404`.
- `GET /api/metricas/pendientes`: usar `Pageable`/page-size del contrato y
  devolver `PendientesPageResponse`.

Los tres métodos usarán `@PreAuthorize("hasRole('SUPERUSUARIO')")`. La
excepción de lock se traducirá a `409` mediante `ErrorResponse`. Mantener
`401`, `403`, `404` y `400` conforme al contrato y no agregar endpoints fuera
del contrato.

Crear `ActualizacionMetricasAdapter` como implementación de
`ActualizacionMetricasPort`, sin modificar la interfaz:

- `actualizar()` ejecuta únicamente la carga incremental sincrónica de
  `CargaMetricasService`. No devuelve estados, resumen ni `CompletableFuture`;
  el scheduler de 006 conserva su contrato actual.
- Si el lock está ocupado, loguea el conflicto y retorna sin iniciar otra.
- Si WhoScored falla, lanza/loguea según el contrato del scheduler para que
  `CotizacionScheduler` recalcule con las métricas persistidas.
- No duplicar la lógica: el endpoint manual crea la ejecución y dispara el
  método asincrónico de `CargaMetricasService`; el scheduler llama al port,
  que solo invoca la variante incremental sincrónica del mismo service.

No modificar los tests de 006 que instancian
`NoOpActualizacionMetricasAdapter`. No borrar esa clase: conservará su API y
se quitará únicamente `@Component`, para que la implementación real sea el
único bean productivo de `ActualizacionMetricasPort`. No tocar el
comportamiento de `CotizacionScheduler`: debe seguir capturando la falla de
actualización y llamando a `RecalculoService`.

### Error handling and observability

- Crear una excepción de adapter con partido/liga/causa y una excepción de
  conflicto de carga con mensaje funcional en español.
- No usar catches amplios que conviertan un éxito en fallback silencioso.
- Log estructurado de inicio, listado, partido omitido, partido cargado,
  pendiente, error y finalización con `correlationId`.
- Propagar MDC con `TaskDecorator` y limpiar el contexto en `finally`.
- Los datos sensibles de Chrome, cookies y credenciales no se escriben en
  logs ni en el repositorio.
- El `GlobalExceptionHandler` debe devolver `ErrorResponse` con status y
  correlationId para errores nuevos sin romper handlers existentes.

## File Change Map

### Files to add

- `pom.xml` dependency/property de Playwright.
- `src/main/java/com/formula11/adapter/WhoScoredPort.java`
- `src/main/java/com/formula11/adapter/WhoScoredPlaywrightAdapter.java`
- `src/main/java/com/formula11/adapter/WhoScoredArchivoAdapter.java`
- `src/main/java/com/formula11/adapter/CalendarioWhoScoredParser.java`
- `src/main/java/com/formula11/adapter/WhoScoredException.java`
- `src/main/java/com/formula11/config/WhoScoredProperties.java`
- `src/main/java/com/formula11/config/AsyncConfig.java`
- `src/main/java/com/formula11/controller/MetricasController.java`
- DTOs de request/ejecución/liga/pendientes.
- Entidades `PartidoExterno`, `EjecucionMetricas`,
  `EjecucionMetricaLiga`, `PendienteVinculacion`.
- Parser puro, records de salida y normalizador.
- Repositories y services de carga/vinculación.
- Migraciones `V13__...sql` a `V17__...sql`.
- Tests unitarios, integración y E2E/MockMvc.
- `specs/011-scraper-whoscored/research.md`
- `specs/011-scraper-whoscored/quickstart.md`

### Files to modify

- `src/main/java/com/formula11/model/Jugador.java`: campo nullable
  `whoscoredId`, getter y mapeo.
- `src/main/java/com/formula11/model/MetricaPartido.java`: referencia
  nullable a `PartidoExterno`/identificador externo si corresponde.
- `src/main/java/com/formula11/persistence/JugadorRepository.java` y
  `MetricaPartidoRepository.java`: consultas de vinculación e idempotencia.
- `src/main/java/com/formula11/service/ActualizacionMetricasPort.java`: no
  modificar; se documenta como contrato estable `void actualizar()`.
- `src/main/java/com/formula11/service/CotizacionScheduler.java`: queda
  intocable; la integración debe realizarse sin cambiar su contrato ni su
  flujo de recálculo local.
- `src/main/java/com/formula11/service/NoOpActualizacionMetricasAdapter.java`:
  conservarlo y quitar únicamente `@Component`, porque
  `CotizacionSchedulerTest` lo instancia directamente.
- `src/main/java/com/formula11/dto/SincronizacionResponse.java`: no modificar;
  pertenece a la sincronización de Football-Data.
- `src/main/java/com/formula11/exception/GlobalExceptionHandler.java`:
  handlers de excepciones nuevas si no encajan en los existentes.
- `src/main/resources/application.yml`: defaults de `app.whoscored`.
- `src/test/resources/application-test.yml`: modo `archivos`, carpeta fixture
  y valores deterministas.
- Las clases existentes de 004, 006, 007 y 009 solo pueden recibir las
  ampliaciones compatibles indicadas arriba. Sus DTOs, tests, firmas, métodos,
  campos y comportamiento existente quedan protegidos.

### Files not to modify

- `src/main/resources/db/migration/V1__*.sql` a `V3__*.sql`.
- `src/main/resources/db/migration/V10__*.sql` a `V12__*.sql`.
- `src/main/java/com/formula11/service/ActualizacionMetricasPort.java`.
- `src/main/java/com/formula11/dto/SincronizacionResponse.java`.
- Interfaces, DTOs y tests existentes de las features 004, 006, 007 y 009.
- Contrato `specs/005-contratos-entrega2/contracts/openapi.yaml`: la feature
  lo implementa, no lo cambia.

## Implementation Phases

### Phase 1 - Dependency, configuration and schema

1. Agregar Playwright al `pom.xml`.
2. Crear propiedades y defaults de producción/test.
3. Crear migraciones V13–V17 y entidades/repositories.
4. Verificar `mvn test-compile` y validación Flyway con Testcontainers.

### Phase 2 - Pure parser and linking

1. Definir records de entrada/salida sin Spring.
2. Implementar normalización, parseo de minutos/stats/rating/eventos.
3. Implementar goles recibidos y valla invicta.
4. Implementar vinculación y pendientes.
5. Ejecutar unitarios offline contra el fixture y agregar casos borde.

### Phase 3 - Source adapters

1. Implementar adapter de archivos y extracción HTML.
2. Implementar adapter Playwright con contexto único por carga.
3. Implementar calendario mensual por `page.evaluate` + `fetch`.
4. Agregar reintentos, `timeoutMs`, `pausaMs` y cierre seguro.
5. Documentar instalación de browsers, configuración y verificación manual en
   `quickstart.md`; no automatizar estas pruebas de red.

### Phase 4 - Orchestration and scheduler

1. Implementar carga por partido y transacción aislada.
2. Implementar ejecución persistida, lock, recuperación de `EN_CURSO` y MDC.
3. Registrar la implementación real del port sin modificar
   `ActualizacionMetricasPort`; conservar el NoOp solo para compatibilidad de
   instanciación en tests existentes, no como bean activo.
4. Conectar scheduler sin mover lógica de negocio al scheduler.
5. Verificar idempotencia, reanudación, parcialidad y fallos externos.

### Phase 5 - Contract endpoints and tests

1. Crear DTOs records y controller documentado.
2. Implementar `202`, `400`, `401`, `403`, `404` y `409`.
3. Agregar unitarios de parser/calendario/normalización.
4. Agregar integración offline con Testcontainers y limpieza selectiva.
5. Agregar controller/E2E con `@WithMockUser` para roles y estados.
6. Ejecutar build, tests focalizados y luego la suite completa.

## Testing Strategy

### Unit tests without Spring or network

- `MatchCentreParserTest` en `src/test/java/com/formula11/unit/`: fixture
  `whoscored/LA_LIGA/partido-1994169.json`; verificar
  Alfonso Herrero, Carlos Puga, Ángel Fortuño, goles, asistencias y nueve
  amarillas.
- `NormalizadorNombreTest`: tildes, mayúsculas, espacios y nombres vacíos.
- `JugadorVinculacionServiceTest`: único, homónimo, equipo contenido y sin
  candidato.
- `CalendarioWhoScoredParserTest`: un partido `FT`, uno no terminado y una
  respuesta con varios meses/equipos.
- `WhoScoredArchivoAdapterTest` en `src/test/java/com/formula11/unit/`: JSON
  fixture y extracción desde HTML.
- `WhoScoredPropertiesTest`: defaults, modo inválido, stage IDs y zona.

No usar `WhoScoredPlaywrightAdapter` en tests automáticos. La verificación de
Chrome, cookies, captcha, `navigator.webdriver`, perfil persistente y red se
documenta y ejecuta manualmente según `quickstart.md`.

### Integration tests with Testcontainers

- Activar `application-test.yml` en modo archivos.
- Crear explícitamente jugadores del Málaga y Espanyol necesarios para el
  fixture, con liga/equipo compatibles.
- Ejecutar la carga y comprobar filas de `metrica_partido`, relación externa,
  `whoscored_id`, pendientes y estado de ejecución.
- Repetir la carga y comprobar cero duplicados y no volver a leer el partido
  ya cargado.
- Simular error después de un partido y comprobar conservación de commits.
- Limpiar primero `metrica_partido`, `partido_externo` y pendientes que
  referencien los jugadores creados; luego borrar solo esos jugadores por ids.
  Nunca usar `deleteAll` sobre `jugadores`.

### Controller/E2E tests

En `src/test/java/com/formula11/integration/`, con MockMvc y usuarios de prueba:

- `SUPERUSUARIO`: `POST` devuelve `202`, consulta devuelve `200`, pendientes
  devuelve `200`.
- `USUARIO`: las tres operaciones devuelven `403`.
- sin autenticación: las tres devuelven `401`.
- ejecución inexistente: `404`.
- carga ya ocupada: `409`.
- request inválido: `400`.
- verificar esquema, `correlationId`, paginación y estados contractuales.

### Validation commands

Desde `formula11/backend`:

```text
mvn test-compile
mvn -Dtest=MatchCentreParserTest,JugadorVinculacionServiceTest test
mvn -Dtest=MetricasControllerTest,CargaMetricasIntegrationTest test
mvn test
```

Si la suite completa requiere Docker por Testcontainers, reportar el bloqueo
explícitamente y conservar la validación offline.

## Research and Quickstart Deliverables

`research.md` debe documentar, después de la primera carga real controlada:

- total de jugadores detectados;
- cantidad y porcentaje vinculados;
- pendientes por causa (sin candidato, homónimo, equipo ambiguo);
- partidos por liga, cargados y omitidos;
- tiempos y errores de WhoScored;
- configuración usada, sin cookies ni credenciales.

`quickstart.md` debe explicar:

- instalación de browsers de Playwright;
- `app.whoscored.modo=automatico`, perfil persistente y headless;
- stage IDs y zona horaria;
- ejecución manual como superusuario;
- lectura del estado y pendientes;
- modo archivos para desarrollo/tests;
- advertencia de que el adapter Playwright usa red y no forma parte de los
  tests automáticos.

## Risks and Mitigations

- **Formato cambiante de WhoScored**: validar claves requeridas, aislar el
  partido fallido y conservar commits anteriores.
- **Captcha/bloqueo**: reintentos configurables, pausa entre partidos y modo
  archivos reproducible.
- **Diferencias de nombres/equipos**: normalización documentada, contención de
  equipos y pendientes ante ambigüedad.
- **Migraciones fuera de orden**: SQL nullable/default, V13–V17 sin tocar
  migraciones aplicadas y prueba con el perfil actual de Flyway.
- **Carga concurrente/reinicio**: lock de un solo proceso, estado persistido y
  transición de `EN_CURSO` a `FALLIDA` al arrancar.
- **Compatibilidad con feature 006**: mantener la unique `(jugador_id,
  fecha_partido)`, conservar sin cambios `ActualizacionMetricasPort` y
  `CotizacionScheduler`, no modificar sus DTOs/tests y correr primero sus tests
  existentes.
