# Tasks: Scraper de métricas por partido de WhoScored

**Feature**: `011-scraper-whoscored`  
**Spec**: [spec.md](spec.md)  
**Plan**: [plan.md](plan.md)

## Conventions and hard constraints

- Ejecutar los comandos desde `formula11/backend`.
- No modificar V1–V3 ni V10–V12.
- Crear únicamente migraciones nuevas V13–V17; V18 y V19 quedan libres.
- En las clases existentes de las features 004, 006, 007 y 009 se permite
  agregar únicamente métodos de consulta nuevos en `JugadorRepository` y
  `MetricaPartidoRepository`, el campo nullable `whoscoredId` en `Jugador`, la
  relación nullable en `MetricaPartido` y quitar `@Component` a
  `NoOpActualizacionMetricasAdapter`. Está prohibido cambiar o borrar firmas,
  métodos, campos o comportamiento existentes que usen esas features.
- No modificar DTOs ni tests existentes de las features 004, 006, 007 y 009.
- `ActualizacionMetricasPort` permanece exactamente como `void actualizar()`.
- `SincronizacionResponse` permanece sin cambios; los endpoints de métricas usan
  DTOs nuevos.
- Los tests automáticos no acceden a red ni abren navegador.
- No usar `deleteAll` sobre `jugadores` en tests de integración.
- La lista de archivos permitidos queda documentada en T002 y en este archivo;
  no crear un archivo separado para esa lista.

## Dependency graph

```text
T001 -> T002 -> T003 -> T004
T003 -> T005 -> T006
T004 -> T007 -> T008 -> T009
T006 -> T010 -> T011 -> T012
T009 -> T013 -> T014 -> T015
T012 + T015 -> T016 -> T017 -> T018
T011 + T015 -> T019 -> T020 -> T021
T018 + T021 -> T022 -> T023
T004 -> T024
T009 + T012 + T019 + T020 + T023 + T024 -> T025
T015 + T017 + T020 + T023 + T025 -> T026
T020 + T026 -> T028
```

La orquestación depende del puerto, del adapter de archivos y de los parsers
offline, pero no depende de T021 ni T022: Playwright se verifica manualmente.
Las tareas de documentación y verificación final pueden ejecutarse en paralelo
cuando sus dependencias estén satisfechas.

## Phase 0 - Baseline and compatibility guardrails

- [ ] **T001 - Registrar baseline de compilación y tests**
  - Ejecutar `mvn test-compile` y los tests focalizados existentes de las
    features 004, 006, 007 y 009.
  - Registrar cualquier fallo preexistente sin modificar esos tests.
  - Confirmar que `ActualizacionMetricasPort` solo expone `void actualizar()`
    y que `SincronizacionResponse` pertenece a Football-Data.
  - **Depends on**: ninguna.

- [ ] **T002 - Fijar el límite de archivos de la feature**
  - Documentar en esta tarea la lista de archivos nuevos/modificados
    permitidos según el plan; no crear un archivo adicional.
  - Archivos nuevos permitidos:
    `adapter/WhoScoredPort.java`,
    `adapter/WhoScoredPlaywrightAdapter.java`,
    `adapter/WhoScoredArchivoAdapter.java`,
    `adapter/WhoScoredException.java`,
    `adapter/CalendarioWhoScoredParser.java`,
    `config/WhoScoredProperties.java`, `config/AsyncConfig.java`,
    `controller/MetricasController.java`, los DTOs nuevos de métricas,
    las entidades y repositories nuevos de 011, parser/normalizador puros,
    services de carga/vinculación, migraciones V13–V17 y tests nuevos.
  - Archivos existentes permitidos, solo para ampliaciones compatibles:
    `model/Jugador.java`, `model/MetricaPartido.java`,
    `persistence/JugadorRepository.java`,
    `persistence/MetricaPartidoRepository.java`,
    `service/NoOpActualizacionMetricasAdapter.java`,
    `exception/GlobalExceptionHandler.java`,
    `resources/application.yml` y `test/resources/application-test.yml`.
  - En esas clases existentes solo se permiten métodos de consulta nuevos,
    `whoscoredId` nullable, la relación nullable con
    `PartidoExterno`, quitar `@Component` del NoOp y configuración de la
    feature. Está prohibido cambiar o borrar firmas, métodos, campos o
    comportamiento existente.
  - Archivos intocables:
    `service/ActualizacionMetricasPort.java`,
    `dto/SincronizacionResponse.java`, `service/CotizacionScheduler.java`,
    todas las interfaces/DTOs/tests existentes de 004, 006, 007 y 009, y
    migraciones V1–V3 y V10–V12.
  - **Depends on**: T001.

