# Contrato: métricas de negocio

## En `/actuator/prometheus`

```text
# HELP formula11_mercado_operaciones_total Operaciones de mercado confirmadas por tipo
# TYPE formula11_mercado_operaciones_total counter
formula11_mercado_operaciones_total{tipo="compra"} 3.0
formula11_mercado_operaciones_total{tipo="venta"} 1.0
formula11_mercado_operaciones_total{tipo="p2p"} 0.0
# HELP formula11_cotizaciones_recalculo_seconds Duración del recálculo de cotizaciones
# TYPE formula11_cotizaciones_recalculo_seconds summary
formula11_cotizaciones_recalculo_seconds_count{resultado="ok"} 1
formula11_cotizaciones_recalculo_seconds_sum{resultado="ok"} 2.84
# TYPE formula11_cotizaciones_recalculo_seconds_max gauge
formula11_cotizaciones_recalculo_seconds_max{resultado="ok"} 2.84
```

Las líneas de `resultado="error"` aparecen recién con el primer recálculo que falla.
Las de `tipo` están desde el arranque, en `0`.

Además de las etiquetas propias, Micrometer puede agregar etiquetas comunes de la plataforma
(por ejemplo, `application`). Las consultas deben filtrar por `tipo` y `resultado`, no por
la lista completa de etiquetas.

## Para la feature P2P (A)

```java
// com.formula11.observabilidad
public enum TipoOperacionMercado { COMPRA, VENTA, P2P }

@Component
public class MetricasNegocio {
    /** Suma una operación confirmada. Si hay una transacción activa, suma después del commit. */
    public void registrarOperacionMercado(TipoOperacionMercado tipo);
}
```

Uso esperado: el service P2P inyecta `MetricasNegocio` y, cuando confirma la operación, llama
a `metricasNegocio.registrarOperacionMercado(TipoOperacionMercado.P2P)`. Puede llamarlo dentro
del método `@Transactional`: si la transacción hace rollback, no suma. Nunca lanza excepciones.

Alternativa sin código: si A confirma el P2P con un `Transaccion.Tipo.P2P` y desde
`MercadoService.comprar/vender`, el aspecto ya lo cuenta por nombre.
