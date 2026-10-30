package com.formula11.service;

import com.formula11.adapter.PartidoWhoScoredResumen;
import com.formula11.adapter.WhoScoredPort;
import com.formula11.config.ClockConfig;
import com.formula11.config.WhoScoredProperties;
import com.formula11.dto.EjecucionMetricasResponse;
import com.formula11.dto.LigaEjecucionResponse;
import com.formula11.dto.PendienteVinculacion;
import com.formula11.dto.PendientesPageResponse;
import com.formula11.model.EjecucionMetricaLiga;
import com.formula11.model.EjecucionMetricas;
import com.formula11.model.Liga;
import com.formula11.persistence.EjecucionMetricaLigaRepository;
import com.formula11.persistence.EjecucionMetricasRepository;
import com.formula11.persistence.PendienteVinculacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Orquesta la carga de métricas de WhoScored: recorre ligas y meses, saltea los partidos no terminados
 * o ya cargados (sin volver a descargarlos), y guarda cada partido en su propia transacción.
 * Una sola carga a la vez: la manual (asincrónica) y la del job semanal (sincrónica) comparten el lock.
 */
@Service
public class CargaMetricasService {
    private static final Logger log = LoggerFactory.getLogger(CargaMetricasService.class);
    private static final String CORRELATION_ID = "correlationId";
    private static final String EN_CURSO = "EN_CURSO";
    private static final String COMPLETADA = "COMPLETADA";
    private static final String PARCIAL = "PARCIAL";
    private static final String FALLIDA = "FALLIDA";
    private static final String PARTIDO_TERMINADO = "FT";

    private final WhoScoredPort port;
    private final WhoScoredProperties properties;
    private final CargaPartidoService cargaPartidoService;
    private final EjecucionMetricasRepository ejecucionRepository;
    private final EjecucionMetricaLigaRepository ligaRepository;
    private final PendienteVinculacionRepository pendienteRepository;
    private final Clock clock;
    private final ObjectProvider<CargaMetricasService> self;
    private final AtomicBoolean enCurso = new AtomicBoolean();

    @SuppressWarnings("java:S107") // Las dependencias son las del servicio; agruparlas no aclara nada.
    public CargaMetricasService(WhoScoredPort port, WhoScoredProperties properties,
                                CargaPartidoService cargaPartidoService,
                                EjecucionMetricasRepository ejecucionRepository,
                                EjecucionMetricaLigaRepository ligaRepository,
                                PendienteVinculacionRepository pendienteRepository, Clock clock,
                                ObjectProvider<CargaMetricasService> self) {
        this.port = port;
        this.properties = properties;
        this.cargaPartidoService = cargaPartidoService;
        this.ejecucionRepository = ejecucionRepository;
        this.ligaRepository = ligaRepository;
        this.pendienteRepository = pendienteRepository;
        this.clock = clock;
        this.self = self;
    }

    /**
     * Carga manual asincrónica. Sin fecha, carga los partidos nuevos (mes anterior y actual).
     *
     * @throws IllegalStateException si ya hay una carga en curso (se responde 409)
     */
    public EjecucionMetricas iniciar(LocalDate desde) {
        if (!enCurso.compareAndSet(false, true)) {
            throw new IllegalStateException("Ya hay una carga de métricas en curso");
        }
        try {
            String correlationId = correlationId();
            EjecucionMetricas ejecucion = nuevaEjecucion(correlationId);
            self.getObject().ejecutarAsync(ejecucion.getId(), normalizarDesde(desde), correlationId);
            return ejecucion;
        } catch (RuntimeException exception) {
            liberarLock();
            throw exception;
        }
    }