## Phase 1 - Dependency, configuration and persistence

- [ ] **T003 - Agregar dependencia de Playwright**
  - Agregar únicamente `com.microsoft.playwright:playwright` al `pom.xml`
    con una propiedad de versión.
  - No agregar otro cliente HTTP ni parser JSON.
  - Verificar que `mvn test-compile` siga compilando sin ejecutar la prueba
    descartable de red.
  - **Depends on**: T002.

- [ ] **T004 - Crear configuración de WhoScored**
  - Crear `WhoScoredProperties` con `@ConfigurationProperties("app.whoscored")`.
  - Implementar `modo`, mapa de ligas/stage IDs, `inicioTemporada`,
    `zonaFuente`, `perfilDir`, `headless`, `pausa`, `timeout`, `reintentos` y
    `carpetaPaginas`.
  - Validar `automatico`/`archivos`, zona horaria, duraciones positivas y
    stage IDs completos.
  - Agregar defaults en `application.yml` sin credenciales ni rutas
    dependientes de una máquina.
  - **Depends on**: T003.

- [ ] **T005 - Configurar modo de tests offline**
  - Actualizar `application-test.yml` con `app.whoscored.modo=archivos` y la
    carpeta del fixture.
  - Asegurar que el contexto de test no cree el adapter de Playwright.
  - **Depends on**: T004.

- [ ] **T006 - Crear migración de partidos externos**
  - Crear `V13__crear_partidos_externos.sql` con fuente, id externo, liga,
    fecha, equipos, fecha de carga, estado/error y unique
    `(fuente, id_externo)`.
  - Usar tipos compatibles con PostgreSQL y columnas opcionales nullable/default.
  - **Depends on**: T003.

- [ ] **T007 - Crear migración de identificador WhoScored**
  - Crear `V14__agregar_whoscored_id_jugadores.sql`.
  - Agregar `jugadores.whoscored_id` nullable y unique parcial para valores no
    nulos.
  - No modificar V3 ni reutilizar `external_id`.
  - **Depends on**: T006.

- [ ] **T008 - Crear migración de relación de métricas**
  - Crear `V15__relacionar_metricas_con_partido_externo.sql`.
  - Agregar a `metrica_partido` una referencia nullable a
    `partido_externo`, más índices/identificador externo opcional.
  - Conservar la unique existente de V10 `(jugador_id, fecha_partido)`.
  - **Depends on**: T007.

- [ ] **T009 - Crear migraciones de ejecuciones y pendientes**
  - Crear V16 para `ejecucion_metricas` y su resumen por liga en la misma
    migración, usando una tabla hija si se necesita consultar cada liga.
  - Crear V17 para `pendiente_vinculacion`.
  - No crear migraciones de relleno: V18 y V19 quedan libres.
  - Incluir estado, fechas, error y `correlation_id`; evitar JSON opaco cuando
    una tabla hija permita consultar el resumen por liga.
  - Verificar que todas las columnas nuevas en tablas existentes sean nullable
    o tengan default.
  - **Depends on**: T008.

- [ ] **T010 - Crear entidades y secuencias JPA**
  - Crear entidades para partido externo, ejecución, resumen por liga y
    pendiente.
  - Usar `GenerationType.SEQUENCE` con `allocationSize=50` en entidades con
    muchas filas.
  - Agregar a `Jugador` el campo nullable `whoscoredId`.
  - Agregar a `MetricaPartido` la relación nullable con partido externo si
    corresponde al esquema elegido.
  - No cambiar constructores ni comportamiento requerido por features previas.
  - **Depends on**: T009.

- [ ] **T011 - Crear repositories**
  - Crear repositories para partidos externos, ejecuciones, resúmenes y
    pendientes.
  - Agregar consultas de existencia por `(fuente, idExterno)`, búsqueda por
    `whoscoredId`, candidatos por liga/nombre y paginación de pendientes.
  - Extender repositories existentes solo en archivos propios de 011 y sin
    cambiar sus contratos usados por 004/006/007/009.
  - **Depends on**: T010.

- [ ] **T012 - Validar esquema y compatibilidad**
  - Levantar el contexto con Testcontainers y Flyway `out-of-order`.
  - Verificar migraciones V13–V17 sobre el esquema que ya contiene V20+.
  - Ejecutar los tests baseline de T001.
  - **Depends on**: T011.

