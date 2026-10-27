# Research: hallazgos de WhoScored

**Feature**: `011-scraper-whoscored`  
**Fuente**: prueba descartable de la rama `spike-whoscored` y fixture local
`src/test/resources/whoscored/partido-1994169.json`.  
**Estado**: hallazgos verificados; no contiene mediciones de una carga
productiva.

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

El porcentaje de jugadores vinculados se medirá en la primera carga real. No se
informa aquí ningún porcentaje porque todavía no existe esa medición.

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

