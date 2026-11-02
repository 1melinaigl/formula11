# Feature Specification: Scraper de métricas por partido de WhoScored

**Feature Branch**: `011-scraper-whoscored`

**Created**: 2026-10-27

**Status**: Draft

**Input**: Scraper de jugadores de WhoScored para cargar métricas por partido en
`metrica_partido`, usando el catálogo existente de Football-Data.

**Related contract**: [Contrato de la Entrega 2](../005-contratos-entrega2/contracts/openapi.yaml).
Esta feature implementa únicamente las operaciones con
`x-feature: 011-scraper-whoscored`.

## Scope

La feature descarga y parsea el rendimiento de todos los jugadores de los dos
equipos de cada partido jugado de Premier League, Bundesliga, La Liga, Serie A
y Ligue 1. Persiste una fila por jugador y partido en `metrica_partido`,
vinculada al catálogo de jugadores existente.

La carga se realiza por partido, no por jugador. El adapter de WhoScored queda
detrás de `WhoScoredPort`, con la descarga separada del parser: el parser recibe
los JSON de `matchCentreData` y `matchCentreEventTypeJson` y no conoce
Playwright ni la red. La carga es idempotente y puede reanudarse después de un
error.

No implementa:

- El catálogo de jugadores ni sus métricas de Football-Data.
- El cálculo de score, estrategias, cotizaciones o historial de cotizaciones,
  que pertenecen a la feature `006-estrategias-cotizacion`.
- La interfaz de usuario.
- Scraping iniciado por una solicitud de un usuario del mercado.

## Clarifications

Las siguientes decisiones cierran ambigüedades detectadas durante la
clarificación de la especificación:

- El listado de partidos usa el endpoint de calendario de WhoScored
  `https://www.whoscored.com/tournaments/{stageId}/data/?d={yyyyMM}&isAggregate=false`.
  La respuesta contiene `tournaments[].matches[]`; solo se procesan partidos con
  `elapsed` igual a `FT`.
- Los `stageId` de la temporada 2026/27 son configurables en
  `app.whoscored.ligas`, con estos valores por defecto: Premier League `25544`,
  La Liga `25662`, Bundesliga `25666`, Serie A `25518` y Ligue 1 `25554`.
- La carga incremental consulta el mes actual y el anterior. La carga histórica
  consulta todos los meses desde `app.whoscored.inicio-temporada`, cuyo valor
  por defecto es `2026-08-01`.
- El listado se obtiene desde la página de Playwright mediante `page.evaluate`
  y `fetch`, para reutilizar las cookies del navegador. La descarga del listado
  y la descarga de los datos de cada partido son dos métodos del mismo
  `WhoScoredPort`.
- En modo archivos, `app.whoscored.carpeta-paginas` contiene una subcarpeta por
  liga nombrada exactamente como el enum `Liga` (por ejemplo, `LA_LIGA`). El
  adapter toma la liga de esa subcarpeta y la fecha del `startTime` de
  `matchCentreData`.
- `desde` es inclusiva. El `startTime` sin zona de WhoScored se interpreta en
  `app.whoscored.zona-fuente` (por defecto `Europe/London`) y luego se convierte
  a `ClockConfig.ZONA_NEGOCIO` para filtrar y persistir la fecha del partido.
  El `Clock` de la aplicación continúa siendo UTC.
- Las migraciones nuevas de esta feature se limitan a V13–V17; V18 y V19 quedan
  libres. Nunca se editan
  V1–V3 ni V10–V12. Las columnas nuevas en tablas existentes son nullable o
  tienen valor por defecto para conservar compatibilidad con los datos ya
  aplicados y con las features 006, 007 y 009.
- Un nombre normalizado ambiguo para el mismo equipo nunca se resuelve por
  posición ni por otra heurística: se registra como pendiente.
- Solo puede existir una carga a la vez. Una segunda solicitud manual responde
  `409`, aunque use otro rango. El job semanal tampoco inicia otra carga:
  registra el conflicto y recalcula con las métricas persistidas. Al reiniciar
  la aplicación, toda ejecución `EN_CURSO` pasa a `FALLIDA`.

