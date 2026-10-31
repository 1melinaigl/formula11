# Data Model: Historial de operaciones

Ver [contracts/frontend-api.md](./contracts/frontend-api.md) para las formas exactas.

## Operación (`OperacionResponse`)

| Campo | Uso en la tabla |
|---|---|
| `id` | Clave de la fila. |
| `fecha` (`date-time`) | "dd/mm/aaaa, HH:mm" en hora local (24 h). |
| `tipo` (`COMPRA` \| `VENTA`) | Texto "Compra"/"Venta"; otros valores se descartan. |
| `jugador` (`id`, `nombre`, `posicion`) | Nombre con link a `/jugadores/{id}` e insignia de posición. |
| `cantidad` | Columna "Tokens". |
| `precioUnitario` | Columna "Precio"; con "(precio inicial)" si `estrategia` es null. |
| `total` | Movimiento: `-total` en compras, `+total` en ventas. |
| `saldoPosterior` | Columna "Saldo final". |
| `contraparte` | Texto del backend o "—". |
| `estrategia`, `version` | Solo para decidir la marca de precio inicial. |

## Página (`TransaccionesPageResponse`)

`content`, `page`, `size`, `totalElements`, `totalPages`.

## Consulta

| Campo | Regla |
|---|---|
| `page` | Base 0; vuelve a 0 al cambiar el filtro. |
| `size` | 10. |
| `tipo` | `COMPRA`, `VENTA` o ausente ("Todas"). |

**Transiciones**: cambiar el filtro → página 0; cambiar de página → conserva el filtro;
repetir el valor vigente → no consulta; página fuera de rango → una sola vuelta a la
última página válida.

## Estados de la pantalla

| Estado | Qué se ve |
|---|---|
| Superusuario | Aviso; sin consulta. |
| Cargando | "Cargando operaciones..." |
| Vacío | "Sin operaciones" + texto según el filtro + links a ranking y catálogo. |
| Error | `mensaje` y `correlationId`; `401` limpia la sesión. |
| Con datos | Tabla y, si hay más de una página, paginación. |
