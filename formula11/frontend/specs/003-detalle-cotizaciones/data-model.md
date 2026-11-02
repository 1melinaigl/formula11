# Data Model: Detalle de jugador con evolución de cotizaciones

## Entities

### JugadorDetalle

| Field | Type | Rules |
|---|---|---|
| `id` | number | Identificador del jugador. |
| `nombre` | string | Nombre visible. |
| `equipo` | string | Equipo legible. |
| `liga` | string | Liga legible. |
| `posicion` | `POR \| DEF \| MED \| DEL` | Código de posición contractual. |
| `cotizacionActual` | `Cotizacion \| null` | Cotización de la estrategia activa; `null` si no hubo recálculo. |

### Cotizacion

| Field | Type | Rules |
|---|---|---|
| `jugadorId` | number | Debe identificar al jugador consultado. |
| `fecha` | `YYYY-MM-DD` | Una cotización por semana; se ordena ascendentemente para series. |
| `precio` | number | Créditos, hasta cuatro decimales en el contrato. |
| `score` | number | Score calculado por la estrategia. |
| `variacion` | number \| null | Porcentaje contractual: `12.5` significa `12,5 %`; `null` en la primera semana. |
| `estrategia` | `RENDIMIENTO_POSICION \| CONSISTENCIA` | Código de estrategia. |
| `version` | number | Versión de pesos usada. |

### HistorialCotizacionesResponse

| Field | Type | Rules |
|---|---|---|
| `jugadorId` | number | Identificador del jugador. |
| `estrategia` | strategy code | Estrategia solicitada por la respuesta. |
| `cotizaciones` | `Cotizacion[]` | Ordenadas por fecha ascendente; no paginadas en el contrato canónico. |

### Estrategia

| Field | Type | Rules |
|---|---|---|
| `codigo` | strategy code | Código estable de estrategia. |
| `nombre` | string | Nombre legible para UI. |
| `activa` | boolean | Exactamente la estrategia activa debe marcarse para el detalle del superusuario; una respuesta inválida requiere estado de error explícito. |

### Sesion

| Field | Type | Rules |
|---|---|---|
| `usuario` | object | Datos de sesión existentes. |
| `rol` | `USUARIO \| SUPERUSUARIO` | Controla si se pueden consultar ambas estrategias. |
| `token` | string | Interno; no se muestra ni se registra. |

## Relationships

- Un `JugadorDetalle` tiene cero o muchas `Cotizacion`.
- Cada `Cotizacion` pertenece a un jugador y una estrategia.
- Una `Estrategia` puede tener muchas cotizaciones históricas.
- Una `Sesion` determina si el usuario puede solicitar una estrategia distinta de la
  activa.

## Transformations and validation

- Se descartan cotizaciones sin `fecha` o cuyo `precio` no sea numérico finito.
- El backend entrega `variacion` como porcentaje; la representación interna puede usar
  fracción para formatear, pero la UI debe mostrar el porcentaje correcto.
- Las fechas se separan por `-` y se formatean como `dd/mm/aaaa`; no se convierten con
  `Date`.
- Las series se agrupan por fecha; la tabla muestra el historial de la más reciente a
  la más antigua.
- Para `USUARIO`, la colección visible contiene solo la estrategia activa.
- Para `SUPERUSUARIO`, la colección puede contener ambas estrategias y la UI marca la
  que devuelve `GET /api/estrategias` con `activa: true`.
