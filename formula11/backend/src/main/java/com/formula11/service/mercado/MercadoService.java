package com.formula11.service.mercado;

import com.formula11.exception.ConflictoMercadoException;
import com.formula11.exception.OperacionNoValidaException;
import com.formula11.exception.RecursoNoEncontradoException;
import com.formula11.exception.SuperusuarioNoOperaException;
import com.formula11.service.cotizaciones.CotizacionVigentePort;
import com.formula11.model.*;
import com.formula11.config.CorrelationIdFilter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import com.formula11.persistence.jugadores.JugadorRepository;
import com.formula11.persistence.mercado.AuditoriaMercadoRepository;
import com.formula11.persistence.mercado.CuentaRepository;
import com.formula11.persistence.mercado.TenenciaRepository;
import com.formula11.persistence.mercado.TransaccionRepository;
import com.formula11.persistence.usuarios.UsuarioRepository;

@Service
public class MercadoService {
    private static final Logger log = LoggerFactory.getLogger(MercadoService.class);
    private static final String JUGADOR_NO_EXISTE = "El jugador no existe";
    private static final String SUPERUSUARIO_NO_EXISTE = "El superusuario no existe";
    private static final int CANTIDAD_MAXIMA = 100;
    private static final int TOKENS_EMITIDOS = 100;
    private final UsuarioRepository usuarioRepository;
    private final JugadorRepository jugadorRepository;
    private final CuentaRepository cuentaRepository;
    private final TenenciaRepository tenenciaRepository;
    private final TransaccionRepository transaccionRepository;
    private final AuditoriaMercadoRepository auditoriaRepository;
    private final CotizacionVigentePort cotizacionPort;
    private final Clock clock;

    public MercadoService(UsuarioRepository usuarioRepository, JugadorRepository jugadorRepository,
                          CuentaRepository cuentaRepository, TenenciaRepository tenenciaRepository,
                          TransaccionRepository transaccionRepository,
                          AuditoriaMercadoRepository auditoriaRepository,
                          CotizacionVigentePort cotizacionPort, Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.jugadorRepository = jugadorRepository;
        this.cuentaRepository = cuentaRepository;
        this.tenenciaRepository = tenenciaRepository;
        this.transaccionRepository = transaccionRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.cotizacionPort = cotizacionPort;
        this.clock = clock;
    }

    @Transactional
    public ResultadoOperacion comprar(Long usuarioId, Long jugadorId, int cantidad) {
        return ejecutarCompra(usuarioId, jugadorId, cantidad, clock);
    }

    @Transactional
    public ResultadoOperacion comprar(Long usuarioId, Long jugadorId, int cantidad, Clock reloj) {
        return ejecutarCompra(usuarioId, jugadorId, cantidad, reloj);
    }

    @Transactional
    public ResultadoOperacion vender(Long usuarioId, Long jugadorId, int cantidad) {
        Operacion operacion = prepararOperacion(usuarioId, jugadorId, cantidad, clock);
        Tenencia tenencia = tenenciaRepository.findByUsuarioIdAndJugadorIdForUpdate(usuarioId, jugadorId)
                .orElseThrow(() -> new OperacionNoValidaException("El usuario no tiene tokens de ese jugador"));
        if (tenencia.getCantidad() < cantidad) {
            throw new OperacionNoValidaException("El usuario no tiene esa cantidad de tokens del jugador");
        }

        Tenencia stock = operacion.stock();
        String antes = snapshot(operacion.cuenta(), tenencia, stock);
        tenencia.vender(cantidad);
        stock.comprar(cantidad, stock.getPrecioPromedio());
        operacion.cuenta().acreditar(operacion.total());
        tenenciaRepository.save(tenencia);
        tenenciaRepository.save(stock);
        return confirmar(Transaccion.Tipo.VENTA, operacion, tenencia, antes);
    }

    private ResultadoOperacion ejecutarCompra(Long usuarioId, Long jugadorId, int cantidad, Clock reloj) {
        Operacion operacion = prepararOperacion(usuarioId, jugadorId, cantidad, reloj);
        BigDecimal precio = operacion.cotizacion().precio();
        Tenencia tenencia = tenenciaRepository.findByUsuarioIdAndJugadorIdForUpdate(usuarioId, jugadorId)
                .orElseGet(() -> new Tenencia(operacion.usuario(), operacion.jugador(), 0, precio));
        if (operacion.cuenta().getSaldo().compareTo(operacion.total()) < 0) {
            throw new OperacionNoValidaException("El usuario no tiene saldo suficiente");
        }
        if (operacion.stock().getCantidad() < cantidad) {
            throw new ConflictoMercadoException("El superusuario no tiene tokens suficientes de ese jugador");
        }

        String antes = snapshot(operacion.cuenta(), tenencia, operacion.stock());
        operacion.cuenta().debitar(operacion.total());
        operacion.stock().vender(cantidad);
        tenencia.comprar(cantidad, precio);
        tenenciaRepository.save(tenencia);
        return confirmar(Transaccion.Tipo.COMPRA, operacion, tenencia, antes);
    }

