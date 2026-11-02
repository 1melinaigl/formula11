# Data Model: Señales de jugadores subvaluados y panel de aciertos

Las formas exactas están en [contracts/frontend-api.md](./contracts/frontend-api.md).

## Señal (`Senal`)

| Campo | Uso |
|---|---|
| `id` | Clave de la fila y del registro de señales vistas. Es estable entre recálculos. |
| `jugador` (`id`, `nombre`, `equipo`, `liga`, `posicion`) | Nombre con link a `/jugadores/{id}`, equipo, liga legible e insignia de posición. |
| `fecha` (`date`) | Semana del recálculo: "al dd/mm/aaaa", sin correrse de día. |
| `tipo` (`ENTRE_ESTRATEGIAS` \| `PRECIO_ATRASADO`) | Texto legible y ayuda (FR-008). Si llega otro valor, la señal se descarta. |
| `brecha` (porcentaje) | "+18,00%". Es la base del orden y de las 3 que muestra la alerta. |
| `precio` | Precio vigente, en créditos. |
| `precioEstimado` | Precio estimado, en créditos. |

`normalizarSenales(data)` devuelve `{ senales, page, totalPages, totalElements }`.
Descarta las señales con un tipo desconocido o sin `id` o `jugador`, y convierte
los números con `Number`. No reordena nada: el orden es el del backend.

## Página (`SenalesPageResponse`)

`content`, `page`, `size`, `totalElements`, `totalPages`.

## Consulta de la lista

| Campo | Regla |
|---|---|
| `vigentes` | `true` en "Vigentes" (por defecto) y `false` en "Historial". |
| `page` | Base 0. Vuelve a 0 al cambiar de vista. Si la página queda fuera de rango, vuelve una sola vez a `totalPages - 1`. |
| `size` | 20. |

## Consulta periódica

`vigentes=true`, `page=0`, `size=100`, cada 30 segundos mientras haya sesión. Se
saltea un ciclo si la consulta anterior sigue pendiente.

## Señales vistas (solo en el front)

| Aspecto | Regla |
|---|---|
| Clave | `formula11.senales.vistas.{usuarioId}` en `sessionStorage`. |
| Valor | Arreglo JSON de ids, con un máximo de 500. Al pasarse, se descartan los más viejos. |
| Alta | Al cerrar la alerta, al pulsar "Ver señales" y al mostrar las vigentes en `/senales`. |
| Baja | Se borra la clave cuando `usuario` pasa a `null` (logout o `401`). Si el JSON es inválido, se toma como vacío. |

`senalesNuevas(senales, vistas)` devuelve las señales cuyo `id` no está en
`vistas`, en el mismo orden. La alerta muestra la cantidad y las 3 primeras.

## Estado de la alerta

| Estado | Condición | Se ve |
|---|---|---|
| Oculta | Sin sesión, sin señales nuevas o recién cerrada. | Nada. |
| Visible | Hay señales nuevas después de una consulta. | "N oportunidades nuevas", las 3 de mayor brecha, "Ver señales" y "Cerrar". |

Si llegan más señales nuevas con la alerta visible, la alerta se actualiza con el
total de las no vistas. No se apilan varias alertas.

## Compra en un click

| Estado por señal | Se ve |
|---|---|
| Lista | Botón "Comprar 1 token" (solo para el rol `USUARIO`). |
| Enviando | Botón deshabilitado con "Comprando...". |
| Confirmada | Mensaje con `role="status"`: el texto de `describirResultado` (005) más el link "Ver historial". |
| Rechazada | `AvisoError` con `mensaje` y `correlationId`. El botón vuelve a estar disponible. |

Solo se muestra un resultado a la vez: comprar en otra fila reemplaza el mensaje
anterior.

## Aciertos (`AciertosResponse`)

| Campo | Uso |
|---|---|
| `tasaAcierto` | Indicador "Tasa de acierto", por ejemplo "50,00%". |
| `diferenciaPromedio` | Indicador "Diferencia contra el mercado", con signo y color. |
| `semanas[].fecha` | Semana medida, "dd/mm/aaaa". |
| `semanas[].senales` | Cantidad de señales evaluadas. |
| `semanas[].variacionSenalados` | Porcentaje con signo y color. |
| `semanas[].variacionMercado` | Porcentaje con signo y color (se oculta hasta 760px). |
| `semanas[].aciertos` | "17 de 23", contra `senales`. |

Si `semanas` está vacía, se muestra el estado vacío del panel y no los
indicadores en 0.
