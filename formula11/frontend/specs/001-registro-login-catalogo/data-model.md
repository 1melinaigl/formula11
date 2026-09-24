# Data Model: Registro, login y catálogo de jugadores

## Cuenta

Representa la identidad devuelta por registro o login.

| Campo | Tipo | Reglas | Origen |
|---|---|---|---|
| `id` | integer/int64 | Identificador del backend | Registro/Login |
| `nombre` | string | 1-100 caracteres al registrar | Registro/Login |
| `email` | string | Email válido, máximo 254 caracteres | Registro/Login |
| `fechaRegistro` | date-time | Presente en registro exitoso | Registro |

La cuenta no contiene ni expone el token en el modelo de presentación.

## Sesion

Estado local que indica si existe una cuenta autenticada.

| Campo | Tipo | Reglas | Ciclo de vida |
|---|---|---|---|
| `usuario` | Cuenta | Puede ser nulo | Se establece al registrar o ingresar |
| `token` | string | Solo interno; nunca se renderiza ni se loguea | Vive en `sessionStorage` hasta logout, cierre de pestaña o `401` protegido |
| `estado` | enum | `restaurando`, `anonima`, `autenticada` | Se deriva al iniciar y al cambiar la credencial |

Transiciones principales: `restaurando -> anonima` si no hay credencial, `restaurando ->
autenticada` si la credencial es recuperable, `anonima -> autenticada` tras registro/login,
y `autenticada -> anonima` tras logout o `401` de un endpoint protegido.

## Jugador

Elemento recibido desde `GET /api/jugadores`.

| Campo | Tipo | Reglas |
|---|---|---|
| `id` | integer/int64 | Identificador del backend |
| `nombre` | string | Texto mostrado y usado por búsqueda |
| `equipo` | string | Texto mostrado y usado por filtro |
| `liga` | enum/string | `Premier League`, `Bundesliga`, `La Liga`, `Serie A` o `Ligue 1` según contrato |
| `posicion` | string | Se muestra tal cual; `POR`, `DEF`, `MED`, `DEL` reciben etiqueta/color conocido y otros valores, estilo neutro |

La colección puede ser vacía. No se agregan jugadores locales ni cuentas demo.

## FiltroCatalogo

Estado efímero de consulta local, no enviado al backend.

| Campo | Tipo | Valor vacío |
|---|---|---|
| `nombre` | string | Cadena vacía |
| `liga` | string | Sin selección |
| `posicion` | string | Sin selección |
| `equipo` | string | Sin selección |

Los campos se combinan con AND; `nombre` compara sin distinguir mayúsculas/minúsculas.
Los filtros activos se reflejan en chips y `Limpiar` restaura todos sus valores vacíos.

## ErrorServicio

Representación normalizada de cualquier respuesta de error manejada por el cliente API.

| Campo | Tipo | Reglas |
|---|---|---|
| `mensaje` | string | Se muestra en español según respuesta del servicio |
| `timestamp` | date-time/string | Se conserva si existe |
| `correlationId` | string | Cuerpo, header de respuesta o id enviado como fallback |
| `status` | integer | Metadato local para decidir validación, aviso o limpieza de sesión |

Los errores locales de forma se modelan por campo y no sustituyen el `ErrorServicio` general.