    /**
     * Carga sincrónica. La usa el job semanal (a través de ActualizacionMetricasPort).
     * Si ya hay una carga en curso, no inicia otra y devuelve null.
     */
    public EjecucionMetricas cargarSincronico(LocalDate desde) {
        if (!enCurso.compareAndSet(false, true)) {
            log.info("Carga de métricas omitida porque ya hay otra en curso");
            return null;
        }
        try {
            EjecucionMetricas ejecucion = nuevaEjecucion(correlationId());
            return ejecutar(ejecucion.getId(), normalizarDesde(desde));
        } finally {
            liberarLock();
        }
    }

    /** Carga incremental: los partidos nuevos del mes anterior y del actual. */
    public void actualizarIncremental() {
        cargarSincronico(null);
    }

    @Async("metricasExecutor")
    public void ejecutarAsync(Long ejecucionId, LocalDate desde, String correlationId) {
        String anterior = MDC.get(CORRELATION_ID);
        MDC.put(CORRELATION_ID, correlationId);
        try {
            ejecutar(ejecucionId, desde);
        } catch (RuntimeException exception) {
            log.error("La carga de métricas {} falló", ejecucionId, exception);
        } finally {
            restaurarMdc(anterior);
            liberarLock();
        }
    }

    EjecucionMetricas ejecutar(Long ejecucionId, LocalDate desde) {
        EjecucionMetricas ejecucion = ejecucionRepository.findById(ejecucionId)
                .orElseThrow(() -> new IllegalStateException("Ejecución inexistente: " + ejecucionId));
        log.info("Inicio de carga de métricas {} desde {}", ejecucionId, desde);
        try {
            YearMonth hasta = YearMonth.now(clock.withZone(ClockConfig.ZONA_NEGOCIO));
            boolean completa = true;
            for (Liga liga : Liga.values()) {
                completa &= procesarLiga(ejecucion, liga, desde, hasta);
            }
            ejecucion.completar(completa ? COMPLETADA : PARCIAL, Instant.now(clock));
        } catch (RuntimeException exception) {
            ejecucion.fallar(Instant.now(clock), exception.getMessage());
            throw exception;
        } finally {
            ejecucionRepository.save(ejecucion);
            port.finalizarCarga();
            log.info("Fin de carga de métricas {} con estado {}", ejecucionId, ejecucion.getEstado());
        }
        return ejecucion;
    }

    private boolean procesarLiga(EjecucionMetricas ejecucion, Liga liga, LocalDate desde, YearMonth hasta) {
        EjecucionMetricaLiga resumen = ligaRepository.save(new EjecucionMetricaLiga(ejecucion, liga));
        boolean completa = true;
        try {
            for (YearMonth mes = YearMonth.from(desde); !mes.isAfter(hasta); mes = mes.plusMonths(1)) {
                for (PartidoWhoScoredResumen partido : port.listarPartidos(liga, mes)) {
                    completa &= procesarPartido(resumen, partido, desde);
                }
            }
        } catch (RuntimeException exception) {
            completa = false;
            resumen.sumarFallidos();
            log.warn("No se pudo listar los partidos de {}", liga, exception);
        }
        ligaRepository.save(resumen);
        log.info("Liga {}: encontrados={} cargados={} fallidos={} vinculados={} pendientes={}", liga,
                resumen.getPartidosEncontrados(), resumen.getPartidosCargados(), resumen.getPartidosFallidos(),
                resumen.getJugadoresVinculados(), resumen.getPendientes());
        return completa;
    }

    /** Devuelve false si el partido falló. Los no terminados y los ya cargados no se descargan. */
    private boolean procesarPartido(EjecucionMetricaLiga resumen, PartidoWhoScoredResumen partido,
                                    LocalDate desde) {
        resumen.sumarEncontrados();
        if (!debeCargarse(partido, desde)) {
            return true;
        }
        try {
            var resultado = cargaPartidoService.prepararYGuardar(partido, port.obtenerDatos(partido.idExterno()));
            if (resultado.cargado()) {
                resumen.sumarCargados();
            }
            resumen.sumarVinculados(resultado.vinculados());
            resumen.sumarPendientes(resultado.pendientes());
            return true;
        } catch (RuntimeException exception) {
            resumen.sumarFallidos();
            log.warn("Falló la carga del partido {} de {}", partido.idExterno(), partido.liga(), exception);
            return false;
        }
    }