## Phase 2 - Pure parser and player linking

- [ ] **T013 - Definir records puros de WhoScored**
  - Crear records de resumen de partido, datos crudos y salida por jugador.
  - No exponer tipos de Playwright, Spring, JPA ni HTTP.
  - Representar equipo, marcador, `expandedMaxMinute`, eventos y métricas
    suficientes para calcular goles recibidos y valla invicta.
  - **Depends on**: T003.

- [ ] **T014 - Implementar normalización de nombres**
  - Crear una utilidad pura que elimine diacríticos, use `Locale.ROOT`,
    convierta a minúsculas, recorte y colapse espacios.
  - Cubrir nombres con tildes, mayúsculas, espacios repetidos y entradas
    inválidas.
  - **Depends on**: T013.

- [ ] **T015 - Implementar `MatchCentreParser`**
  - Parsear ambos JSON con Jackson sin dependencia de Spring.
  - Omitir suplentes con `stats` vacío.
  - Calcular minutos desde 0 o `subbedInExpandedMinute` hasta salida o
    `expandedMaxMinute`.
  - Sumar `shotsTotal`, `passesKey`, `dribblesWon`, `tackleSuccessful`,
    `interceptions` y `totalSaves` por minuto.
  - Tomar el rating del minuto más alto.
  - Procesar goles, asistencias y tarjetas mediante `events`,
    `satisfiedEventsTypes` y `matchCentreEventTypeJson`.
  - Calcular goles recibidos y valla invicta según las reglas de la spec.
  - **Depends on**: T013, T014.

- [ ] **T016 - Probar el parser con el fixture**
  - Crear `MatchCentreParserTest` usando
    `src/test/resources/whoscored/LA_LIGA/partido-1994169.json`.
  - Ubicar el test en `src/test/java/com/formula11/unit/`.
  - Verificar Alfonso Herrero, Carlos Puga y Ángel Fortuño.
  - Verificar 2 goles, 2 asistencias y 9 amarillas.
  - Agregar casos de suplente sin estadísticas, rating final y arqueros.
  - **Depends on**: T015.

- [ ] **T017 - Implementar vinculación al catálogo**
  - Crear `JugadorVinculacionService` con búsqueda por liga y nombre
    normalizado.
  - Reutilizar `whoscoredId` cuando ya existe.
  - Resolver diferencias de equipos mediante contención de nombres
    normalizados, como `Arsenal`/`Arsenal FC`.
  - Dejar pendiente cualquier caso sin candidato o ambiguo; no desempatar por
    posición.
  - Persistir `whoscoredId` solo en coincidencias inequívocas.
  - **Depends on**: T010, T014.

- [ ] **T018 - Probar normalización y vinculación**
  - Crear tests de candidato único, equipo contenido, homónimo ambiguo y
    ausencia de candidato.
  - Verificar que una vinculación ambigua no modifica jugadores ni asigna un
    identificador incorrecto.
  - **Depends on**: T017.

## Phase 3 - WhoScored adapters

- [ ] **T019 - Crear `WhoScoredPort`**
  - Definir exactamente dos operaciones:
    listar partidos de una liga/mes y obtener datos crudos de un partido.
  - Usar records propios del dominio/adaptador.
  - Mantener separado el listado de la descarga de datos del partido.
  - **Depends on**: T004.

- [ ] **T020 - Implementar `WhoScoredArchivoAdapter`**
  - Activar con `@ConditionalOnProperty` cuando `modo=archivos`.
  - Leer desde una subcarpeta cuyo nombre sea exactamente el enum `Liga`
    (por ejemplo, `LA_LIGA`).
  - Leer JSON con `matchCentreData` y `matchCentreEventTypeJson`.
  - Tomar la liga de la subcarpeta y la fecha del `startTime` de
    `matchCentreData`, no del nombre del archivo.
  - Leer HTML y extraer ambos objetos con manejo explícito de errores.
  - No crear navegador ni acceder a red.
  - Crear test del fixture JSON y de extracción HTML.
  - **Depends on**: T019, T015.

- [ ] **T021 - Implementar `WhoScoredPlaywrightAdapter`**
  - Activar solo con `modo=automatico`.
  - Crear un único contexto persistente por carga con Chrome real,
    `--disable-blink-features=AutomationControlled`, sin
    `--enable-automation` y `navigator.webdriver` indefinido.
  - Navegar con `COMMIT`, timeout configurable y reintentos ante navegación.
  - Leer `matchCentreData` y `matchCentreEventTypeJson` mediante
    `page.evaluate`.
  - Reutilizar navegador para todos los partidos de la carga y cerrar recursos
    en `finally`.
  - Implementar pausa configurable y errores explícitos por partido.
  - **Depends on**: T019, T004.

