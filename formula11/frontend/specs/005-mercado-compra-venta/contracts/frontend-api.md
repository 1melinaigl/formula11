# Contrato frontend ↔ API: Mercado

Fuente: `backend/specs/005-contratos-entrega2/contracts/openapi.yaml`. Todas las
consultas usan `solicitarProtegida` (Bearer + `X-Correlation-Id`).

## Consultas

| Uso | Método y ruta | Respuesta |
|---|---|---|
| Precio y disponibilidad | `GET /api/jugadores/{jugadorId}/mercado` | `MercadoJugadorResponse` |
| Saldo y tenencia | `GET /api/usuarios/{usuarioId}/portfolio` con el `usuario.id` de la sesión | `PortfolioResponse` |

## Órdenes

| Uso | Método y ruta | Cuerpo | Respuesta |
|---|---|---|---|
| Comprar | `POST /api/ordenes/compra` | `{ jugadorId, cantidad }` | `201 OperacionResponse` |
| Vender | `POST /api/ordenes/venta` | `{ jugadorId, cantidad }` | `201 OperacionResponse` |

El usuario de la orden sale del JWT; no va en el cuerpo.

## Errores

Esquema `Error`: `{ mensaje, timestamp, correlationId }`.

| Código | Dónde | Tratamiento en el front |
|---|---|---|
| `400` | órdenes | Mensaje del backend; no hay confirmación. |
| `401` | todas | `clienteApi` limpia la sesión; se muestra el acceso denegado. |
| `403` | portfolio | Solo si el id no es el del JWT; el front nunca arma otra ruta. |
| `404` | `/mercado`, portfolio, órdenes | Mensaje del backend; sin formulario si falla una consulta. |
| `409` | compra | El superusuario no tiene tokens suficientes; mensaje del backend. |
| `422` | compra y venta | Saldo o tokens insuficientes; mensaje del backend. |

Si el backend no manda `correlationId`, `clienteApi` usa el `X-Correlation-Id` que generó;
si no manda `mensaje`, muestra "No se pudo completar la solicitud.".

## Rol

El panel de operación se muestra solo al `USUARIO`. El contrato no define una respuesta
específica si el `SUPERUSUARIO` envía una orden; el frontend no la envía.
