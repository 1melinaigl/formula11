package com.formula11.service;

import com.formula11.config.ClockConfig;
import com.formula11.exception.RecursoNoEncontradoException;
import com.formula11.exception.SoloSuperusuarioException;
import com.formula11.model.Cotizacion;
import com.formula11.model.Estrategia;
import com.formula11.model.Jugador;
import com.formula11.model.Liga;
import com.formula11.persistence.CotizacionRepository;
import com.formula11.persistence.EstrategiaRepository;
import com.formula11.persistence.JugadorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Lecturas de cotizaciones de la feature 008: detalle de jugador, historial, cotización a una fecha y ranking.
 * Sin parámetro de estrategia usa la activa; otra estrategia solo la puede consultar el superusuario.
 */
@Service
@Transactional(readOnly = true)
public class CotizacionConsultaService {
    static final Set<String> ESTRATEGIAS = Set.of("RENDIMIENTO_POSICION", "CONSISTENCIA");
    static final Set<String> POSICIONES = Set.of("POR", "DEF", "MED", "DEL");
    static final String MENSAJE_SOLO_SUPERUSUARIO =
            "Solo el superusuario puede consultar una estrategia que no es la activa";
    private static final int MAX_SIZE = 100;
    private static final Map<String, Liga> LIGAS = Arrays.stream(Liga.values())
            .collect(Collectors.toUnmodifiableMap(
                    liga -> liga.getNombreLegible().toLowerCase(Locale.ROOT), Function.identity()));
    private static final Sort ORDEN_RANKING = Sort.by(
            Sort.Order.desc("score"),
            Sort.Order.desc("precio"),
            Sort.Order.asc("jugador.nombre"),
            Sort.Order.asc("jugador.id"));

    private final JugadorRepository jugadores;
    private final CotizacionRepository cotizaciones;
    private final EstrategiaRepository estrategias;
    private final Clock clock;

    public CotizacionConsultaService(JugadorRepository jugadores, CotizacionRepository cotizaciones,
                                     EstrategiaRepository estrategias, Clock clock) {
        this.jugadores = jugadores;
        this.cotizaciones = cotizaciones;
        this.estrategias = estrategias;
        this.clock = clock;
    }

    public JugadorDetalleResultado detalle(long jugadorId) {
        Jugador jugador = buscarJugador(jugadorId);
        CotizacionLeida actual = estrategiaActiva()
                .flatMap(codigo -> leerVigente(jugadorId, codigo, fechaNegocio()))
                .orElse(null);
        return new JugadorDetalleResultado(jugador, actual);
    }