## User Scenarios & Testing

### User Story 1 - Descargar y parsear partidos (Priority: P1)

Como sistema quiero obtener cada partido jugado de las cinco ligas y cargar las
métricas de todos sus jugadores para que el mercado pueda calcular
cotizaciones a partir del rendimiento real.

**Why this priority**: Es la capacidad central de la feature y la fuente de los
datos que consume la feature 006.

**Independent Test**: Ejecutar el parser sin red ni navegador con
`src/test/resources/whoscored/LA_LIGA/partido-1994169.json`, que contiene ambos
objetos JSON, y verificar las métricas esperadas del partido Málaga 1-1
Espanyol.

**Acceptance Scenarios**:

1. **Given** un partido jugado de una de las cinco ligas, **When** el adapter
   abre `/Matches/{id}/Live` con Playwright para Java, **Then** lee mediante
   JavaScript `require.config.params.args.matchCentreData` y
   `require.config.params.args.matchCentreEventTypeJson`.
2. **Given** los dos objetos JSON de un partido, **When** el parser los procesa,
   **Then** obtiene jugadores de ambos equipos y sus minutos, titularidad,
   goles, asistencias, tiros, pases clave, regates, entradas,
   intercepciones, amarillas, rojas, atajadas, goles recibidos, valla invicta
   y rating final.
3. **Given** un partido ya persistido, **When** una carga posterior lo
   encuentra, **Then** no vuelve a solicitarlo ni duplica las filas de sus
   jugadores.
4. **Given** un suplente con `stats` vacío, **When** se parsea el partido,
   **Then** se considera que no jugó y no genera una fila.
5. **Given** un error al procesar un partido, **When** continúa la carga,
   **Then** el partido queda omitido con su error registrado y los demás
   partidos siguen procesándose.

### User Story 2 - Obtener la lista de partidos (Priority: P1)

Como sistema quiero obtener de WhoScored los partidos jugados de cada liga en
un rango de fechas para cargar únicamente encuentros relevantes.

**Why this priority**: El scraper necesita conocer el universo de partidos antes
de pedir sus páginas individuales.

**Independent Test**: Probar el adapter de listado con una respuesta
determinista, filtrando partidos postergados, sin estadísticas o fuera del
rango, sin acceder a Internet.

**Acceptance Scenarios**:

1. **Given** una liga y un rango de fechas, **When** se solicita el listado,
   **Then** devuelve únicamente partidos jugados dentro del rango, con
   identificador, liga, fecha y equipos.
   El adapter consulta el calendario mensual de WhoScored usando el `stageId`
   configurado y conserva únicamente partidos con `elapsed = FT`.
2. **Given** un partido postergado o sin estadísticas, **When** se arma la
   lista procesable, **Then** se omite sin detener las otras ligas.
3. **Given** una sincronización sin fecha `desde`, **When** se ejecuta,
   **Then** se solicitan partidos nuevos desde la última carga exitosa.
4. **Given** una sincronización con `desde`, **When** se ejecuta, **Then** se
   consideran todos los partidos desde esa fecha, incluyendo el backfill de la
   temporada actual.
   La fecha es inclusiva; una sincronización incremental consulta el mes actual
   y el anterior, mientras una histórica consulta todos los meses desde
   `app.whoscored.inicio-temporada`.

### User Story 3 - Vincular jugadores al catálogo (Priority: P1)

Como sistema quiero relacionar cada jugador de WhoScored con el catálogo de
Football-Data para guardar sus métricas en la base local.

**Why this priority**: Sin un `jugador_id` local las métricas no pueden ser
consumidas por la feature 006.

**Independent Test**: Probar la normalización y resolución usando nombres con
mayúsculas, tildes y espacios repetidos, incluyendo homónimos y transferencias.

**Acceptance Scenarios**:

1. **Given** un jugador de WhoScored y candidatos del mismo equipo, **When** se
   normalizan nombres, **Then** se eliminan tildes, se ignoran mayúsculas y se
   colapsan espacios antes de comparar.
