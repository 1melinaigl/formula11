package com.formula11.service;

import com.formula11.dto.*;
import com.formula11.config.ClockConfig;
import com.formula11.model.Estrategia;
import com.formula11.persistence.EstrategiaRepository;
import org.slf4j.*;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class RecalculoService {
    private static final Logger log = LoggerFactory.getLogger(RecalculoService.class);
    private final Clock clock;
    private final RecalculoSemanaService semanas;
    private final EstrategiaRepository estrategias;
    private final com.formula11.persistence.CotizacionRepository cotizaciones;
    private final CotizacionCacheInvalidator cache;
    private final AtomicBoolean ejecutando = new AtomicBoolean();

    public RecalculoService(Clock clock, RecalculoSemanaService semanas, EstrategiaRepository estrategias,
                            com.formula11.persistence.CotizacionRepository cotizaciones,
                            CotizacionCacheInvalidator cache) {
        this.clock = clock; this.semanas = semanas; this.estrategias = estrategias;
        this.cotizaciones = cotizaciones; this.cache = cache;
    }

    public RecalculoResponse recalcular(RecalculoRequest request) {
        if (!ejecutando.compareAndSet(false, true)) throw new IllegalStateException("Ya hay un recálculo en curso");
        if (request != null && !request.rangoValido()) {
            ejecutando.set(false);
            throw new IllegalArgumentException("Desde y hasta deben enviarse juntas y desde no puede ser posterior a hasta");
        }
        Instant inicio = clock.instant();
        try {
            LocalDate fin = request != null && request.hasta() != null ? request.hasta() : ultimaSemanaCerrada();
            LocalDate desde = request != null && request.desde() != null ? request.desde() : fin;
            if (request != null && request.desde() != null && request.hasta() != null) {
                LocalDate ultimaPersistida = cotizaciones.findMaxFecha().orElse(fin);
                if (ultimaPersistida.isAfter(fin)) fin = ultimaPersistida;
            }
            int weeks = 0, players = 0, noMetricas = 0;
            for (LocalDate domingo = domingoDeSemana(desde); !domingo.isAfter(fin); domingo = domingo.plusWeeks(1)) {
                RecalculoSemanaService.SemanaResultado result = semanas.procesar(domingo);
                weeks++; players += result.jugadoresCotizados(); noMetricas += result.jugadoresSinMetricas();
            }
            cache.invalidate();
            String correlationId = Optional.ofNullable(org.slf4j.MDC.get("correlationId")).orElse(UUID.randomUUID().toString());
            List<RecalculoResponse.EstrategiaVersionResponse> versions = Estrategia.vigentes(estrategias.findAll()).stream()
                    .map(e -> new RecalculoResponse.EstrategiaVersionResponse(e.getCodigo(), e.getVersion())).toList();
            return new RecalculoResponse("COMPLETADO", weeks, players, noMetricas, versions, inicio, clock.instant(), correlationId);
        } finally {
            ejecutando.set(false);
        }
    }

    private LocalDate ultimaSemanaCerrada() {
        return LocalDate.ofInstant(clock.instant(), ClockConfig.ZONA_NEGOCIO).minusDays(1)
                .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.SUNDAY));
    }
    private LocalDate domingoDeSemana(LocalDate fecha) { return fecha.with(java.time.DayOfWeek.SUNDAY); }
}
