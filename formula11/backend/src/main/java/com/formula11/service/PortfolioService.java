package com.formula11.service;

import com.formula11.exception.AccesoPortfolioDenegadoException;
import com.formula11.exception.RecursoNoEncontradoException;
import com.formula11.model.Cuenta;
import com.formula11.model.Jugador;
import com.formula11.model.Tenencia;
import com.formula11.model.Usuario;
import com.formula11.persistence.CuentaRepository;
import com.formula11.persistence.TenenciaRepository;
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
    private final CotizacionVigentePort cotizacionPort;
    private final Clock clock;

    public PortfolioService(UsuarioRepository usuarioRepository,
                             CuentaRepository cuentaRepository,
                             TenenciaRepository tenenciaRepository,
                             CotizacionVigentePort cotizacionPort,
                             Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.cuentaRepository = cuentaRepository;
        this.tenenciaRepository = tenenciaRepository;
        this.cotizacionPort = cotizacionPort;
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