2. **Given** una vinculación exitosa, **When** se persiste la métrica,
   **Then** se guarda el jugador del catálogo y el identificador de WhoScored
   para no volver a resolverlo.
3. **Given** un jugador sin coincidencia, **When** se procesa el partido,
   **Then** se registra como pendiente con su `whoscoredId`, nombre, equipo,
   liga y fecha de detección, sin cortar la carga.
4. **Given** dos jugadores con el mismo nombre en el mismo equipo o un jugador
   transferido, **When** la coincidencia no es determinista, **Then** no se
   asigna silenciosamente el jugador incorrecto y se conserva como pendiente.

### User Story 4 - Ejecutar y consultar una sincronización (Priority: P1)

Como superusuario quiero iniciar una carga manual asincrónica, consultar su
avance y revisar los jugadores pendientes.

**Why this priority**: La carga histórica puede tardar varios minutos y debe
ser observable sin bloquear una solicitud HTTP.

**Independent Test**: Con MockMvc y un adapter falso, iniciar una ejecución,
consultar sus estados y verificar autorización, conflicto concurrente,
`correlationId` y resumen por liga.

**Acceptance Scenarios**:

1. **Given** un `SUPERUSUARIO` y ninguna carga en curso, **When** ejecuta
   `POST /api/metricas/sincronizacion` sin cuerpo, **Then** recibe `202` con
   `EjecucionMetricasResponse` en estado `EN_CURSO` y se cargan los partidos
   nuevos.
2. **Given** un `SUPERUSUARIO` envía `desde`, **When** la ejecución comienza,
   **Then** carga desde esa fecha y responde `202` con el identificador de
   ejecución.
3. **Given** una ejecución existente, **When** el superusuario consulta
   `GET /api/metricas/sincronizacion/{ejecucionId}`, **Then** recibe estado,
   fechas, `correlationId` y resumen por liga con partidos cargados,
   omitidos, jugadores vinculados, pendientes y error opcional.
4. **Given** una carga en curso, **When** se solicita otra, **Then** responde
   `409` con el esquema `Error`, sin reutilizar ni fusionar la ejecución
   existente.
5. **Given** un `USUARIO`, **When** intenta iniciar, consultar o listar una
   carga, **Then** recibe `403` con `ErrorSoloSuperusuario`.
6. **Given** una solicitud sin JWT, **When** accede a cualquiera de las tres
   operaciones, **Then** recibe `401` con `ErrorNoAutorizado`.
7. **Given** un rango inválido, **When** se inicia la carga, **Then** recibe
   `400` con `ErrorValidacion`.
8. **Given** una ejecución que termina con errores parciales, **When** se
   consulta, **Then** queda en `PARCIAL` y conserva los partidos guardados
   antes de cada error.

### User Story 5 - Actualizar desde el job semanal (Priority: P1)

Como sistema quiero que el job semanal reutilice la misma operación de carga
que el disparo manual, sin acoplar el scheduler a WhoScored.

**Why this priority**: La actualización automática debe mantener el mercado al
día y seguir funcionando con las métricas ya persistidas si falla la fuente
externa.

**Independent Test**: Mockear `WhoScoredPort` y verificar que
`ActualizacionMetricasPort` sea invocado por el scheduler, que el
`correlationId` llegue al hilo asincrónico y que un error no impida el
recálculo con datos locales.

**Acceptance Scenarios**:

1. **Given** el job semanal, **When** llama a `ActualizacionMetricasPort`,
   **Then** se carga desde la última carga exitosa y se reutiliza el mismo
   service que usa la operación manual.
   Si hay una carga en curso, no inicia otra, registra el conflicto y recalcula
   con las métricas ya persistidas.
2. **Given** una falla de WhoScored, **When** corre el job, **Then** se registra
   el error con `correlationId` y el sistema conserva disponibles las métricas
   ya persistidas.
3. **Given** una carga asincrónica, **When** se ejecuta en otro hilo, **Then**
   el `correlationId` de la solicitud se propaga al hilo de carga y a sus logs.

### User Story 6 - Cargar el histórico y usar respaldo local (Priority: P2)

