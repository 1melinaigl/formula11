# Data Model: Ranking de jugadores

## Entities

### RankingPageResponse

| Field | Type | Rules |
|---|---|---|
| `content` | `RankingItem[]` | Filas en el orden y con el puesto entregados por backend. |
| `page` | number | Índice base cero de la página actual. |
| `size` | number | Tamaño solicitado; esta feature usa 20. |
| `totalElements` | number | Cantidad total tras filtros. |
| `totalPages` | number | Cantidad total de páginas. |
| `estrategia` | strategy code \| null | Estrategia activa reportada por el backend; no se solicita otra en esta feature. |
| `version` | number \| null | Versión del último recálculo. |
| `fecha` | `YYYY-MM-DD` \| null | Fecha de la última cotización. |

### RankingItem

| Field | Type | Rules |
|---|---|---|
| `puesto` | number | Puesto asignado por backend; no se recalcula. |
| `jugador` | `JugadorRanking` | Jugador anidado según contrato. |
| `precio` | number | Créditos, hasta cuatro decimales contractuales. |
| `variacion` | number \| null | Porcentaje contractual; `12.5` significa `12,5 %`. |
| `score` | number \| null | Score del último cálculo; se muestra `—` si falta. |

### JugadorRanking

| Field | Type | Rules |
|---|---|---|
| `id` | number | Identificador para `/jugadores/:id`. |
| `nombre` | string | Texto del enlace al detalle. |
| `equipo` | string | Se oculta visualmente hasta 760px. |
| `liga` | string | Nombre legible de la liga. |
| `posicion` | `POR \| DEF \| MED \| DEL` | Código mostrado mediante etiqueta legible. |

### RankingQuery

| Field | Type | Rules |
|---|---|---|
| `page` | number | Base cero; inicia en 0. |
| `size` | number | Fijo en 20. |
| `posicion` | code \| empty | Se envía solo si hay filtro. |
| `liga` | string \| empty | Se envía solo si hay filtro. |
| `estrategia` | not sent | Fuera del alcance de esta feature. |

### Sesion

| Field | Type | Rules |
|---|---|---|
| `usuario` | object | Sesión existente. |
| `rol` | `USUARIO \| SUPERUSUARIO` | No cambia la vista de esta feature. |
| `token` | string | Interno; no se muestra ni registra. |

## Relationships

- Una `RankingPageResponse` contiene cero o muchas `RankingItem`.
- Cada `RankingItem` referencia un `JugadorRanking`.
- `RankingQuery` determina la página y los filtros enviados al backend.
- `Sesion` habilita la llamada protegida; sin sesión no se consulta la API.

## Transformations and validation

- El frontend no reordena, recalcula puestos ni filtra filas localmente.
- Se descartan filas sin `jugador.id` o con precio no numérico finito.
- `variacion` se transforma de porcentaje contractual a fracción únicamente para
  reutilizar el formateador visual.
- Las fechas `YYYY-MM-DD` se formatean por componentes, sin pasar por `Date`.
- `null` en `score` se muestra como `—`; `null` en metadatos indica ausencia de
  recálculo y no un error.
