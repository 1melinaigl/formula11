package com.formula11.service;

import com.formula11.config.SenalesProperties;
import com.formula11.model.Cotizacion;
import com.formula11.model.Estrategia;
import com.formula11.model.Jugador;
import com.formula11.model.Senal;
import com.formula11.model.senales.DetectorSenales;
import com.formula11.model.senales.MedidorAciertos;
import com.formula11.persistence.CotizacionRepository;
import com.formula11.persistence.EstrategiaRepository;
import com.formula11.persistence.MetricaPartidoRepository;
import com.formula11.persistence.SenalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Señales de jugadores subvaluados (feature 010): generación después de cada semana recalculada,
 * listado para las alertas del frontend y panel de aciertos.
 */
@Service
public class SenalService implements GeneradorSenalesPort {
    private static final Logger log = LoggerFactory.getLogger(SenalService.class);
    private static final List<String> CODIGOS_ESTRATEGIA = List.of("RENDIMIENTO_POSICION", "CONSISTENCIA");
    private static final int MAX_SIZE = 100;
    private static final Sort ORDEN = Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("brecha"), Sort.Order.asc("id"));

    private final SenalRepository senales;
    private final CotizacionRepository cotizaciones;
    private final MetricaPartidoRepository metricas;
    private final EstrategiaRepository estrategias;
    private final SenalesProperties properties;
    private final Clock clock;

    public SenalService(SenalRepository senales, CotizacionRepository cotizaciones, MetricaPartidoRepository metricas,
                        EstrategiaRepository estrategias, SenalesProperties properties, Clock clock) {
        this.senales = senales;
        this.cotizaciones = cotizaciones;
        this.metricas = metricas;
        this.estrategias = estrategias;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void generar(LocalDate domingo) {
        Optional<String> activa = estrategias.findByActivaTrue().map(Estrategia::getCodigo);
        if (activa.isEmpty()) {
            log.info("Sin estrategia activa: no se generan señales para la semana {}", domingo);
            return;
        }
        String codigo = activa.get();
        List<Cotizacion> semana = cotizaciones.findByEstrategiaAndFecha(codigo, domingo);
        Map<Long, BigDecimal> preciosOtra = precios(otraEstrategia(codigo), domingo);
        Set<Long> jugaron = new HashSet<>(metricas.findJugadoresQueJugaron(domingo.minusDays(6), domingo));
        Map<Long, Jugador> jugadores = new HashMap<>();
        List<DetectorSenales.Candidato> candidatos = new ArrayList<>();
        for (Cotizacion cotizacion : semana) {
            Jugador jugador = cotizacion.getJugador();
            if (jugaron.contains(jugador.getId())) {
                jugadores.put(jugador.getId(), jugador);
                candidatos.add(new DetectorSenales.Candidato(jugador.getId(), jugador.getPosicion(),
                        cotizacion.getPrecio(), cotizacion.getScore(), preciosOtra.get(jugador.getId())));
            }
        }
        List<DetectorSenales.Detectada> detectadas = DetectorSenales.detectar(candidatos, parametros());
        guardar(domingo, codigo, detectadas, jugadores);
    }

    private void guardar(LocalDate domingo, String codigo, List<DetectorSenales.Detectada> detectadas,
                         Map<Long, Jugador> jugadores) {
        Map<String, Senal> existentes = new HashMap<>();
        for (Senal senal : senales.findByFecha(domingo)) {
            existentes.put(clave(senal.getJugador().getId(), senal.getTipo()), senal);
        }
        List<Senal> aGuardar = new ArrayList<>();
        int entreEstrategias = 0;
        for (DetectorSenales.Detectada detectada : detectadas) {
            Senal existente = existentes.remove(clave(detectada.jugadorId(), detectada.tipo()));
            if (existente != null) {
                existente.actualizar(codigo, detectada.brecha(), detectada.precio(), detectada.precioEstimado());
                aGuardar.add(existente);
            } else {
                aGuardar.add(new Senal(jugadores.get(detectada.jugadorId()), domingo, detectada.tipo(), codigo,
                        detectada.brecha(), detectada.precio(), detectada.precioEstimado(), clock.instant()));
            }
            if (detectada.tipo() == Senal.Tipo.ENTRE_ESTRATEGIAS) {
                entreEstrategias++;
            }
        }
        senales.deleteAll(existentes.values());
        senales.saveAll(aGuardar);
        log.info("Señales generadas: semana={} estrategia={} entreEstrategias={} precioAtrasado={} borradas={}",
                domingo, codigo, entreEstrategias, detectadas.size() - entreEstrategias, existentes.size());
    }

    @Transactional(readOnly = true)
    public PaginaSenales listar(boolean vigentes, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("El parámetro page no puede ser negativo");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("El parámetro size debe estar entre 1 y 100");
        }
        Pageable pageable = PageRequest.of(page, size, ORDEN);
        Page<Senal> resultado;
        if (vigentes) {
            Optional<LocalDate> ultima = senales.findMaxFecha();
            if (ultima.isEmpty()) {
                return new PaginaSenales(List.of(), page, size, 0, 0);
            }
            resultado = senales.findByFecha(ultima.get(), pageable);
        } else {
            resultado = senales.findAllBy(pageable);
        }
        return new PaginaSenales(resultado.getContent().stream().map(SenalLeida::de).toList(), page, size,
                resultado.getTotalElements(), resultado.getTotalPages());
    }

    @Transactional(readOnly = true)
    public MedidorAciertos.Resultado aciertos() {
        Map<LocalDate, List<Senal>> porFecha = new TreeMap<>();
        for (Senal senal : senales.findAll()) {
            porFecha.computeIfAbsent(senal.getFecha(), fecha -> new ArrayList<>()).add(senal);
        }
        List<MedidorAciertos.SemanaAMedir> semanas = new ArrayList<>();
        for (Map.Entry<LocalDate, List<Senal>> entrada : porFecha.entrySet()) {
            LocalDate fecha = entrada.getKey();
            String estrategia = entrada.getValue().get(0).getEstrategia();
            Map<Long, BigDecimal> siguiente = precios(estrategia, fecha.plusWeeks(1));
            if (siguiente.isEmpty()) {
                continue;
            }
            List<Long> senalados = entrada.getValue().stream().map(senal -> senal.getJugador().getId()).toList();
            semanas.add(new MedidorAciertos.SemanaAMedir(fecha, senalados, precios(estrategia, fecha), siguiente));
        }
        return MedidorAciertos.medir(semanas);
    }

    private Map<Long, BigDecimal> precios(String estrategia, LocalDate fecha) {
        Map<Long, BigDecimal> precios = new HashMap<>();
        for (Object[] fila : cotizaciones.findPreciosByEstrategiaAndFecha(estrategia, fecha)) {
            precios.put((Long) fila[0], (BigDecimal) fila[1]);
        }
        return precios;
    }

    private DetectorSenales.Parametros parametros() {
        return new DetectorSenales.Parametros(properties.umbral(), properties.ventanaScore(),
                properties.minimoPares(), properties.maximoPorTipo());
    }

    private static String otraEstrategia(String activa) {
        return CODIGOS_ESTRATEGIA.stream().filter(codigo -> !codigo.equals(activa)).findFirst().orElse(activa);
    }

    private static String clave(Long jugadorId, Senal.Tipo tipo) {
        return jugadorId + ":" + tipo;
    }
}
