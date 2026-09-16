# Data Model: Registro y catálogo

## Usuario

| Campo | Tipo | Reglas |
|---|---|---|
| `id` | `Long` | PK generada, no expuesta como requisito de input |
| `nombre` | `String` | obligatorio, trim, longitud acotada |
| `email` | `String` | obligatorio, trim + lowercase, formato email, único |
| `passwordHash` | `String` | obligatorio, BCrypt, nunca se serializa |
| `fechaRegistro` | `Instant` | obligatorio, generado por el backend |

La entidad `Usuario` no almacena el JWT: el token es una credencial firmada y temporal, no un dato persistente de esta entrega.

## Jugador

| Campo | Tipo | Reglas |
|---|---|---|
| `id` | `Long` | PK generada |
| `nombre` | `String` | obligatorio, saneado y con longitud acotada |
| `equipo` | `String` | obligatorio, saneado y con longitud acotada |
| `liga` | `Liga` | obligatorio, enum cerrado |
| `posicion` | `String` | obligatorio, saneado y con longitud acotada |

## Liga

Valores permitidos exactamente:

- `PREMIER_LEAGUE` -> `Premier League`
- `BUNDESLIGA` -> `Bundesliga`
- `LA_LIGA` -> `La Liga`
- `SERIE_A` -> `Serie A`
- `LIGUE_1` -> `Ligue 1`

Se persistirá como `STRING` y el DTO de salida expondrá el valor legible. No se agregan cotización, precio, tokens ni relaciones de mercado.

## Transiciones e invariantes

- Registro: request válido -> email normalizado -> password hasheado -> usuario persistido -> JWT emitido.
- Email duplicado: request válido -> búsqueda/índice único detecta conflicto -> `409`, sin usuario adicional ni token.
- Consulta: JWT válido -> service consulta repository -> lista de jugadores, posiblemente vacía.
- Un `Jugador` no puede construirse con una liga fuera del enum.
- Ninguna respuesta de usuario contiene `passwordHash`.
