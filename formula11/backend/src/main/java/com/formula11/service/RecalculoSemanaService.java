package com.formula11.service;

import com.formula11.model.*;
import com.formula11.model.valuacion.*;
import com.formula11.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class RecalculoSemanaService {
    private static final Logger log = LoggerFactory.getLogger(RecalculoSemanaService.class);
    private final JugadorRepository jugadores;
    private final MetricaPartidoRepository metricas;
    private final EstrategiaRepository estrategias;
    private final CotizacionRepository cotizaciones;

    public RecalculoSemanaService(JugadorRepository jugadores, MetricaPartidoRepository metricas,
                                  EstrategiaRepository estrategias, CotizacionRepository cotizaciones) {
        this.jugadores = jugadores;
        this.metricas = metricas;
        this.estrategias = estrategias;
        this.cotizaciones = cotizaciones;
    }

    @Transactional
    public SemanaResultado procesar(LocalDate domingo) {
        LocalDate lunes = domingo.minusDays(6);
        LocalDate inicioVentana = domingo.minusWeeks(5).minusDays(6);
        List<MetricaPartido> entradas = metricas.findByFechaPartidoBetween(inicioVentana, domingo);
        List<MetricaPartidoInput> todas = entradas.stream().map(this::entrada).toList();
        List<MetricaPartidoInput> semana = todas.stream()
                .filter(m -> !m.fecha().isBefore(lunes)).toList();
        Map<Long, List<MetricaPartidoInput>> porJugador = semana.stream()
                .collect(java.util.stream.Collectors.groupingBy(MetricaPartidoInput::jugadorId));
        List<Estrategia> configuraciones = estrategias.findAll();
        Map<String, ResultadoValuacion> resultados = new HashMap<>();
        for (Estrategia estrategia : configuraciones) {
            List<MetricaPartidoInput> entradasEstrategia = "CONSISTENCIA".equals(estrategia.getCodigo())
                    ? todas : semana;
            EstrategiaValuacion calculadora = "CONSISTENCIA".equals(estrategia.getCodigo())
                    ? new Consistencia() : new RendimientoPosicion();
            resultados.put(estrategia.getCodigo(), calculadora.calcular(entradasEstrategia, domingo,
                    parametros(estrategia)));
        }
        resultados.values().stream().flatMap(r -> r.filasSinMinutos().stream()).distinct()
                .forEach(id -> log.info("Fila de métrica excluida por falta de minutos: jugadorId={}", id));

        cotizaciones.deleteByFecha(domingo);
        int sinMetricas = 0;
        int cotizados = 0;
        for (Jugador jugador : jugadores.findAll()) {
            List<MetricaPartidoInput> datos = porJugador.getOrDefault(jugador.getId(), List.of());
            if (datos.isEmpty()) sinMetricas++;
            boolean jugo = datos.stream().anyMatch(MetricaPartidoInput::jugo);
            for (Estrategia estrategia : configuraciones) {
                ResultadoValuacion resultado = resultados.get(estrategia.getCodigo());
                BigDecimal score = BigDecimal.valueOf(resultado.scores()
                        .getOrDefault(jugador.getId(), 0d));
                BigDecimal anterior = cotizaciones.findPrevious(jugador.getId(), estrategia.getCodigo(), domingo)
                        .stream().findFirst().map(Cotizacion::getPrecio).orElse(null);
                ParametrosPrecio precio = precioParametros(estrategia);
                PrecioCalculado calculado = CalculadoraPrecio.calcular(anterior, score, precio, jugo);
                cotizaciones.save(new Cotizacion(jugador, domingo, calculado.precio(), score,
                        estrategia.getCodigo(), estrategia.getVersion(), estrategia.getPesos()));
                cotizados++;
            }
        }
        return new SemanaResultado(cotizados, sinMetricas);
    }

    private MetricaPartidoInput entrada(MetricaPartido m) {
        return new MetricaPartidoInput(m.getJugador().getId(), m.getJugador().getLiga().name(),
                m.getJugador().getPosicion(), m.getFechaPartido(), m.getMinutos(), m.getTitular(),
                m.getGoles(), m.getAsistencias(), m.getTiros(), m.getPasesClave(), m.getRegates(),
                m.getEntradas(), m.getIntercepciones(), m.getAmarillas(), m.getRojas(), m.getAtajadas(),
                m.getGolesRecibidos(), m.getVallaInvicta(), m.getRating());
    }

    private ParametrosEstrategia parametros(Estrategia estrategia) {
        Map<String, BigDecimal> pesos = new HashMap<>();
        estrategia.getPesos().forEach((posicion, valores) ->
                valores.forEach((metrica, peso) -> pesos.put(posicion + ":" + metrica, BigDecimal.valueOf(peso))));
        Double lambda = estrategia.getParametros().get("lambda");
        return new ParametrosEstrategia(pesos, lambda == null ? BigDecimal.ONE : BigDecimal.valueOf(lambda));
    }

    private ParametrosPrecio precioParametros(Estrategia estrategia) {
        Map<String, Double> p = estrategia.getParametros();
        return new ParametrosPrecio(decimal(p.get("k"), "0.4"), decimal(p.get("topeVariacion"), "0.20"),
                decimal(p.get("piso"), "0.10"));
    }

    private BigDecimal decimal(Double value, String fallback) {
        return value == null ? new BigDecimal(fallback) : BigDecimal.valueOf(value);
    }

    public record SemanaResultado(int jugadoresCotizados, int jugadoresSinMetricas) {}
}
