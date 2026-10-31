# Contrato frontend ↔ API: Historial de operaciones

Fuente: `backend/specs/005-contratos-entrega2/contracts/openapi.yaml`. La consulta usa
`solicitarProtegida` (Bearer + `X-Correlation-Id`) y el `usuario.id` de la sesión. Solo
la hace el rol `USUARIO`.

## Consulta

| Uso | Método y ruta | Respuesta |
|---|---|---|
| Historial | `GET /api/usuarios/{usuarioId}/transacciones?page=&size=10[&tipo=COMPRA\|VENTA]` | `TransaccionesPageResponse` |

`content[]` es `OperacionResponse { id, tipo, jugador, cantidad, precioUnitario, total, saldoPosterior, contraparte, fecha, estrategia, version }`.
El backend ordena de la más nueva a la más vieja. El front no envía `sort` ni `jugadorId`.

## Errores

Esquema `Error`: `{ mensaje, timestamp, correlationId }`.

| Código | Tratamiento en el front |
|---|---|
| `400` / `404` / `5xx` | Se muestran `mensaje` y `correlationId`. |
| `401` | `clienteApi` limpia la sesión; acceso denegado. |
| `403` | Solo si el id no es el del JWT; se muestra como cualquier error. |

Si falta `correlationId`, `clienteApi` usa el `X-Correlation-Id` generado; si falta
`mensaje`, muestra "No se pudo completar la solicitud.".
