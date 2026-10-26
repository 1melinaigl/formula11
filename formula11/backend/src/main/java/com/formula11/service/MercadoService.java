package com.formula11.service;

import com.formula11.exception.ConflictoMercadoException;
import com.formula11.exception.OperacionNoValidaException;
import com.formula11.exception.RecursoNoEncontradoException;
import com.formula11.exception.SuperusuarioNoOperaException;
import com.formula11.model.*;
import com.formula11.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

@Service
public class MercadoService {
    private static final Logger log = LoggerFactory.getLogger(MercadoService.class);
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

    // Variante con reloj explícito: la usa la demo del perfil local para comprar "al inicio del campeonato".
    @Transactional
    public ResultadoOperacion comprar(Long usuarioId, Long jugadorId, int cantidad, Clock reloj) {
        return ejecutarCompra(usuarioId, jugadorId, cantidad, reloj);
    }

    // Núcleo de la compra; corre dentro de la transacción del método público que lo llama.
    private ResultadoOperacion ejecutarCompra(Long usuarioId, Long jugadorId, int cantidad, Clock reloj) {
        if (cantidad < 1 || cantidad > 100) throw new IllegalArgumentException("La cantidad debe estar entre 1 y 100");
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe"));
        if (usuario.esSuperusuario()) throw new SuperusuarioNoOperaException();
        Jugador jugador = jugadorRepository.findById(jugadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El jugador no existe"));
        Usuario superusuario = usuarioRepository.findByRol(Rol.SUPERUSUARIO)
                .orElseThrow(() -> new RecursoNoEncontradoException("El superusuario no existe"));
        Instant fecha = Instant.now(reloj);
        CotizacionVigentePort.CotizacionVigente cotizacion = cotizacionPort.vigente(jugador, fecha);
        BigDecimal total = cotizacion.precio().multiply(BigDecimal.valueOf(cantidad)).setScale(4, java.math.RoundingMode.HALF_UP);

        Cuenta cuenta = cuentaRepository.findByUsuarioIdForUpdate(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La cuenta del usuario no existe"));
        Tenencia stock = tenenciaRepository.findByUsuarioIdAndJugadorIdForUpdate(superusuario.getId(), jugadorId)
                .orElseThrow(() -> new ConflictoMercadoException("El superusuario no tiene tokens de ese jugador"));
        Tenencia tenencia = tenenciaRepository.findByUsuarioIdAndJugadorIdForUpdate(usuarioId, jugadorId)
                .orElseGet(() -> new Tenencia(usuario, jugador, 0, cotizacion.precio()));
        if (cuenta.getSaldo().compareTo(total) < 0) throw new OperacionNoValidaException("El usuario no tiene saldo suficiente");
        if (stock.getCantidad() < cantidad) throw new ConflictoMercadoException("El superusuario no tiene tokens suficientes de ese jugador");

        String antes = snapshot(cuenta, tenencia, stock);
        cuenta.debitar(total);
        stock.vender(cantidad);
        tenencia.comprar(cantidad, cotizacion.precio());
        Transaccion transaccion = transaccionRepository.save(new Transaccion(
                Transaccion.Tipo.COMPRA, usuario, jugador, cantidad, cotizacion.precio(),
                superusuario, fecha, cotizacion.estrategia(), cotizacion.version()));
        tenenciaRepository.save(tenencia);
        auditoriaRepository.save(new AuditoriaMercado(usuario, transaccion, antes, snapshot(cuenta, tenencia, stock), fecha));
        log.info("mercado_operacion_confirmada tipo=COMPRA usuarioId={} jugadorId={} cantidad={} correlationId={}",
                usuarioId, jugadorId, cantidad, org.slf4j.MDC.get("correlationId"));
        return new ResultadoOperacion(transaccion, cuenta.getSaldo());
    }

    @Transactional
    public ResultadoOperacion vender(Long usuarioId, Long jugadorId, int cantidad) {
        if (cantidad < 1 || cantidad > 100) throw new IllegalArgumentException("La cantidad debe estar entre 1 y 100");
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe"));
        if (usuario.esSuperusuario()) throw new SuperusuarioNoOperaException();
        Jugador jugador = jugadorRepository.findById(jugadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El jugador no existe"));
        Usuario superusuario = usuarioRepository.findByRol(Rol.SUPERUSUARIO)
                .orElseThrow(() -> new RecursoNoEncontradoException("El superusuario no existe"));
        Instant fecha = Instant.now(clock);
        CotizacionVigentePort.CotizacionVigente cotizacion = cotizacionPort.vigente(jugador, fecha);
        BigDecimal total = cotizacion.precio().multiply(BigDecimal.valueOf(cantidad))
                .setScale(4, java.math.RoundingMode.HALF_UP);

        Cuenta cuenta = cuentaRepository.findByUsuarioIdForUpdate(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La cuenta del usuario no existe"));
        Tenencia stock = tenenciaRepository.findByUsuarioIdAndJugadorIdForUpdate(superusuario.getId(), jugadorId)
                .orElseThrow(() -> new ConflictoMercadoException("El superusuario no tiene tokens de ese jugador"));
        Tenencia tenencia = tenenciaRepository.findByUsuarioIdAndJugadorIdForUpdate(usuarioId, jugadorId)
                .orElseThrow(() -> new OperacionNoValidaException("El usuario no tiene tokens de ese jugador"));
        if (tenencia.getCantidad() < cantidad) {
            throw new OperacionNoValidaException("El usuario no tiene esa cantidad de tokens del jugador");
        }

        String antes = snapshot(cuenta, tenencia, stock);
        tenencia.vender(cantidad);
        stock.comprar(cantidad, stock.getPrecioPromedio());
        cuenta.acreditar(total);
        Transaccion transaccion = transaccionRepository.save(new Transaccion(
                Transaccion.Tipo.VENTA, usuario, jugador, cantidad, cotizacion.precio(),
                superusuario, fecha, cotizacion.estrategia(), cotizacion.version()));
        tenenciaRepository.save(tenencia);
        tenenciaRepository.save(stock);
        auditoriaRepository.save(new AuditoriaMercado(usuario, transaccion, antes, snapshot(cuenta, tenencia, stock), fecha));
        log.info("mercado_operacion_confirmada tipo=VENTA usuarioId={} jugadorId={} cantidad={} correlationId={}",
                usuarioId, jugadorId, cantidad, org.slf4j.MDC.get("correlationId"));
        return new ResultadoOperacion(transaccion, cuenta.getSaldo());
    }

    @Transactional(readOnly = true)
    public MercadoJugadorResultado consultarMercado(Long jugadorId) {
        Jugador jugador = jugadorRepository.findById(jugadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El jugador no existe"));
        Usuario superusuario = usuarioRepository.findByRol(Rol.SUPERUSUARIO)
                .orElseThrow(() -> new RecursoNoEncontradoException("El superusuario no existe"));
        Tenencia stock = tenenciaRepository.findByUsuarioIdAndJugadorId(superusuario.getId(), jugadorId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La tenencia del mercado no existe"));
        CotizacionVigentePort.CotizacionVigente cotizacion = cotizacionPort.vigente(jugador, Instant.now(clock));
        return new MercadoJugadorResultado(
                jugador.getId(),
                100,
                stock.getCantidad(),
                cotizacion.precio(),
                cotizacion.fechaCotizacion());
    }

    private static String snapshot(Cuenta cuenta, Tenencia usuario, Tenencia superusuario) {
        return "{\"saldoUsuario\":" + cuenta.getSaldo().toPlainString()
                + ",\"tokensUsuario\":" + usuario.getCantidad()
                + ",\"tokensSuperusuario\":" + superusuario.getCantidad() + "}";
    }
}
