# Data Model: Portfolio y gráficos del usuario

Ver [contracts/frontend-api.md](./contracts/frontend-api.md) para las formas exactas.

## Portfolio (`PortfolioResponse`)

| Campo | Uso en la pantalla |
|---|---|
| `fechaValuacion` | "Valuado al dd/mm/aaaa" sobre el título. |
| `saldo` | Tarjeta "Saldo disponible" y suma del valor total. |
| `invertido` | Tarjeta "Invertido". |
| `valorTenencias` | Tarjeta "Valor de tus tokens" y suma del valor total. |
| `ganancia` | Tarjeta "Ganancia" o "Pérdida" (según el signo), con signo. |
| `gananciaPorcentaje` | Porcentaje sobre lo invertido; "Sin inversión" si `invertido` es 0. |
| `posiciones[]` | Filas de la tabla y datos de los gráficos de composición y ganancia. |

**Derivado en el front**: valor total = `saldo` + `valorTenencias`.

## Posición (`PosicionPortfolio`)

| Campo | Uso |
|---|---|
| `jugador` (`id`, `nombre`, `posicion`) | Nombre con link al detalle, insignia y "Vender". |
| `cantidad` | Columna "Tokens"; las filas con 0 no se muestran. |
| `precioPromedio`, `precioActual` | Columnas (se ocultan hasta 760px). |
| `invertido` | No se muestra por fila; lo usa el resumen del backend. |
| `valorActual` | Columna "Valor", orden de la tabla y composición. |
| `ganancia`, `gananciaPorcentaje` | Columna "Ganancia" y gráfico de ganancia o pérdida. |

**Orden**: `valorActual` descendente; a igual valor, `jugador.nombre` ascendente.

## Punto de evolución (`EvolucionPortfolioResponse.puntos[]`)

| Campo | Uso |
|---|---|
| `fecha` | Eje X (dd/mm) y tabla oculta (dd/mm/aaaa). |
| `valor` | Línea "Valor actual". |
| `invertido` | Línea escalonada "Invertido". |

Se ordena por fecha ascendente. Con menos de 2 puntos se muestra el aviso de poca historia.

## Composición (derivada)

Los 7 jugadores de mayor valor (mismo orden que la tabla) y una fila "Otros (n)" con la
suma del resto; cada fila lleva su porcentaje sobre la suma de `valorActual`.

## Estados de la página

| Estado | Condición | Qué se ve |
|---|---|---|
| Superusuario | `rol` = `SUPERUSUARIO` | Aviso; sin consultas. |
| Cargando | Consultas pendientes | "Cargando portfolio..." |
| Error del portfolio | Falla el portfolio | Error con `correlationId`; nada más. |
| Sin tenencias | `posiciones` vacío | Resumen + invitación a ranking y catálogo. |
| Con tenencias | `posiciones` con datos | Resumen, gráficos y tabla. |
| Error de evolución | Falla la evolución | Error en lugar del gráfico de evolución; el resto visible. |
| Poca historia | Evolución con 0 o 1 punto | Tarjeta con el aviso. |
