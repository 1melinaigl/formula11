# Research: hallazgos de WhoScored

**Feature**: `011-scraper-whoscored`  
**Fuente**: prueba descartable de la rama `spike-whoscored` y fixture local
`src/test/resources/whoscored/LA_LIGA/partido-1994169.json`.
**Estado**: hallazgos verificados; la sección 9 registra la primera carga
real en modo automático (2026-10-29).

## 1. Protección anti-bots y configuración del navegador

WhoScored no entregó una página utilizable a un navegador automático con la
configuración por defecto: la página quedaba en blanco o terminaba en timeout.

La prueba descartable funcionó utilizando:

- Playwright con `channel("chrome")`, es decir, Chrome instalado como navegador
  real.
- Perfil persistente.
- Modo visible (`headless(false)`).
- Exclusión del argumento default `--enable-automation`.
- Argumento `--disable-blink-features=AutomationControlled`.
- Init script que define `navigator.webdriver` como `undefined`.

Esto justifica que el adapter automático use Chrome real, perfil persistente y
una única sesión de navegador reutilizada durante una carga.

El timeout y la pausa del adapter se configuran como `timeoutMs` y `pausaMs`,
valores `double` positivos en milisegundos, para pasarlos directamente a la
API de Playwright sin conversiones implícitas.

El archivo `WhoScoredPlaywrightAdapter.java` está excluido de la métrica de
cobertura de SonarCloud porque requiere Chrome, red y una verificación manual
según [quickstart.md](quickstart.md); los demás componentes de la feature se
cubren con tests unitarios offline.

## 2. Dónde están los datos

El HTML devuelto por `page.content()` no contiene de forma confiable
`matchCentreData`: la página borra o modifica el script que lo contiene
después de cargar.

Los datos sí se pueden leer desde JavaScript con:

```javascript
require.config.params.args.matchCentreData
require.config.params.args.matchCentreEventTypeJson
```

La lectura debe serializar ambos objetos con `JSON.stringify`. Si la página
todavía está navegando, la evaluación puede fallar y debe reintentarse.

La estrategia de guardar el HTML de `page.content()` se mantiene únicamente
como evidencia diagnóstica; no es la fuente principal del parser.

## 3. Navegación posterior a la carga

La página puede seguir navegando después de que la navegación inicial haya
terminado. En ese estado, `page.content()` puede lanzar una excepción de
Playwright con un mensaje equivalente a `page is navigating`.

El adapter debe:

1. Navegar con `WaitUntilState.COMMIT`.
2. Leer los objetos mediante `page.evaluate`.
3. Reintentar mientras la página siga navegando, usando timeout y cantidad de
   reintentos configurables.
4. Aislar el error del partido si no logra obtener los objetos.

## 4. Calendario mensual

El endpoint observado para listar partidos es:

```text
/tournaments/{stageId}/data/?d={yyyyMM}&isAggregate=false
```

La consulta debe realizarse desde una página de WhoScored mediante
`page.evaluate` y `fetch`, para reutilizar las cookies del navegador.

Los `stageId` observados para la temporada 2026/27 son:

| Liga | `stageId` |
|---|---:|
| Premier League | 25544 |
| La Liga | 25662 |
| Bundesliga | 25666 |
| Serie A | 25518 |
| Ligue 1 | 25554 |

La respuesta contiene `tournaments[].matches[]`. Los partidos terminados
tienen `elapsed` igual a `FT`.

La estrategia definida para la implementación es:

- incremental: consultar el mes actual y el anterior;
- histórica: consultar todos los meses desde
  `app.whoscored.inicio-temporada`;
- mantener los `stageId` como configuración.

## 5. Fechas y zona horaria

`startTime` llega sin información de zona horaria. En la observación realizada
se comporta como hora de Londres; por ejemplo, el fixture contiene:

```text
2026-10-09T20:00:00
```

La implementación debe interpretar ese valor en
`app.whoscored.zona-fuente`, cuyo default definido para la feature es
`Europe/London`, y convertirlo a `ClockConfig.ZONA_NEGOCIO` para obtener la
fecha del partido. `desde` se interpreta de forma inclusiva. El `Clock` de la
aplicación continúa siendo UTC.

## 6. Mapeo observado en `matchCentreData`

El fixture `partido-1994169.json` tiene `matchId` `1994169`, resultado `1 : 1`,
`elapsed` `FT`, y los dos objetos requeridos:

- `matchCentreData`
- `matchCentreEventTypeJson`

Los jugadores están en `matchCentreData.home.players` y
`matchCentreData.away.players`. Cada jugador contiene, entre otros, `playerId`,
`name`, `isFirstEleven` y `stats`. Las estadísticas relevantes están
representadas como valores indexados por minuto; `ratings` también es un mapa
por minuto.

