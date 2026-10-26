package com.formula11.service;

import com.formula11.exception.AccesoPortfolioDenegadoException;
import com.formula11.exception.RecursoNoEncontradoException;
import com.formula11.model.Cuenta;
import com.formula11.model.Jugador;
import com.formula11.model.Tenencia;
import com.formula11.model.Transaccion;
import com.formula11.model.Usuario;
import com.formula11.persistence.CuentaRepository;
import com.formula11.persistence.TenenciaRepository;
import com.formula11.persistence.TransaccionRepository;
import com.formula11.persistence.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PortfolioService {
    private static final int DINERO_SCALE = 4;
    private static final int PORCENTAJE_SCALE = 2;
    private static final RoundingMode REDONDEO = RoundingMode.HALF_UP;

    private final UsuarioRepository usuarioRepository;
    private final CuentaRepository cuentaRepository;
    private final TenenciaRepository tenenciaRepository;
    private final TransaccionRepository transaccionRepository;
    private final CotizacionVigentePort cotizacionPort;
    private final CotizacionHistoricaPort cotizacionHistoricaPort;
    private final Clock clock;

    public PortfolioService(UsuarioRepository usuarioRepository,
                             CuentaRepository cuentaRepository,
                             TenenciaRepository tenenciaRepository,
                             TransaccionRepository transaccionRepository,
                             CotizacionVigentePort cotizacionPort,
                             CotizacionHistoricaPort cotizacionHistoricaPort,
                             Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.cuentaRepository = cuentaRepository;
        this.tenenciaRepository = tenenciaRepository;
        this.transaccionRepository = transaccionRepository;
        this.cotizacionPort = cotizacionPort;
        this.cotizacionHistoricaPort = cotizacionHistoricaPort;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PortfolioResultado portfolio(Long solicitanteId, Long usuarioId) {
        Usuario usuario = verificarAcceso(solicitanteId, usuarioId);
        Cuenta cuenta = cuentaRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La cuenta del usuario no existe"));

        Instant momento = clock.instant();
        LocalDate fechaValuacion = LocalDate.ofInstant(momento, ZoneOffset.UTC);
        List<PosicionPortfolioResultado> posiciones = new ArrayList<>();
        BigDecimal invertido = ceroDinero();
        BigDecimal valorTenencias = ceroDinero();

        for (Tenencia tenencia : tenenciaRepository.findByUsuarioIdOrderByJugadorIdAsc(usuarioId)) {
            if (tenencia.getCantidad() <= 0) {
                continue;
            }
            Jugador jugador = tenencia.getJugador();
            CotizacionVigentePort.CotizacionVigente cotizacion =
                    cotizacionPort.vigente(jugador, momento);
            BigDecimal precioActual = dinero(cotizacion.precio());
            BigDecimal precioPromedio = dinero(tenencia.getPrecioPromedio());
            BigDecimal invertidoPosicion = dinero(precioPromedio.multiply(BigDecimal.valueOf(tenencia.getCantidad())));
            BigDecimal valorActual = dinero(precioActual.multiply(BigDecimal.valueOf(tenencia.getCantidad())));
            BigDecimal ganancia = dinero(valorActual.subtract(invertidoPosicion));
            BigDecimal gananciaPorcentaje = porcentaje(ganancia, invertidoPosicion);

            posiciones.add(new PosicionPortfolioResultado(
                    jugador, tenencia.getCantidad(), precioPromedio, precioActual,
                    invertidoPosicion, valorActual, ganancia, gananciaPorcentaje));
            invertido = dinero(invertido.add(invertidoPosicion));
            valorTenencias = dinero(valorTenencias.add(valorActual));
        }

        BigDecimal ganancia = dinero(valorTenencias.subtract(invertido));
        return new PortfolioResultado(
                usuario,
                fechaValuacion,
                dinero(cuenta.getSaldo()),
                invertido,
                valorTenencias,
                ganancia,
                porcentaje(ganancia, invertido),
                posiciones);
    }

    @Transactional(readOnly = true)
    public EvolucionPortfolioResultado evolucion(Long solicitanteId, Long usuarioId,
                                                  LocalDate desde, LocalDate hasta) {
        Usuario usuario = verificarAcceso(solicitanteId, usuarioId);
        LocalDate hoy = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
        LocalDate fechaHasta = hasta == null ? hoy : hasta;
        LocalDate fechaDesde = desde;
        if (fechaDesde == null) {
            fechaDesde = transaccionRepository.findFirstFechaByUsuarioId(usuarioId)
                    .map(fecha -> LocalDate.ofInstant(fecha, ZoneOffset.UTC))
                    .orElse(fechaHasta);
        }
        if (fechaDesde.isAfter(fechaHasta)) {
            throw new IllegalArgumentException("La fecha desde no puede ser posterior a hasta");
        }

        List<Transaccion> transacciones =
                transaccionRepository.findByUsuarioIdOrderByFechaAscIdAsc(usuarioId);
        List<LocalDate> fechas = cotizacionHistoricaPort.fechasCotizacion(fechaDesde, fechaHasta);
        Map<Long, TenenciaHistorica> tenencias = new LinkedHashMap<>();
        List<PuntoEvolucionResultado> puntos = new ArrayList<>();
        int indiceTransaccion = 0;

        for (LocalDate fecha : fechas) {
            while (indiceTransaccion < transacciones.size()
                    && !fechaDe(transacciones.get(indiceTransaccion)).isAfter(fecha)) {
                aplicar(transacciones.get(indiceTransaccion), tenencias);
                indiceTransaccion++;
            }

            List<Long> jugadores = tenencias.values().stream()
                    .filter(tenencia -> tenencia.cantidad > 0)
                    .map(tenencia -> tenencia.jugador.getId())
                    .toList();
            Map<Long, BigDecimal> precios = cotizacionHistoricaPort.preciosA(jugadores, fecha);
            BigDecimal invertido = ceroDinero();
            BigDecimal valor = ceroDinero();
            for (TenenciaHistorica tenencia : tenencias.values()) {
                if (tenencia.cantidad <= 0) {
                    continue;
                }
                BigDecimal cantidad = BigDecimal.valueOf(tenencia.cantidad);
                invertido = dinero(invertido.add(dinero(tenencia.precioPromedio.multiply(cantidad))));
                BigDecimal precio = precios.get(tenencia.jugador.getId());
                if (precio == null) {
                    throw new IllegalStateException(
                            "No hay cotización histórica para el jugador " + tenencia.jugador.getId());
                }
                valor = dinero(valor.add(dinero(precio.multiply(cantidad))));
            }
            puntos.add(new PuntoEvolucionResultado(fecha, dinero(valor), dinero(invertido)));
        }
        return new EvolucionPortfolioResultado(usuario, puntos);
    }

    private static LocalDate fechaDe(Transaccion transaccion) {
        return LocalDate.ofInstant(transaccion.getFecha(), ZoneOffset.UTC);
    }

    private static void aplicar(Transaccion transaccion, Map<Long, TenenciaHistorica> tenencias) {
        Long jugadorId = transaccion.getJugador().getId();
        TenenciaHistorica tenencia = tenencias.computeIfAbsent(
                jugadorId, ignored -> new TenenciaHistorica(transaccion.getJugador()));
        if (transaccion.getTipo() == Transaccion.Tipo.COMPRA) {
            BigDecimal cantidadAnterior = BigDecimal.valueOf(tenencia.cantidad);
            BigDecimal cantidadCompra = BigDecimal.valueOf(transaccion.getCantidad());
            int cantidadNueva = tenencia.cantidad + transaccion.getCantidad();
            tenencia.precioPromedio = tenencia.precioPromedio.multiply(cantidadAnterior)
                    .add(transaccion.getPrecioUnitario().multiply(cantidadCompra))
                    .divide(BigDecimal.valueOf(cantidadNueva), DINERO_SCALE, REDONDEO);
            tenencia.cantidad = cantidadNueva;
        } else {
            tenencia.cantidad -= transaccion.getCantidad();
            if (tenencia.cantidad < 0) {
                throw new IllegalStateException("La reconstrucción histórica produjo una tenencia negativa");
            }
        }
    }

    private static final class TenenciaHistorica {
        private final Jugador jugador;
        private int cantidad;
        private BigDecimal precioPromedio = ceroDinero();

        private TenenciaHistorica(Jugador jugador) {
            this.jugador = jugador;
        }
    }

    // Regla común a portfolio, evolución e historial. Se compara el id antes de buscar al usuario
    // pedido, para no revelar qué identificadores existen; el superusuario no tiene portfolio.
    private Usuario verificarAcceso(Long solicitanteId, Long usuarioId) {
        if (!usuarioId.equals(solicitanteId)) {
            throw new AccesoPortfolioDenegadoException();
        }
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe"));
        if (usuario.esSuperusuario()) {
            throw new AccesoPortfolioDenegadoException();
        }
        return usuario;
    }

    private static BigDecimal ceroDinero() {
        return BigDecimal.ZERO.setScale(DINERO_SCALE, REDONDEO);
    }

    private static BigDecimal dinero(BigDecimal valor) {
        if (valor == null) {
            throw new IllegalStateException("La cotización no tiene precio");
        }
        return valor.setScale(DINERO_SCALE, REDONDEO);
    }

    private static BigDecimal porcentaje(BigDecimal ganancia, BigDecimal invertido) {
        if (invertido.signum() == 0) {
            return BigDecimal.ZERO.setScale(PORCENTAJE_SCALE, REDONDEO);
        }
        return ganancia.divide(invertido, 8, REDONDEO)
                .multiply(BigDecimal.valueOf(100))
                .setScale(PORCENTAJE_SCALE, REDONDEO);
    }
}
