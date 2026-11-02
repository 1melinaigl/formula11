# Data Model: Mercado — compra y venta de tokens

Entidades que consume el frontend. Los nombres y tipos siguen el contrato de Entrega 2;
ver [contracts/frontend-api.md](./contracts/frontend-api.md).

## MercadoJugador (`MercadoJugadorResponse`)

| Campo | Tipo | Regla |
|---|---|---|
| `jugadorId` | entero | Coincide con el jugador del detalle. |
| `tokensEmitidos` | entero | 100 por jugador. |
| `tokensDisponibles` | entero ≥ 0 | Tokens que conserva el superusuario. 0 = jugador agotado. |
| `precioVigente` | créditos (hasta 4 decimales) | Precio referencial de la estrategia activa. |
| `fechaCotizacion` | fecha `AAAA-MM-DD` \| null | Null antes del primer recálculo: precio inicial de 1 crédito. |

## Portfolio (subconjunto de `PortfolioResponse` usado por el panel)

| Campo | Tipo | Regla |
|---|---|---|
| `saldo` | créditos | Saldo disponible del usuario de la sesión. |
| `posiciones[].jugador.id` | entero | Identifica la tenencia del jugador del detalle. |
| `posiciones[].cantidad` | entero > 0 | Tokens que el usuario puede vender. |
| `posiciones[].precioPromedio` | créditos | Se muestra junto a la tenencia. |

## Orden (`OrdenRequest`)

| Campo | Tipo | Regla |
|---|---|---|
| `jugadorId` | entero | Del detalle. |
| `cantidad` | entero 1–100 | Validada en el cliente antes de enviar. |

## Operación (`OperacionResponse`)

`{ id, tipo, jugador, cantidad, precioUnitario, total, saldoPosterior, contraparte, fecha, estrategia, version }`.
La confirmación usa `cantidad`, `precioUnitario`, `total` y `saldoPosterior`.

## Estado del panel

| Estado | Condición | Qué se ve |
|---|---|---|
| Cargando | Portfolio o `/mercado` pendientes | "Cargando tu saldo..." |
| Error de datos | Falla portfolio o `/mercado` | Error con `correlationId`; sin formulario. |
| Listo | Datos cargados | Formulario con tipo, cantidad y total estimado. |
| Enviando | Orden en curso | Botón "Enviando..."; selector y cantidad deshabilitados. |
| Confirmado | Orden `201` | Mensaje con `role="status"`; se vuelven a pedir los datos. |
| Confirmado con error de actualización | `201` y falla la recarga | Confirmación + error + "Reintentar". |
| Error de orden | `4xx`/`5xx` | Error con `correlationId`; envío habilitado; cantidad conservada. |

## Reglas de validación (en este orden)

1. Cantidad vacía → "Ingresá una cantidad de tokens."
2. No entera o menor a 1 → "La cantidad debe ser un número entero mayor a 0."
3. Mayor a 100 → "Podés operar hasta 100 tokens por orden."
4. Venta mayor a la tenencia → "Solo tenés N tokens para vender." / "No tenés tokens de este jugador para vender."
5. Compra con `tokensDisponibles` en 0 → "No quedan tokens disponibles de este jugador."; mayor a los disponibles → "Solo quedan N tokens disponibles."
6. Compra con total mayor al saldo → "Saldo insuficiente: necesitás X créditos."