Como superusuario quiero cargar todos los partidos desde el inicio de la
temporada actual y poder procesar páginas guardadas cuando no hay red.

**Why this priority**: El backfill permite que la feature 006 genere el
historial de cotizaciones y el respaldo permite reproducir cargas sin depender
de WhoScored.

**Independent Test**: Ejecutar una carga histórica con un adapter local que
interrumpa después de algunos partidos y reanudarla; luego procesar HTML de una
carpeta configurable sin navegador.

**Acceptance Scenarios**:

1. **Given** el inicio de la temporada actual, **When** se pide una carga
   histórica, **Then** se recorren los partidos de las cinco ligas hasta la
   última fecha disponible.
2. **Given** una interrupción a mitad de la carga, **When** se reanuda,
   **Then** se retoma desde el último partido persistido sin duplicar métricas.
3. **Given** una carpeta de respaldo configurada, **When** falla la descarga
   automática, **Then** el mismo parser extrae de cada HTML
   `matchCentreData` y `matchCentreEventTypeJson`.

## Domain Rules

### Datos de jugadores y minutos

Cada jugador recibido puede contener `playerId`, `name`, `isFirstEleven`,
`subbedInExpandedMinute`, `subbedOutExpandedMinute` y `stats`. Los minutos se
calculan desde `0` para titulares o desde `subbedInExpandedMinute` para
suplentes, hasta `subbedOutExpandedMinute` o `expandedMaxMinute`. Los suplentes
con `stats` vacío no jugaron.

Los valores por minuto de `stats` se suman para `shotsTotal`, `passesKey`,
`dribblesWon`, `tackleSuccessful`, `interceptions` y `totalSaves`. El rating
final es el rating del minuto más alto, no el promedio.

### Eventos

- Los goles son eventos `Goal`, excepto los de tipo `goalOwn`.
- Las asistencias son eventos cuyo `satisfiedEventsTypes` incluye `assist`,
  traducido mediante `matchCentreEventTypeJson`.
- Las tarjetas son eventos `Card` de tipo `Yellow`, `Red` o `SecondYellow`.
- Los goles recibidos son goles del rival mientras el arquero estuvo en cancha.
- La valla invicta corresponde a un arquero con cero goles recibidos y al menos
  60 minutos jugados.

Para el partido de referencia Málaga 1-1 Espanyol del 9/10/2026, identificado
como `1994169`, deben verificarse al menos estos resultados:

- Alfonso Herrero (`261509`): titular, 96 minutos, rating `6,47`.
- Carlos Puga (`447938`): titular, 63 minutos, rating `6,80`.
- Ángel Fortuño (`449409`): titular, 96 minutos, rating `7,24`.
- Dos goles: Bryan Zaragoza y Adrián Niño.
- Dos asistencias: Javi Hernández y Rafita.
- Nueve amarillas.

## API Contract Surface

La implementación debe respetar literalmente el contrato de
[openapi.yaml](../005-contratos-entrega2/contracts/openapi.yaml):

### `POST /api/metricas/sincronizacion`

- Requiere JWT y rol `SUPERUSUARIO`.
- Cuerpo opcional `SincronizacionMetricasRequest` con `desde` como fecha.
- Responde `202` con `EjecucionMetricasResponse`.
- Responde `400` por validación, `401` sin autenticación, `403` sin rol y
  `409` si ya existe una carga en curso.
- Sin `desde`, carga partidos nuevos desde la última carga; con `desde`, carga
  desde esa fecha.

### `GET /api/metricas/sincronizacion/{ejecucionId}`

- Requiere JWT y rol `SUPERUSUARIO`.
- Responde `200` con estado `EN_CURSO`, `COMPLETADA`, `PARCIAL` o `FALLIDA`.
- Incluye `id`, `estado`, `inicio`, `fin` nullable, `ligas` y `correlationId`.
- Cada resumen de liga incluye `liga`, `estado`, `partidosCargados`,
  `partidosOmitidos`, `jugadoresVinculados`, `pendientes` y `error` nullable.
- Responde `401`, `403` o `404` según el contrato.

### `GET /api/metricas/pendientes`

