# Contrato frontend ↔ API: Señales y aciertos

Fuente: `backend/specs/005-contratos-entrega2/contracts/openapi.yaml`, operaciones
con `x-feature: 010-feature-extra`, más la compra de la `007-mercado-tokens`. Todas
las consultas usan `solicitarProtegida` (Bearer + `X-Correlation-Id`).

## Consultas

| Uso | Método y ruta | Respuesta | Quién |
|---|---|---|---|
| Alerta periódica | `GET /api/senales?vigentes=true&page=0&size=100` | `SenalesPageResponse` | Cualquier sesión |
| Lista | `GET /api/senales?vigentes=true\|false&page=&size=20` | `SenalesPageResponse` | Cualquier sesión |
| Panel | `GET /api/senales/aciertos` | `AciertosResponse` | Cualquier sesión |
| Compra en un click | `POST /api/ordenes/compra` con `{ "jugadorId": 7, "cantidad": 1 }` | `201 OperacionResponse` | Solo `USUARIO` |

`SenalesPageResponse.content[]` es
`Senal { id, jugador: JugadorResponse, fecha, tipo, brecha, precio, precioEstimado }`.
El backend ordena por fecha descendente, brecha descendente e id ascendente. El
front no envía `sort`.

`AciertosResponse { semanas: [{ fecha, senales, variacionSenalados, variacionMercado, aciertos }], tasaAcierto, diferenciaPromedio }`,
con las semanas en orden de fecha ascendente. Sin semanas medibles devuelve
`semanas: []`, `tasaAcierto: 0` y `diferenciaPromedio: 0`.

Los porcentajes llegan como número (18 = 18 %) y los precios, en créditos.

## Errores

Esquema `Error`: `{ mensaje, timestamp, correlationId }`.

| Código | Lista / panel | Consulta periódica | Compra |
|---|---|---|---|
| `400` | `mensaje` + `correlationId` | Se ignora y se reintenta en 30 s | `mensaje` + `correlationId` |
| `401` | `clienteApi` limpia la sesión; acceso denegado | Limpia la sesión y corta las consultas | Limpia la sesión; acceso denegado |
| `403` | — | — | No debería pasar: al superusuario no se le ofrece el botón. Si pasa, se muestra el `mensaje`. |
| `404` | — | — | `mensaje` + `correlationId` |
| `409` | — | — | Sin tokens disponibles: `mensaje` + `correlationId` |
| `422` | — | — | Saldo insuficiente: `mensaje` + `correlationId` |
| Red / `5xx` | `mensaje` + `correlationId` | Se ignora y se reintenta en 30 s | `mensaje` + `correlationId` |

Si falta `correlationId`, `clienteApi` usa el `X-Correlation-Id` que generó. Si
falta `mensaje`, muestra "No se pudo completar la solicitud.".

## Handlers de MSW

- Por defecto, en `src/test/handlers.js`: `GET /api/senales` devuelve una página
  vacía y `GET /api/senales/aciertos` devuelve `semanas: []` con los indicadores
  en 0. Así ningún test existente ve alertas.
- Por caso, con `server.use(...)`: señales vigentes y del historial, respuestas
  sucesivas para simular señales nuevas, `401`, `500`, compra `201`, `409` y
  `422`, y aciertos con semanas.
