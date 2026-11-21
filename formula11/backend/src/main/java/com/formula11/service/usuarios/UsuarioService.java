package com.formula11.service.usuarios;

import com.formula11.exception.CredencialesInvalidasException;
import com.formula11.model.Usuario;
import com.formula11.persistence.UsuarioRepository;
import com.formula11.persistence.CuentaRepository;
import com.formula11.model.Cuenta;
import java.time.Clock;
import com.formula11.security.JwtService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.formula11.config.MercadoProperties;
import com.formula11.exception.RecursoNoEncontradoException;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CuentaRepository cuentaRepository;
    private final MercadoProperties mercadoProperties;
    private final Clock clock;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                          CuentaRepository cuentaRepository, MercadoProperties mercadoProperties,
                          Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.cuentaRepository = cuentaRepository;
        this.mercadoProperties = mercadoProperties;
        this.clock = clock;
    }

    @Transactional
    public UsuarioAutenticado registrar(String nombre, String email, String password) {
        String emailNormalizado = Usuario.normalizarEmail(email);
        if (usuarioRepository.existsByEmail(emailNormalizado)) throw new EmailDuplicadoException();
        Usuario usuario = new Usuario(nombre.trim(), emailNormalizado, passwordEncoder.encode(password), clock.instant());
        try {
            Usuario guardado = usuarioRepository.saveAndFlush(usuario);
            cuentaRepository.saveAndFlush(new Cuenta(guardado, mercadoProperties.getSaldoInicial()));
            return UsuarioAutenticado.de(guardado, jwtService.generarToken(
                    guardado.getId(), guardado.getEmail(), guardado.getRol()));
        } catch (DataIntegrityViolationException exception) {
            throw new EmailDuplicadoException();
        }
    }

    public UsuarioAutenticado login(String email, String password) {
        Usuario usuario = usuarioRepository.findByEmail(Usuario.normalizarEmail(email))
                .orElseThrow(CredencialesInvalidasException::new);
        if (!passwordEncoder.matches(password, usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException();
        }
        return UsuarioAutenticado.de(usuario, jwtService.generarToken(usuario.getId(), usuario.getEmail(), usuario.getRol()));
    }

    public Long obtenerIdPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe"))
                .getId();
    }

    public static class EmailDuplicadoException extends RuntimeException {
        public EmailDuplicadoException() { super("El email ya está registrado"); }
    }
}