- Requiere JWT y rol `SUPERUSUARIO`.
- Acepta los parámetros paginados `page` y `size`.
- Responde `200` con `PendientesPageResponse`, cuyo `content` contiene
  `whoscoredId`, `nombre`, `equipo`, `liga` y `detectadoEn`.
- Responde `401` o `403` según el contrato.

Todas las respuestas de error usan el esquema `Error`. El cliente puede enviar
`X-Correlation-Id`; el valor debe conservarse en la ejecución y en los logs.

## Functional Requirements

- **FR-001**: El sistema MUST implementar `WhoScoredPort` como adapter externo
  separado del parser y MUST permitir sustituir la descarga por páginas
  guardadas.
- **FR-002**: El sistema MUST listar partidos jugados de las cinco ligas en un
  rango de fechas mediante el endpoint mensual de calendario de WhoScored,
  usando `stageId` configurable, y MUST conservar únicamente partidos con
  `elapsed = FT`.
- **FR-003**: El sistema MUST abrir la página `/Matches/{id}/Live` con
  Playwright para Java y MUST leer mediante JavaScript los dos objetos
  `require.config.params.args.matchCentreData` y
  `require.config.params.args.matchCentreEventTypeJson`.
- **FR-004**: El parser MUST recibir ambos JSON como entrada independiente de
  la fuente y MUST producir las métricas de todos los jugadores que jugaron de
  ambos equipos.
- **FR-005**: El sistema MUST persistir en `metrica_partido` minutos,
  titularidad, goles, asistencias, tiros, pases clave, regates, entradas,
  intercepciones, amarillas, rojas, atajadas, goles recibidos, valla invicta,
  rating y fuente.
- **FR-006**: Los contadores por minuto MUST sumarse y el rating final MUST
  corresponder al minuto más alto.
- **FR-007**: El sistema MUST calcular minutos y eventos conforme a las reglas
  de dominio de esta especificación.
- **FR-008**: El sistema MUST mantener unicidad por jugador y partido y MUST
  evitar volver a pedir o insertar un partido ya cargado.
- **FR-009**: El sistema MUST vincular por equipo y nombre normalizado sin
  tildes, sin distinguir mayúsculas y sin espacios repetidos.
- **FR-010**: El sistema MUST guardar el identificador de WhoScored asociado al
  jugador del catálogo para no resolverlo nuevamente.
- **FR-011**: Los jugadores no vinculados MUST persistirse como pendientes y
  MUST not detener la carga de partidos restantes.
- **FR-012**: El sistema MUST implementar `ActualizacionMetricasPort` de la
  feature 006 sin modificar su método `void actualizar()`. La implementación
  real debe reemplazar al NoOp como bean productivo, pero la clase
  `NoOpActualizacionMetricasAdapter` se conserva sin `@Component` para los
  tests existentes.
- **FR-013**: El job semanal MUST llamar a `ActualizacionMetricasPort` y luego
  al mismo método de service que usa el recálculo manual; el scheduler no debe
  contener lógica de negocio.
- **FR-014**: La carga manual MUST ser asincrónica y MUST rechazar con `409` una
  segunda carga mientras la primera está en curso.
- **FR-015**: La ejecución MUST conservar estado, inicio, fin, resumen por liga,
  errores, partidos omitidos, jugadores vinculados, pendientes y
  `correlationId`.
- **FR-016**: El `correlationId` MUST propagarse al hilo que realiza la carga y
  MUST aparecer en logs estructurados de inicio, avance, error y finalización.
- **FR-017**: El sistema MUST soportar reanudación desde el último partido
  persistido y MUST conservar las filas guardadas si falla un partido posterior.
- **FR-018**: El sistema MUST permitir cargar desde el inicio de la temporada
  actual para habilitar el backfill de la feature 006.
- **FR-019**: El sistema MUST permitir un directorio configurable de HTML de
  respaldo y el parser MUST extraer ambos objetos JSON de cada página guardada.