- [ ] **T022 - Implementar listado mensual de calendario**
  - Desde una página WhoScored, ejecutar `fetch` vía `page.evaluate` contra
    `/tournaments/{stageId}/data/?d={yyyyMM}&isAggregate=false`.
  - Delegar el parseo de `tournaments[].matches[]` a una clase pura separada,
    sin Playwright ni Spring.
  - Mantener el adapter responsable de la navegación/fetch y de entregar el
    JSON a esa clase pura.
  - Parsear `id`, `startTime`, equipos y `elapsed`.
  - Mantener partidos `FT` y permitir contabilizar no terminados como omitidos.
  - Convertir `startTime` desde `zonaFuente` a la zona de negocio.
  - **Depends on**: T021.

- [ ] **T023 - Probar parsers de adapters offline**
  - Crear el test de la clase pura de calendario en
    `src/test/java/com/formula11/unit/`, con un JSON pequeño: un partido `FT`
    y otro no terminado.
  - Crear también el test de extracción de ambos objetos desde HTML en
    `src/test/java/com/formula11/unit/`.
  - Cubrir extracción de ambos objetos desde HTML.
  - No instanciar `WhoScoredPlaywrightAdapter` en tests automáticos.
  - **Depends on**: T020, T022.

## Phase 4 - Orchestration, transactions and execution state

- [ ] **T024 - Crear configuración async y MDC**
  - Crear executor de un solo hilo con `@EnableAsync`.
  - Crear `TaskDecorator` que copie/restaure el MDC y `correlationId`.
  - Definir límites de cola y nombres de hilos configurables o documentados.
  - **Depends on**: T004.

- [ ] **T025 - Crear `CargaMetricasService`**
  - Implementar lock en memoria para una sola carga simultánea.
  - Calcular rango incremental mes actual/anterior o rango histórico desde
    `inicioTemporada`, con `desde` inclusiva.
  - Recorrer las cinco ligas y sus stage IDs.
  - Persistir ejecución, estado, fechas, correlationId y resumen por liga.
  - Marcar ejecuciones `EN_CURSO` como `FALLIDA` en startup.
  - Lanzar la carga manual con `@Async`; mantener estados y resumen aquí,
    no en `ActualizacionMetricasPort`.
  - **Depends on**: T009, T012, T019, T020, T023, T024.

- [ ] **T026 - Crear transacción aislada por partido**
  - Implementar `CargaPartidoService` en componente separado.
  - Descargar los datos crudos, parsear y resolver las vinculaciones fuera de
    la transacción.
  - Delegar a un método/componente transaccional únicamente la verificación de
    idempotencia y el guardado de métricas, pendientes y `partido_externo`.
  - Confirmar `partido_externo` y actualizar contadores dentro de esa
    transacción de persistencia.
  - Aislar el error de un partido sin borrar partidos ya confirmados.
  - **Depends on**: T015, T017, T020, T023, T025.

- [ ] **T027 - Implementar adapter real de `ActualizacionMetricasPort`**
  - Crear la implementación con el método único `void actualizar()`.
  - Delegar solo en la variante incremental sincrónica de
    `CargaMetricasService`.
  - Si hay lock ocupado, registrar el conflicto y retornar sin iniciar otra
    carga.
  - Si falla WhoScored, permitir que el scheduler continúe con métricas
    persistidas según su comportamiento existente.
  - No modificar `ActualizacionMetricasPort`, `CotizacionScheduler` ni tests de
    006.
  - No borrar `NoOpActualizacionMetricasAdapter`: `CotizacionSchedulerTest`
    de la feature 006 lo instancia directamente.
  - Quitar únicamente su anotación `@Component` para que no compita como bean
    con la implementación real.
  - **Depends on**: T025, T026.

- [ ] **T028 - Probar idempotencia y reanudación**
  - Crear el test en `src/test/java/com/formula11/integration/`, usando el
    adapter de archivos y jugadores propios de Málaga y Espanyol.
  - Ejecutar dos veces y verificar que no haya solicitudes/filas duplicadas.
  - Simular error posterior y verificar conservación de commits previos.
  - Limpiar referencias primero y borrar solo los jugadores creados por ID.
  - **Depends on**: T020, T026.

## Phase 5 - Contract endpoints

