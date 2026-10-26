package com.formula11.service;

import com.formula11.dto.RecalculoRequest;
import org.slf4j.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

@Component
public class CotizacionScheduler {
    private static final Logger log = LoggerFactory.getLogger(CotizacionScheduler.class);
    private final ActualizacionMetricasPort actualizacion;
    private final RecalculoService recalculo;
    private final Clock clock;
    public CotizacionScheduler(ActualizacionMetricasPort actualizacion, RecalculoService recalculo, Clock clock) {
        this.actualizacion = actualizacion; this.recalculo = recalculo; this.clock = clock;
    }
    @Scheduled(cron = "${app.cotizacion.cron}", zone = "America/Argentina/Buenos_Aires")
    public void ejecutar() {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        try {
            try {
                actualizacion.actualizar();
                log.info("Actualización de métricas completada; se recalculará con datos locales");
            }
            catch (RuntimeException exception) { log.warn("No se pudieron actualizar las métricas; se usarán datos locales", exception); }
            recalculo.recalcular(null);
        } finally { MDC.remove("correlationId"); }
    }
}