- **FR-020**: La descarga automática MUST usar un único navegador reutilizado
  durante una carga, con perfil persistente configurable, Chrome sin
  `--enable-automation`, el argumento
  `--disable-blink-features=AutomationControlled` y
  `navigator.webdriver` indefinido.
- **FR-021**: La descarga MUST usar timeouts, reintentos de lectura JavaScript
  y una pausa configurable entre partidos.
- **FR-022**: Si WhoScored no está disponible, el sistema MUST continuar
  funcionando con las métricas ya persistidas y MUST registrar el fallo.
- **FR-023**: Todo service que use fecha u hora MUST recibir `Clock`; el reloj
  debe ser UTC. `startTime` debe interpretarse en la zona configurable
  `app.whoscored.zona-fuente` (por defecto `Europe/London`) y la fecha de
  partido debe convertirse con `ClockConfig.ZONA_NEGOCIO`. `desde` es inclusiva.
- **FR-024**: Las migraciones de esta feature MUST estar en V13 a V17; V18 y
  V19 quedan libres. V1 a V3 y V10 a V12 pertenecen a otras features y no deben modificarse. Toda columna
  nueva en una tabla existente MUST ser nullable o tener un valor por defecto.
- **FR-025**: La única dependencia nueva permitida MUST ser Playwright para
  Java.
- **FR-026**: Los tests MUST ser offline, no abrir navegador ni acceder a red,
  y MUST probar el parser usando el JSON guardado del partido de referencia.
- **FR-027**: El sistema MUST aplicar autenticación JWT y permitir las
  operaciones de esta feature únicamente a `SUPERUSUARIO`; nunca debe
  dispararse un scrape por una solicitud del mercado.
- **FR-028**: El sistema MUST respetar los esquemas, códigos de respuesta,
  paginación y reglas de rol definidos por el contrato de la Entrega 2.
- **FR-029**: El sistema MUST permitir una sola carga simultánea. Una segunda
  solicitud manual MUST responder `409`; el job semanal MUST omitir el inicio,
  registrarlo y recalcular con datos locales si ya hay una carga en curso.
- **FR-030**: Al iniciar la aplicación, toda ejecución persistida en estado
  `EN_CURSO` MUST pasar a `FALLIDA`.

## Data Model and Migrations

- **`metrica_partido`**: una fila por jugador vinculado y partido, con
  referencia a `jugadores`, fecha de partido, liga, identificador del partido,
  identificador de WhoScored, métricas calculadas y fuente. Debe existir una
  restricción única que impida duplicar jugador y partido.
- **Pendiente de vinculación**: identifica `whoscoredId`, nombre, equipo, liga y
  fecha de detección; puede conservar el último error o detalle de resolución.
- **Partido cargado**: registra el identificador del partido, liga, fecha,
  estado de carga y error opcional para habilitar idempotencia y reanudación.
- **Ejecución de sincronización**: registra estado, fechas, `correlationId` y
  resumen por liga para responder el contrato.
- Las migraciones Flyway de la feature deben cubrir V13 a V17, separando
  partidos, métricas, pendientes y ejecuciones con su resumen por liga, sin
  crear migraciones de relleno. V18 y V19 quedan libres. No alterar V1 a V3 ni V10 a V12. Toda
  columna agregada a una tabla existente debe ser nullable o tener un valor por
  defecto.

## Non-Functional Requirements

- La configuración de perfil persistente, directorio de respaldo, timeouts,
  pausa entre partidos y rangos por defecto debe ser externa y no quedar
  codificada en el adapter.
- La carga debe tener un solo navegador por ejecución, cerrar recursos incluso
  ante errores y evitar concurrencia de dos cargas.
- Los logs deben ser estructurados, estar en español cuando sean mensajes
  funcionales e incluir `correlationId`, liga, partido, estado y cantidades.
- El parser debe ser determinista con los mismos JSON y no depender de zona
  horaria local ni de la hora del sistema.
- El error de una página, un formato desconocido o un captcha debe aislarse al
  partido afectado siempre que sea posible; la ejecución debe informar si
  terminó completa, parcial o fallida.

## Edge Cases

