# Data Model: Auditoría de web services

No hay tablas ni migraciones nuevas. La única estructura es la línea que se escribe en el
archivo de auditoría.

## LineaAuditoria (record, no se persiste)

Paquete: `com.formula11.observabilidad`.

| Campo | Tipo | Regla |
|---|---|---|
| `usuario` | `String` | Email del usuario autenticado o `anónimo`. Nunca vacío. |
| `operacion` | `String` | `NombreSimpleDeClase.metodo`, por ejemplo `MercadoController.comprar`. |
| `parametros` | `String` | Mapa `nombre=valor` ya enmascarado y recortado a 500 caracteres. `{}` si no hay. |
| `duracionMs` | `long` | Mayor o igual a 0. |
| `resultado` | `String` | `OK`, o `ERROR(NombreSimpleDeLaExcepcion)`. |

La fecha y hora y el correlation ID no son campos del record: los agrega el patrón de
logback (ver [contracts/linea-auditoria.md](contracts/linea-auditoria.md)).

## EnmascaradorParametros (sin estado)

- Entrada: nombres y valores de los argumentos del método.
- Omite argumentos de infraestructura (`Authentication`, `Principal`, `ServletRequest`,
  `ServletResponse`, `HttpSession`, `BindingResult`, `Errors`).
- Reemplaza por `****` los campos llamados `password`, `contrasena`, `token`,
  `accessToken`, `refreshToken` o `secret` (sin distinguir mayúsculas, a cualquier
  profundidad). Si el argumento mismo se llama así, también se enmascara.
- `null` se muestra como `null`.
- Recorta el resultado a 500 caracteres y agrega `…(recortado)`.