- [ ] **T029 - Crear DTOs nuevos de métricas**
  - Crear records `SincronizacionMetricasRequest`,
    `EjecucionMetricasResponse`, resumen por liga,
    `PendienteVinculacion` y `PendientesPageResponse`.
  - Respetar nombres, campos, enums, nullable y paginación del OpenAPI.
  - No modificar `SincronizacionResponse`.
  - **Depends on**: T009, T025.

- [ ] **T030 - Implementar `MetricasController`**
  - Crear `POST /api/metricas/sincronizacion` con respuesta `202`.
  - Crear `GET /api/metricas/sincronizacion/{ejecucionId}`.
  - Crear `GET /api/metricas/pendientes` con `page` y `size`.
  - Documentar con `@Operation` y `@ApiResponse`.
  - Proteger los tres endpoints con `@PreAuthorize` para
    `SUPERUSUARIO`.
  - **Depends on**: T029.

- [ ] **T031 - Implementar errores contractuales**
  - Validar `desde` y tamaños de página como `400`.
  - Mapear lock ocupado a `409`.
  - Mapear ejecución inexistente a `404`.
  - Confirmar `401`/`403` mediante SecurityConfig y
    `GlobalExceptionHandler`.
  - Mantener `ErrorResponse` y correlationId.
  - **Depends on**: T030.

- [ ] **T032 - Probar endpoints y autorización**
  - Crear tests MockMvc en `src/test/java/com/formula11/integration/` para
    `202`, `200`, `400`, `401`, `403`, `404` y `409`.
  - Cubrir `SUPERUSUARIO`, `USUARIO` y solicitud sin autenticación.
  - Verificar campos, estados, resumen por liga, pendientes y correlationId.
  - No modificar tests existentes de 004, 006, 007 ni 009.
  - **Depends on**: T031.

## Phase 6 - Documentation, manual verification and final validation

- [ ] **T033 - Completar documentación operativa**
  - Mantener `research.md` con hallazgos verificados y sin inventar
    mediciones.
  - Mantener `quickstart.md` con configuración automática, Chrome, login,
    disparo, ventana, estado y pendientes.
  - Documentar que el porcentaje de vinculados se mide en la primera carga
    real.
  - **Depends on**: T021, T030.

- [ ] **T034 - Preparar medición de la primera carga real**
  - Definir en logs/resumen los totales necesarios: jugadores detectados,
    vinculados, pendientes por causa, partidos por liga, omitidos, tiempos y
    errores.
  - No reportar porcentajes antes de ejecutar una carga real controlada.
  - **Depends on**: T025, T032, T033.

- [ ] **T035 - Verificar instalación automática manualmente**
  - Seguir [quickstart.md](quickstart.md) con `modo=automatico`.
  - Confirmar Chrome real, perfil persistente, lectura JavaScript y cierre del
    navegador.
  - Registrar solo configuración no sensible, id de ejecución, estados y
    resultados.
  - **Depends on**: T032, T033.

- [ ] **T036 - Ejecutar validación focalizada**
  - Ejecutar:
    `mvn test-compile`
  - Ejecutar unitarios de parser, normalización, vinculación y adapters de
    archivos.
  - Ejecutar integración de carga en modo archivos y tests de controller.
  - **Depends on**: T016, T018, T023, T028, T032.

- [ ] **T037 - Ejecutar regresión completa y revisar alcance**
  - Ejecutar `mvn test`.
  - Confirmar que no se modificaron interfaces, DTOs ni tests de 004, 006,
    007 y 009.
  - Confirmar que no se modificaron V1–V3 ni V10–V12.
  - Revisar `git diff --check` y el estado del repositorio.
  - **Depends on**: T036.

## Definition of Done

- [ ] Parser probado offline con el fixture y valores esperados.
- [ ] Adapters de archivos y Playwright separados detrás de `WhoScoredPort`.
- [ ] Carga idempotente por partido y transacción aislada.
- [ ] Carga manual asincrónica con estado, resumen por liga y correlationId.
- [ ] `ActualizacionMetricasPort` sigue siendo exactamente `void actualizar()`.
- [ ] `SincronizacionResponse` y las superficies de 004, 006, 007 y 009 no
      fueron modificadas.
- [ ] Endpoints alineados con OpenAPI y protegidos para `SUPERUSUARIO`.
- [ ] Migraciones V13–V17 compatibles y anteriores intactas; V18 y V19 libres.
- [ ] Tests offline, integración y controller pasan.
- [ ] Verificación automática manual documentada sin secretos.