- WhoScored bloquea la página, muestra captcha o continúa navegando después de
  cargar: reintentar la lectura JavaScript con timeout configurable y marcar el
  partido como omitido si no se recupera.
- Cambia el formato de la página o falta una clave requerida: fallar ese
  partido con error explícito y continuar con los demás.
- El partido fue postergado, no tiene estadísticas o tiene equipos incompletos:
  omitirlo sin crear métricas inválidas.
- El jugador no existe en el catálogo: registrar pendiente y continuar.
- Dos jugadores comparten nombre y equipo: no elegir arbitrariamente; dejar
  pendiente hasta una resolución inequívoca.
- Un jugador fue transferido durante la temporada: priorizar el equipo del
  partido y no reutilizar una vinculación incompatible.
- La carga falla a mitad de camino: conservar los partidos ya confirmados y
  reanudar sin duplicados.
- Se solicita otra carga mientras una está en curso: responder `409`.
- El parser recibe estadísticas vacías, minutos ausentes, eventos duplicados o
  valores nulos: aplicar las reglas explícitas, validar el formato y registrar
  un error cuando no pueda producir una métrica confiable.

## Key Entities

- **PartidoWhoscored**: identificador, liga, fecha, equipos y estado de carga de
  un partido externo.
- **MetricaPartido**: rendimiento de un jugador del catálogo en un partido,
  incluyendo las métricas deportivas y la fuente.
- **PendienteVinculacion**: jugador externo que no pudo vincularse con el
  catálogo.
- **EjecucionMetricas**: carga asincrónica, su estado, resumen, errores y
  `correlationId`.
- **WhoScoredPort**: puerto de listado y descarga de datos externos.
- **ParserWhoScored**: componente puro que transforma los dos JSON de un
  partido en métricas y eventos.
- **ActualizacionMetricasPort**: puerto existente consumido por el job de la
  feature 006.

## Success Criteria

### Measurable Outcomes

- **SC-001**: El parser procesa sin red el JSON de referencia y produce los
  valores esperados de minutos, ratings, goles, asistencias y tarjetas.
- **SC-002**: Una segunda carga del mismo partido realiza cero nuevas
  solicitudes para ese partido y deja exactamente una métrica por jugador y
  partido.
- **SC-003**: Una carga con un partido inválido continúa con los partidos
  restantes y expone el partido fallido en el resumen de ejecución.
- **SC-004**: El `POST` de sincronización responde `202` para una carga válida,
  permite consultar su estado y responde `409` ante una segunda carga
  concurrente.
- **SC-005**: Un usuario con rol `USUARIO` recibe `403` en las tres operaciones
  de métricas y un usuario no autenticado recibe `401`.
- **SC-006**: Una falla de WhoScored no elimina ni invalida las métricas locales
  previamente persistidas.
- **SC-007**: El job semanal invoca el puerto existente con el mismo flujo de
  carga manual y conserva el `correlationId` en el hilo asincrónico y los logs.
- **SC-008**: Los tests de la feature no acceden a la red ni abren navegador y
  cubren el parser con el archivo de referencia.

## Assumptions

- El catálogo de Football-Data ya existe y sus equipos, ligas y nombres son la
  fuente local para la vinculación.
- La tabla base `metrica_partido` de la feature 006 está disponible después de
  sus migraciones V10 a V12; esta feature no modifica esas migraciones.
- `ClockConfig.ZONA_NEGOCIO` es `America/Argentina/Buenos_Aires` y el `Clock`
  inyectado entrega instantes en UTC.
- WhoScored puede requerir un perfil persistente y una sesión de Chrome
  existente; la configuración de infraestructura debe permitirlo sin guardar
  credenciales en el repositorio.
- El calendario mensual de WhoScored es accesible desde la página de
  Playwright mediante `fetch` con las cookies de esa página.
- Los `stageId` y la zona horaria de la fuente son configuración, no constantes
  de dominio, para permitir nuevas temporadas y cambios de fuente.
- Los endpoints del contrato son de administración y no constituyen una API de
  scraping para usuarios del mercado.

## Out of Scope

- Cálculo de score y cotización.
- Métricas de Football-Data.
- Frontend.
