package com.formula11.observabilidad;

import com.formula11.model.metricas.EjecucionMetricas;
import com.formula11.service.metricas.CargaMetricasService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Componente {@code cargaMetricas} del health: {@code DOWN} si la última carga de métricas de WhoScored
 * terminó fallida, porque el recálculo de la semana usaría datos viejos. Una carga parcial no lo baja.
 */
@Component
public class CargaMetricasHealthIndicator implements HealthIndicator {
    private static final String FALLIDA = "FALLIDA";

    private final CargaMetricasService cargaMetricas;

    public CargaMetricasHealthIndicator(CargaMetricasService cargaMetricas) {
        this.cargaMetricas = cargaMetricas;
    }

    @Override
    public Health health() {
        Optional<EjecucionMetricas> ultima = cargaMetricas.ultimaTerminada();
        if (ultima.isEmpty()) {
            return Health.up().withDetail("detalle", "Sin cargas de métricas").build();
        }
        EjecucionMetricas ejecucion = ultima.get();
        boolean fallida = FALLIDA.equals(ejecucion.getEstado());
        Health.Builder builder = fallida ? Health.down() : Health.up();
        builder.withDetail("estado", ejecucion.getEstado());
        detalleSiHay(builder, "fin", ejecucion.getFin());
        detalleSiHay(builder, "ejecucionId", ejecucion.getId());
        if (fallida) {
            detalleSiHay(builder, "error", ejecucion.getError());
        }
        return builder.build();
    }

    /** {@link Health.Builder#withDetail} no acepta valores nulos. */
    private static void detalleSiHay(Health.Builder builder, String clave, Object valor) {
        if (valor != null) {
            builder.withDetail(clave, valor);
        }
    }
}