| Métrica persistida | Campo observado | Regla verificada |
|---|---|---|
| tiros | `stats.shotsTotal` | sumar los valores por minuto |
| pases clave | `stats.passesKey` | sumar los valores por minuto |
| regates | `stats.dribblesWon` | sumar los valores por minuto |
| entradas | `stats.tackleSuccessful` | sumar los valores por minuto |
| intercepciones | `stats.interceptions` | sumar los valores por minuto |
| atajadas | `stats.totalSaves` | sumar los valores por minuto |
| rating | `stats.ratings` | usar el rating del minuto más alto |
| titular | `isFirstEleven` | conservar el booleano del jugador |
| minutos | `subbedInExpandedMinute`, `subbedOutExpandedMinute`, `expandedMaxMinute` | desde 0 para titulares o desde la entrada hasta la salida/fin |

Los suplentes que tienen `stats` vacío no jugaron y no deben generar una fila.
Los contadores de goles, asistencias y tarjetas se obtienen de
`matchCentreData.events`:

- Gol: `type.displayName == "Goal"`, excluyendo el tipo/qualifier
  `goalOwn`.
- Asistencia: evento cuyo `satisfiedEventsTypes` se traduce con
  `matchCentreEventTypeJson` y contiene `assist`.
- Tarjeta: `type.displayName == "Card"` y qualifier `Yellow`, `Red` o
  `SecondYellow`.
- Goles recibidos: goles del rival mientras el arquero estuvo en cancha.
- Valla invicta: arquero con cero goles recibidos y al menos 60 minutos.

Valores esperados observados en el fixture:

- Alfonso Herrero (`261509`): titular, 96 minutos, rating `6,47`.
- Carlos Puga (`447938`): titular, 63 minutos, rating `6,80`.
- Ángel Fortuño (`449409`): titular, 96 minutos, rating `7,24`.
- Dos goles: Bryan Zaragoza y Adrián Niño.
- Dos asistencias: Javi Hernández y Rafita.
- Nueve amarillas.

Estos valores son checks del fixture, no resultados de una carga automática
contra el sitio.

## 7. Vinculación con el catálogo

Los nombres de equipo no coinciden siempre entre WhoScored y Football-Data;
por ejemplo, WhoScored puede usar `Arsenal` y Football-Data `Arsenal FC`.

La decisión de vinculación es:

1. Normalizar el nombre del jugador: eliminar tildes, pasar a minúsculas,
   recortar y colapsar espacios.
2. Buscar por nombre normalizado dentro de la misma liga.
3. Si hay varios candidatos, comparar los nombres de equipo normalizados
   considerando que uno esté contenido en el otro.
4. Si la coincidencia sigue siendo ambigua o no existe, registrar un pendiente
   y no elegir por posición u otra heurística.
5. Guardar el `whoscoredId` cuando la vinculación es inequívoca.

El porcentaje de jugadores vinculados se midió en la primera carga real; ver
la sección 9.

## 8. Alternativas descartadas

### Cliente HTTP sin navegador

Descartado como estrategia principal: la protección anti-bots bloquea la
obtención directa sin el contexto de navegador/cookies observado en la prueba.

### Leer `page.content()` como fuente de datos

Descartado como estrategia principal: después de cargar, la página borra o
modifica el script con los datos, por lo que `page.content()` no contiene de
forma confiable `matchCentreData`. Además, puede fallar si la página sigue
navegando.

### Scrapear por jugador

Descartado: requiere abrir muchas más páginas y repite navegación para datos que
WhoScored entrega juntos en la página del partido. El diseño scrapea por
partido y procesa los jugadores de ambos equipos en una sola carga.

## 9. Primera carga real (verificación manual, 2026-10-29)

Carga hecha en la computadora de la demo siguiendo
[quickstart.md](quickstart.md). Todos los números salen de
`GET /api/metricas/sincronizacion/{id}`, del log del backend y de consultas
directas a la base después de la carga.

### Configuración no sensible

- `modo: automatico`, `headless: false`, perfil persistente propio y fuera del
  repositorio (pasado por variable de entorno, no commiteado).
- `stageId`, `inicio-temporada`, `zona-fuente`, `pausa-ms`, `timeout-ms` y
  `reintentos` con los valores de `application.yml`.
- Chrome real (`channel: chrome`). En el primer arranque Playwright 1.52.0
  descargó sus binarios.
- PostgreSQL local con el catálogo ya sincronizado desde Football-Data
  (2629 jugadores) y sin métricas previas.

### Ejecuciones

| Ejecución | `correlationId` | `desde` | Inicio (UTC) | Fin (UTC) | Duración | Estado |
|---:|---|---|---|---|---|---|
| 1 | `prueba-whoscored-20261029` | 2026-10-20 | 14:04:09 | 14:11:50 | 7 min 41 s | `COMPLETADA` |
| 2 | `historica-whoscored-20261029` | 2026-08-01 | 14:12:01 | 14:45:06 | 33 min 5 s | `COMPLETADA` |

La duración de la ejecución 1 incluye la descarga inicial de los binarios de
Playwright.

Resumen por liga. "Omitidos" es `encontrados - cargados`: partidos no
terminados, anteriores a `desde` o ya cargados. En la ejecución 2 incluye los
57 partidos de la ejecución 1, que no se volvieron a descargar.

