package com.formula11.service;

import com.formula11.adapter.PartidoWhoScoredResumen;
import com.formula11.adapter.WhoScoredPort;
import com.formula11.config.WhoScoredProperties;
import com.formula11.dto.*;
import com.formula11.model.EjecucionMetricaLiga;
import com.formula11.model.EjecucionMetricas;
import com.formula11.model.Liga;
import com.formula11.persistence.EjecucionMetricaLigaRepository;
import com.formula11.persistence.EjecucionMetricasRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class CargaMetricasService {
    private static final Logger log = LoggerFactory.getLogger(CargaMetricasService.class);
    private final WhoScoredPort port;
    private final WhoScoredProperties properties;
    private final CargaPartidoService cargaPartidoService;
    private final ObjectProvider<CargaMetricasService> self;
    private final EjecucionMetricasRepository ejecucionRepository;
    private final EjecucionMetricaLigaRepository ligaRepository;
    private final Clock clock;
    private final AtomicBoolean enCurso = new AtomicBoolean();

    public CargaMetricasService(WhoScoredPort port, WhoScoredProperties properties,
                                CargaPartidoService cargaPartidoService,
                                EjecucionMetricasRepository ejecucionRepository,
                                EjecucionMetricaLigaRepository ligaRepository, Clock clock,
                                ObjectProvider<CargaMetricasService> self) {
        this.port = port;
        this.properties = properties;
        this.cargaPartidoService = cargaPartidoService;
        this.ejecucionRepository = ejecucionRepository;
        this.ligaRepository = ligaRepository;
        this.clock = clock;
        this.self = self;
    }

    public EjecucionMetricas iniciar(LocalDate desde) {
        if (!enCurso.compareAndSet(false, true)) {
            throw new IllegalStateException("Ya hay una carga de métricas en curso");
        }
        String correlationId = correlationId();
        EjecucionMetricas ejecucion = ejecucionRepository.save(
                new EjecucionMetricas("EN_CURSO", Instant.now(clock), correlationId));
        if (desde == null) {
            desde = properties.getInicioTemporada();
        }
        self.getObject().ejecutarAsync(ejecucion.getId(), desde, correlationId);
        return ejecucion;
    }

    public void actualizarIncremental() {
        if (enCurso.get()) {
            log.info("Carga incremental omitida porque ya hay una ejecución en curso");
            return;
        }
        iniciar(properties.getInicioTemporada());
    }

    @Async("metricasExecutor")
    public void ejecutarAsync(Long ejecucionId, LocalDate desde, String correlationId) {
        String anterior = MDC.get("correlationId");
        MDC.put("correlationId", correlationId);
        try {
            ejecutar(ejecucionId, desde);
        } finally {
            if (anterior == null) MDC.remove("correlationId"); else MDC.put("correlationId", anterior);
            liberarLock();
        }
    }

    void ejecutar(Long ejecucionId, LocalDate desde) {
        EjecucionMetricas ejecucion = ejecucionRepository.findById(ejecucionId)
                .orElseThrow(() -> new IllegalStateException("Ejecución inexistente"));
        Map<Liga, EjecucionMetricaLiga> resumenes = new EnumMap<>(Liga.class);
        try {
            YearMonth inicio = YearMonth.from(desde);
            YearMonth fin = YearMonth.from(LocalDate.now(clock));
            for (Liga liga : Liga.values()) {
                EjecucionMetricaLiga resumen = ligaRepository.save(new EjecucionMetricaLiga(ejecucion, liga));
                resumenes.put(liga, resumen);
                for (YearMonth mes = inicio; !mes.isAfter(fin); mes = mes.plusMonths(1)) {
                    for (PartidoWhoScoredResumen partido : port.listarPartidos(liga, mes)) {
                        resumen.sumarEncontrados();
                        if (!"FT".equalsIgnoreCase(partido.elapsed())
                                || partido.startTime().toLocalDate().isBefore(desde)) continue;
                        try {
                            var datos = port.obtenerDatos(partido.idExterno());
                            var resultado = cargaPartidoService.prepararYGuardar(partido, datos);
                            if (resultado.cargado()) resumen.sumarCargados();
                            for (int i = 0; i < resultado.vinculados(); i++) resumen.sumarVinculados();
                            for (int i = 0; i < resultado.pendientes(); i++) resumen.sumarPendientes();
                        } catch (RuntimeException exception) {
                            resumen.sumarFallidos();
                            log.warn("Falló la carga del partido {} de {}", partido.idExterno(), liga, exception);
                        }

                    }
                }
                ligaRepository.save(resumen);
            }
            ejecucion.completar("COMPLETADA", Instant.now(clock));
        } catch (RuntimeException exception) {
            ejecucion.fallar(Instant.now(clock), exception.getMessage());
            throw exception;
        } finally {
            ejecucionRepository.save(ejecucion);
        }
    }

    void liberarLock() { enCurso.set(false); }

    public void marcarEnCursoComoFallidas() {
        for (EjecucionMetricas ejecucion : ejecucionRepository.findByEstado("EN_CURSO")) {
            ejecucion.fallar(Instant.now(clock), "La aplicación se reinició durante la carga");
            ejecucionRepository.save(ejecucion);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recuperarEjecucionesInterrumpidas() {
        marcarEnCursoComoFallidas();
    }

    public boolean estaEnCurso() { return enCurso.get(); }

    public EjecucionMetricas obtener(Long id) {
        EjecucionMetricas ejecucion = ejecucionRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("Ejecución no encontrada"));
        return ejecucion;
    }

    public PendientesPageResponse pendientes(Pageable pageable,
                                             com.formula11.persistence.PendienteVinculacionRepository repository) {
        Page<com.formula11.model.PendienteVinculacion> page = repository.findByResueltoFalse(pageable);
        return new PendientesPageResponse(page.getContent().stream()
                .map(item -> new com.formula11.dto.PendienteVinculacion(item.getWhoscoredId(),
                        item.getNombre(), item.getEquipo(), item.getLiga(), item.getFechaRegistro()))
                .toList(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    public EjecucionMetricasResponse response(EjecucionMetricas ejecucion,
                                               java.util.List<EjecucionMetricaLiga> ligas) {
        return new EjecucionMetricasResponse(ejecucion.getId(), ejecucion.getEstado(),
                ejecucion.getInicio(), ejecucion.getFin(), ligas.stream()
                .map(item -> new LigaEjecucionResponse(item.getLiga(),
                        item.getPartidosFallidos() > 0 ? "FALLIDA" : ejecucion.getEstado(),
                        item.getPartidosCargados(), item.getPartidosEncontrados()
                                - item.getPartidosCargados(), item.getJugadoresVinculados(),
                        item.getPendientes(), null)).toList(), ejecucion.getCorrelationId());
    }

    private static String correlationId() {
        String actual = MDC.get("correlationId");
        return actual == null || actual.isBlank() ? UUID.randomUUID().toString() : actual;
    }
}
