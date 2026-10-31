# Contrato frontend ↔ API: Portfolio

Fuente: `backend/specs/005-contratos-entrega2/contracts/openapi.yaml`. Las dos consultas
usan `solicitarProtegida` (Bearer + `X-Correlation-Id`) y el `usuario.id` de la sesión.
Solo las hace el rol `USUARIO`.

## Consultas

| Uso | Método y ruta | Respuesta |
|---|---|---|
| Posición actual | `GET /api/usuarios/{usuarioId}/portfolio` | `PortfolioResponse { usuarioId, fechaValuacion, saldo, invertido, valorTenencias, ganancia, gananciaPorcentaje, posiciones }` |
| Evolución | `GET /api/usuarios/{usuarioId}/portfolio/evolucion` (sin `desde` ni `hasta`) | `{ usuarioId, puntos: [{ fecha, valor, invertido }] }` |

`posiciones[]` es `PosicionPortfolio { jugador, cantidad, precioPromedio, precioActual, invertido, valorActual, ganancia, gananciaPorcentaje }`.
Los porcentajes vienen como número (12.5 = 12,5 %).

## Errores

Esquema `Error`: `{ mensaje, timestamp, correlationId }`.

| Código | Tratamiento en el front |
|---|---|
| `401` | `clienteApi` limpia la sesión; acceso denegado. |
| `403` | Solo si el id no es el del JWT; el front no arma otra ruta. |
| `404` / `5xx` en el portfolio | Se muestra solo el error. |
| `400` / `404` / `5xx` en la evolución | Se muestra el error en lugar de ese gráfico; el resto sigue visible. |

Si falta `correlationId`, `clienteApi` usa el `X-Correlation-Id` generado; si falta
`mensaje`, muestra "No se pudo completar la solicitud.".