    public HistorialCotizacionesResultado historial(long jugadorId, String estrategia, LocalDate desde,
                                                    LocalDate hasta, boolean superusuario) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha desde no puede ser posterior a hasta");
        }
        String codigo = resolverEstrategia(estrategia, superusuario);
        buscarJugador(jugadorId);
        if (codigo == null) {
            return new HistorialCotizacionesResultado(jugadorId, null, List.of());
        }
        // La serie completa de una temporada tiene menos de 52 filas: se calcula cada variación contra
        // la cotización anterior de la serie y recién después se filtra el rango pedido.
        List<CotizacionLeida> resultado = new ArrayList<>();
        BigDecimal precioAnterior = null;
        for (Cotizacion cotizacion : cotizaciones.findByJugadorIdAndEstrategiaOrderByFechaAsc(jugadorId, codigo)) {
            if (enRango(cotizacion.getFecha(), desde, hasta)) {
                resultado.add(CotizacionLeida.de(cotizacion, precioAnterior));
            }
            precioAnterior = cotizacion.getPrecio();
        }
        return new HistorialCotizacionesResultado(jugadorId, codigo, resultado);
    }

    public CotizacionLeida vigente(long jugadorId, String estrategia, LocalDate fecha, boolean superusuario) {
        String codigo = resolverEstrategia(estrategia, superusuario);
        buscarJugador(jugadorId);
        LocalDate fechaConsulta = fecha != null ? fecha : fechaNegocio();
        return Optional.ofNullable(codigo)
                .flatMap(valor -> leerVigente(jugadorId, valor, fechaConsulta))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El jugador no tiene cotizaciones hasta la fecha " + fechaConsulta));
    }

    public RankingResultado ranking(String estrategia, String liga, String posicion, int page, int size,
                                    boolean superusuario) {
        validarPaginacion(page, size);
        Liga ligaFiltro = parsearLiga(liga);
        String posicionFiltro = parsearPosicion(posicion);
        String codigo = resolverEstrategia(estrategia, superusuario);
        LocalDate fecha = codigo == null ? null : cotizaciones.findMaxFechaByEstrategia(codigo).orElse(null);
        if (fecha == null) {
            return RankingResultado.vacio(codigo, page, size);
        }
        Page<Cotizacion> pagina = cotizaciones.findAll(
                especificacionRanking(codigo, fecha, ligaFiltro, posicionFiltro),
                PageRequest.of(page, size, ORDEN_RANKING));
        Map<Long, BigDecimal> preciosAnteriores = preciosAnteriores(codigo, fecha, pagina.getContent());
        Integer version = cotizaciones.findVersionByEstrategiaAndFecha(codigo, fecha).orElse(null);
        List<RankingResultado.Item> items = new ArrayList<>();
        int primerPuesto = page * size + 1;
        for (int indice = 0; indice < pagina.getContent().size(); indice++) {
            Cotizacion cotizacion = pagina.getContent().get(indice);
            Jugador jugador = cotizacion.getJugador();
            items.add(new RankingResultado.Item(primerPuesto + indice, jugador,
                    CotizacionLeida.de(cotizacion, preciosAnteriores.get(jugador.getId()))));
        }
        return new RankingResultado(codigo, version, fecha, page, size,
                pagina.getTotalElements(), pagina.getTotalPages(), items);
    }

    String resolverEstrategia(String pedida, boolean superusuario) {
        String activa = estrategiaActiva().orElse(null);
        if (pedida == null) {
            return activa;
        }
        String codigo = pedida.trim();
        if (!ESTRATEGIAS.contains(codigo)) {
            throw new IllegalArgumentException("La estrategia no es válida");
        }
        if (!superusuario && !codigo.equals(activa)) {
            throw new SoloSuperusuarioException(MENSAJE_SOLO_SUPERUSUARIO);
        }
        return codigo;
    }

    private Optional<String> estrategiaActiva() {
        return estrategias.findByActivaTrue().map(Estrategia::getCodigo);
    }

    private Optional<CotizacionLeida> leerVigente(long jugadorId, String codigo, LocalDate fecha) {
        return cotizaciones.findTopByJugadorIdAndEstrategiaAndFechaLessThanEqualOrderByFechaDesc(jugadorId, codigo, fecha)
                .map(actual -> CotizacionLeida.de(actual, cotizaciones
                        .findTopByJugadorIdAndEstrategiaAndFechaLessThanEqualOrderByFechaDesc(
                                jugadorId, codigo, actual.getFecha().minusDays(1))
                        .map(Cotizacion::getPrecio)
                        .orElse(null)));
    }

    private Map<Long, BigDecimal> preciosAnteriores(String codigo, LocalDate fecha, List<Cotizacion> pagina) {
        if (pagina.isEmpty()) {
            return Map.of();
        }
        Optional<LocalDate> fechaAnterior = cotizaciones.findFechaAnterior(codigo, fecha);
        if (fechaAnterior.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = pagina.stream().map(cotizacion -> cotizacion.getJugador().getId()).toList();
        Map<Long, BigDecimal> precios = new HashMap<>();
        for (Cotizacion anterior : cotizaciones.findByEstrategiaAndFechaAndJugadorIdIn(codigo, fechaAnterior.get(), ids)) {
            precios.put(anterior.getJugador().getId(), anterior.getPrecio());
        }
        return precios;
    }

    private static Specification<Cotizacion> especificacionRanking(String codigo, LocalDate fecha, Liga liga,
                                                                    String posicion) {
        Specification<Cotizacion> especificacion = (root, query, builder) -> builder.and(
                builder.equal(root.get("estrategia"), codigo),
                builder.equal(root.get("fecha"), fecha));
        if (liga != null) {
            especificacion = especificacion.and((root, query, builder) ->
                    builder.equal(root.get("jugador").get("liga"), liga));
        }
        if (posicion != null) {
            especificacion = especificacion.and((root, query, builder) ->
                    builder.equal(root.get("jugador").get("posicion"), posicion));
        }
        return especificacion;
    }

    private Jugador buscarJugador(long jugadorId) {
        return jugadores.findById(jugadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El jugador no existe"));
    }

    private LocalDate fechaNegocio() {
        return LocalDate.ofInstant(clock.instant(), ClockConfig.ZONA_NEGOCIO);
    }

    private static boolean enRango(LocalDate fecha, LocalDate desde, LocalDate hasta) {
        return (desde == null || !fecha.isBefore(desde)) && (hasta == null || !fecha.isAfter(hasta));
    }

    private static void validarPaginacion(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("El parámetro page no puede ser negativo");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("El parámetro size debe estar entre 1 y 100");
        }
    }

    private static Liga parsearLiga(String valor) {
        if (valor == null) {
            return null;
        }
        Liga liga = LIGAS.get(valor.trim().toLowerCase(Locale.ROOT));
        if (liga == null) {
            throw new IllegalArgumentException("El filtro liga no es válido");
        }
        return liga;
    }

    private static String parsearPosicion(String valor) {
        if (valor == null) {
            return null;
        }
        String posicion = valor.trim().toUpperCase(Locale.ROOT);
        if (!POSICIONES.contains(posicion)) {
            throw new IllegalArgumentException("El filtro posicion no es válido");
        }
        return posicion;
    }
}