    private boolean debeCargarse(PartidoWhoScoredResumen partido, LocalDate desde) {
        return PARTIDO_TERMINADO.equalsIgnoreCase(partido.elapsed())
                && !partido.startTime().toLocalDate().isBefore(desde)
                && !cargaPartidoService.yaCargado(partido.idExterno());
    }

    /** Sin fecha: incremental. Nunca antes del inicio de la temporada configurada. */
    LocalDate normalizarDesde(LocalDate desde) {
        LocalDate inicioTemporada = properties.getInicioTemporada();
        LocalDate efectiva = desde != null ? desde
                : YearMonth.now(clock.withZone(ClockConfig.ZONA_NEGOCIO)).minusMonths(1).atDay(1);
        return efectiva.isBefore(inicioTemporada) ? inicioTemporada : efectiva;
    }

    private EjecucionMetricas nuevaEjecucion(String correlationId) {
        return ejecucionRepository.save(new EjecucionMetricas(EN_CURSO, Instant.now(clock), correlationId));
    }

    void liberarLock() {
        enCurso.set(false);
    }

    public boolean estaEnCurso() {
        return enCurso.get();
    }

    public void marcarEnCursoComoFallidas() {
        for (EjecucionMetricas ejecucion : ejecucionRepository.findByEstado(EN_CURSO)) {
            ejecucion.fallar(Instant.now(clock), "La aplicación se reinició durante la carga");
            ejecucionRepository.save(ejecucion);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recuperarEjecucionesInterrumpidas() {
        marcarEnCursoComoFallidas();
    }

    public EjecucionMetricas obtener(Long id) {
        return ejecucionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Ejecución no encontrada"));
    }

    public PendientesPageResponse pendientes(Pageable pageable) {
        Page<com.formula11.model.PendienteVinculacion> page = pendienteRepository.findByResueltoFalse(pageable);
        List<PendienteVinculacion> content = page.getContent().stream()
                .map(item -> new PendienteVinculacion(item.getWhoscoredId(), item.getNombre(),
                        item.getEquipo(), item.getLiga().getNombreLegible(), item.getFechaRegistro()))
                .toList();
        return new PendientesPageResponse(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    public EjecucionMetricasResponse response(EjecucionMetricas ejecucion, List<EjecucionMetricaLiga> ligas) {
        List<LigaEjecucionResponse> resumenes = ligas.stream()
                .map(item -> new LigaEjecucionResponse(item.getLiga().getNombreLegible(), estadoLiga(ejecucion, item),
                        item.getPartidosCargados(), item.getPartidosEncontrados() - item.getPartidosCargados(),
                        item.getJugadoresVinculados(), item.getPendientes(), null))
                .toList();
        return new EjecucionMetricasResponse(ejecucion.getId(), ejecucion.getEstado(), ejecucion.getInicio(),
                ejecucion.getFin(), resumenes, ejecucion.getCorrelationId());
    }

    /** El contrato admite EN_CURSO, COMPLETADA o FALLIDA por liga. */
    private static String estadoLiga(EjecucionMetricas ejecucion, EjecucionMetricaLiga liga) {
        if (liga.getPartidosFallidos() > 0) {
            return FALLIDA;
        }
        return EN_CURSO.equals(ejecucion.getEstado()) ? EN_CURSO : COMPLETADA;
    }

    private static void restaurarMdc(String anterior) {
        if (anterior == null) {
            MDC.remove(CORRELATION_ID);
        } else {
            MDC.put(CORRELATION_ID, anterior);
        }
    }

    private static String correlationId() {
        String actual = MDC.get(CORRELATION_ID);
        return actual == null || actual.isBlank() ? UUID.randomUUID().toString() : actual;
    }
}