| Ejecución | Liga | Encontrados | Cargados | Omitidos | Fallidos | Vinculados* | Pendientes* |
|---:|---|---:|---:|---:|---:|---:|---:|
| 1 | Premier League | 38 | 10 | 28 | 0 | 283 | 20 |
| 1 | Bundesliga | 34 | 9 | 25 | 0 | 255 | 26 |
| 1 | La Liga | 36 | 11 | 25 | 0 | 280 | 68 |
| 1 | Serie A | 43 | 18 | 25 | 0 | 513 | 58 |
| 1 | Ligue 1 | 31 | 9 | 22 | 0 | 231 | 48 |
| 2 | Premier League | 88 | 70 | 18 | 0 | 2010 | 148 |
| 2 | Bundesliga | 70 | 54 | 16 | 0 | 1530 | 168 |
| 2 | La Liga | 105 | 88 | 17 | 0 | 2233 | 559 |
| 2 | Serie A | 93 | 70 | 23 | 0 | 1991 | 235 |
| 2 | Ligue 1 | 76 | 63 | 13 | 0 | 1632 | 341 |

\* Los contadores del resumen cuentan **apariciones** (jugador por partido), no
jugadores únicos.

No hubo errores por liga ni partidos fallidos. El log no registró `WARN` ni
`ERROR` durante las cargas. WhoScored no mostró ninguna verificación que
hubiera que resolver a mano. Chrome llegó a las páginas `/Matches/{id}/Live`, la
lectura por JavaScript funcionó y el navegador se cerró al terminar.

### Estado de la base después de las dos cargas

| Liga | Partidos | Primer partido | Último partido | Jugadores vinculados (únicos) | Pendientes (únicos) | % vinculados |
|---|---:|---|---|---:|---:|---:|
| Premier League | 80 | 2026-08-21 | 2026-10-25 | 411 | 36 | 91,9 % |
| Bundesliga | 63 | 2026-08-28 | 2026-10-25 | 361 | 40 | 90,0 % |
| La Liga | 99 | 2026-08-15 | 2026-10-26 | 370 | 110 | 77,1 % |
| Serie A | 88 | 2026-08-22 | 2026-10-28 | 422 | 64 | 86,8 % |
| Ligue 1 | 72 | 2026-08-21 | 2026-10-26 | 332 | 86 | 79,4 % |
| **Total** | **402** | | | **1896** | **336** | **84,9 %** |

- Filas en `metrica_partido`: 10958, de 1896 jugadores distintos.
- Ningún pendiente quedó también vinculado: los dos conjuntos son disjuntos.
- % vinculados por jugador único = `vinculados / (vinculados + pendientes)`
  = 1896 / 2232 = 84,9 %.
- % por aparición: 10958 / (10958 + 1671) = 86,8 %.

### Causas de los pendientes

`pendiente_vinculacion` no guarda la causa. Se clasificaron los 336 pendientes
fuera del sistema, comparándolos con `jugadores` mediante la misma
normalización de `NormalizadorNombre`:

| Causa | Cantidad | Ejemplos (WhoScored → catálogo) |
|---|---:|---|
| Ningún jugador del catálogo de esa liga tiene ese nombre, ni completo ni parcial (el equipo sí existe) | 202 | Abdul Fatawu (Ipswich), Charly Alcaraz (Everton) |
| El mismo jugador figura con otro nombre: nombre abreviado, apellido compuesto o guion contra espacio | 104 | "Kiernan Dewsbury-Hall" → "Kiernan Dewsbury Hall"; "Jaden Philogene" → "Jaden Philogene-Bidace"; "Igor Thiago" → "Thiago"; "Reinildo Mandava" → "Reinildo" |
| No está en el catálogo y el nombre del equipo tampoco coincide por contención | 26 | Joe Scally (Borussia M.Gladbach), Rômulo (RBL) |
| Está en el catálogo pero en otra liga: transferencia o catálogo desactualizado | 4 | Cody Drameh (WhoScored: Hull; catálogo: Genoa) |
| Homónimo ambiguo dentro de la liga | 0 | — |

Por liga, La Liga (110) y Ligue 1 (86) concentran la mayoría de los pendientes.
Las causas más comunes son jugadores que faltan en el catálogo de Football-Data
y diferencias de escritura del nombre entre las dos fuentes. Como lo exige la
regla de vinculación, ningún pendiente se asignó por heurística.

### Recálculo de cotizaciones (006)

`POST /api/cotizaciones/recalcular` (`correlationId`
`recalculo-post-whoscored-20261029`) respondió `COMPLETADO`:
`semanasCalculadas=1`, `jugadoresCotizados=5258`, `jugadoresSinMetricas=1355`,
estrategias `RENDIMIENTO_POSICION` v1 y `CONSISTENCIA` v1. Duró 53 s.

### Respaldo

Después del recálculo se hizo un `pg_dump` de la base a un archivo `.sql`, que
se guardó fuera del repositorio. No se commitearon cookies, el token, el perfil
de Chrome ni el dump.
