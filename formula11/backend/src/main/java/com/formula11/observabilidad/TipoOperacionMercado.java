package com.formula11.observabilidad;

import java.util.Locale;

/**
 * Tipos del contador de operaciones de mercado. Compra y venta las cuenta el aspecto de métricas;
 * P2P lo registra la feature del mercado P2P llamando a {@link MetricasNegocio#registrarOperacionMercado}
 * (ver specs/016-actuator-prometheus/contracts/metricas-negocio.md).
 */
public enum TipoOperacionMercado {
    COMPRA, VENTA, P2P;

    /** Valor de la etiqueta {@code tipo} en la métrica. */
    public String etiqueta() {
        return name().toLowerCase(Locale.ROOT);
    }
}
