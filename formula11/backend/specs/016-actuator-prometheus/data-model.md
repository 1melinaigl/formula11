# Data Model: Actuator y Prometheus

**Feature**: [spec.md](spec.md) | **Fecha**: 2026-11-26

No hay tablas ni migraciones nuevas. Las métricas viven en memoria (Micrometer) y el health
lee una entidad que ya existe.

## EjecucionMetricas (existente, solo lectura)

| Campo | Uso en esta feature |
|---|---|
| `id` | Orden para elegir la última carga terminada; va en el detalle del health como `ejecucionId`. |
| `estado` | `EN_CURSO` se ignora. `FALLIDA` → `DOWN`; `COMPLETADA` y `PARCIAL` → `UP`. |
| `fin` | Va en el detalle del health. |
| `error` | Va en el detalle si el estado es `FALLIDA`. |

Consulta nueva: `EjecucionMetricasRepository.findFirstByEstadoInOrderByIdDesc(Collection<String> estados)`.
Service nuevo: `CargaMetricasService.ultimaTerminada(): Optional<EjecucionMetricas>`.

## Estado del componente `cargaMetricas`

| Última carga terminada | Estado | Detalles |
|---|---|---|
| ninguna | `UP` | `detalle: "Sin cargas de métricas"` |
| `COMPLETADA` | `UP` | `estado`, `fin`, `ejecucionId` |
| `PARCIAL` | `UP` | `estado`, `fin`, `ejecucionId` |
| `FALLIDA` | `DOWN` | `estado`, `fin`, `ejecucionId`, `error` |
| la consulta lanza una excepción | `DOWN` | `error` (lo arma Spring Boot) |

Cuando una carga `COMPLETADA` o `PARCIAL` es posterior a una `FALLIDA`, el componente vuelve
a `UP`. No hay estado guardado en el indicador.

## TipoOperacionMercado (enum nuevo)

| Valor | Etiqueta `tipo` | Origen |
|---|---|---|
| `COMPRA` | `compra` | `MercadoService.comprar(..)` → `Transaccion.Tipo.COMPRA` |
| `VENTA` | `venta` | `MercadoService.vender(..)` → `Transaccion.Tipo.VENTA` |
| `P2P` | `p2p` | feature de A, con una llamada directa a `MetricasNegocio` |

El mapeo desde `Transaccion.Tipo` se hace por nombre (`valueOf(tipo.name())`). Un tipo que no
exista en el enum se ignora con un `warn` y no rompe la operación (FR-009).

## Métricas (Micrometer → Prometheus)

| Micrometer | Tipo | Tags | Prometheus |
|---|---|---|---|
| `formula11.mercado.operaciones` | Counter | `tipo` = `compra`, `venta`, `p2p` | `formula11_mercado_operaciones_total` |
| `formula11.cotizaciones.recalculo` | Timer | `resultado` = `ok`, `error` | `formula11_cotizaciones_recalculo_seconds_count`, `_sum`, `_max` |

Reglas:

- Los tres contadores se registran al crear `MetricasNegocio`, así que valen `0` desde el arranque.
- Solo suman las operaciones confirmadas: después del commit si hay una transacción activa.
- El timer registra cada llamada a `recalcular`, incluida la que se rechaza porque ya hay otra
  en curso o porque el rango es inválido (`resultado=error`).
- Todo se reinicia al reiniciar la aplicación; la historia la guarda Prometheus.
