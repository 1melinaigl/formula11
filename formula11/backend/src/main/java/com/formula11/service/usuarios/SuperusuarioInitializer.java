package com.formula11.service.usuarios;

import com.formula11.config.MercadoProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.annotation.Order;
import java.math.BigDecimal;
import java.time.Clock;
import com.formula11.persistence.jugadores.JugadorRepository;
import com.formula11.persistence.mercado.TenenciaRepository;
import com.formula11.persistence.usuarios.UsuarioRepository;
import com.formula11.model.jugadores.Jugador;
import com.formula11.model.mercado.Tenencia;
import com.formula11.model.usuarios.Rol;
import com.formula11.model.usuarios.Usuario;

@Component
@Order(1)
public class SuperusuarioInitializer implements ApplicationRunner {
    private static final int TOKENS_EMITIDOS = 100;
    private static final BigDecimal PRECIO_INICIAL = BigDecimal.ONE;
    private final UsuarioRepository usuarioRepository;
    private final JugadorRepository jugadorRepository;
    private final TenenciaRepository tenenciaRepository;
    private final PasswordEncoder passwordEncoder;
    private final MercadoProperties properties;
    private final Clock clock;

    public SuperusuarioInitializer(UsuarioRepository usuarioRepository, JugadorRepository jugadorRepository,
                                   TenenciaRepository tenenciaRepository, PasswordEncoder passwordEncoder,
                                   MercadoProperties properties, Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.jugadorRepository = jugadorRepository;
        this.tenenciaRepository = tenenciaRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Usuario superusuario = usuarioRepository.findByRol(Rol.SUPERUSUARIO).orElseGet(() -> {
            Usuario nuevo = new Usuario("Superusuario",
                    properties.getSuperusuarioEmail(),
                    passwordEncoder.encode(properties.getSuperusuarioPassword()),
                    clock.instant());
            nuevo.asignarRol(Rol.SUPERUSUARIO);
            return usuarioRepository.saveAndFlush(nuevo);
        });
        for (Jugador jugador : jugadorRepository.findAll()) {
            if (tenenciaRepository.findByUsuarioIdAndJugadorId(superusuario.getId(), jugador.getId()).isEmpty()) {
                tenenciaRepository.save(new Tenencia(superusuario, jugador, TOKENS_EMITIDOS, PRECIO_INICIAL));
            }
        }
    }
}