    /**
     * Validaciones y lecturas comunes a compra y venta. Bloquea primero la cuenta del usuario y después el stock del
     * superusuario; la tenencia del usuario se bloquea después, en cada operación.
     */
    private Operacion prepararOperacion(Long usuarioId, Long jugadorId, int cantidad, Clock reloj) {
        if (cantidad < 1 || cantidad > CANTIDAD_MAXIMA) {
            throw new IllegalArgumentException("La cantidad debe estar entre 1 y " + CANTIDAD_MAXIMA);
        }
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe"));
        if (usuario.esSuperusuario()) throw new SuperusuarioNoOperaException();
        Jugador jugador = jugadorRepository.findById(jugadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException(JUGADOR_NO_EXISTE));
        Usuario superusuario = usuarioRepository.findByRol(Rol.SUPERUSUARIO)
                .orElseThrow(() -> new RecursoNoEncontradoException(SUPERUSUARIO_NO_EXISTE));
        Instant fecha = Instant.now(reloj);
        CotizacionVigentePort.CotizacionVigente cotizacion = cotizacionPort.vigente(jugador, fecha);
        BigDecimal total = cotizacion.precio().multiply(BigDecimal.valueOf(cantidad)).setScale(4, RoundingMode.HALF_UP);

        Cuenta cuenta = cuentaRepository.findByUsuarioIdForUpdate(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La cuenta del usuario no existe"));
        Tenencia stock = tenenciaRepository.findByUsuarioIdAndJugadorIdForUpdate(superusuario.getId(), jugadorId)
                .orElseThrow(() -> new ConflictoMercadoException("El superusuario no tiene tokens de ese jugador"));
        return new Operacion(usuario, jugador, superusuario, cantidad, fecha, cotizacion, total, cuenta, stock);
    }

    /** Registra la transacción y su auditoría con el estado anterior y el posterior a la operación. */
    private ResultadoOperacion confirmar(Transaccion.Tipo tipo, Operacion operacion, Tenencia tenencia, String antes) {
        CotizacionVigentePort.CotizacionVigente cotizacion = operacion.cotizacion();
        Transaccion transaccion = transaccionRepository.save(new Transaccion(
                tipo, operacion.usuario(), operacion.jugador(), operacion.cantidad(), cotizacion.precio(),
                operacion.superusuario(), operacion.fecha(), cotizacion.estrategia(), cotizacion.version()));
        String despues = snapshot(operacion.cuenta(), tenencia, operacion.stock());
        auditoriaRepository.save(new AuditoriaMercado(operacion.usuario(), transaccion, antes, despues, operacion.fecha()));
        log.info("mercado_operacion_confirmada tipo={} usuarioId={} jugadorId={} cantidad={} correlationId={}",
                tipo, operacion.usuario().getId(), operacion.jugador().getId(), operacion.cantidad(),
                MDC.get(CorrelationIdFilter.MDC_KEY));
        return new ResultadoOperacion(transaccion, operacion.cuenta().getSaldo());
    }

    @Transactional(readOnly = true)
    public MercadoJugadorResultado consultarMercado(Long jugadorId) {
        Jugador jugador = jugadorRepository.findById(jugadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException(JUGADOR_NO_EXISTE));
        Usuario superusuario = usuarioRepository.findByRol(Rol.SUPERUSUARIO)
                .orElseThrow(() -> new RecursoNoEncontradoException(SUPERUSUARIO_NO_EXISTE));
        Tenencia stock = tenenciaRepository.findByUsuarioIdAndJugadorId(superusuario.getId(), jugadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La tenencia del mercado no existe"));
        CotizacionVigentePort.CotizacionVigente cotizacion = cotizacionPort.vigente(jugador, Instant.now(clock));
        return new MercadoJugadorResultado(
                jugador.getId(),
                TOKENS_EMITIDOS,
                stock.getCantidad(),
                cotizacion.precio(),
                cotizacion.fechaCotizacion());
    }

    private static String snapshot(Cuenta cuenta, Tenencia usuario, Tenencia superusuario) {
        return "{\"saldoUsuario\":" + cuenta.getSaldo().toPlainString()
                + ",\"tokensUsuario\":" + usuario.getCantidad()
                + ",\"tokensSuperusuario\":" + superusuario.getCantidad() + "}";
    }

    private record Operacion(Usuario usuario, Jugador jugador, Usuario superusuario, int cantidad, Instant fecha,
                             CotizacionVigentePort.CotizacionVigente cotizacion, BigDecimal total, Cuenta cuenta,
                             Tenencia stock) {}
}
