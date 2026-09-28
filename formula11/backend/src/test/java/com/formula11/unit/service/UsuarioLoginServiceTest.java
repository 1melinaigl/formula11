package com.formula11.unit.service;

import com.formula11.dto.LoginRequest;
import com.formula11.exception.CredencialesInvalidasException;
import com.formula11.model.Usuario;
import com.formula11.persistence.UsuarioRepository;
import com.formula11.security.JwtService;
import com.formula11.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioLoginServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void loginEmiteTokenConEmailNormalizado() {
        Usuario usuario = usuario("Ana", "ana@example.com", "hash-encriptado", 1L);
        when(usuarioRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Secreto123!", "hash-encriptado")).thenReturn(true);
        when(jwtService.generarToken(1L, "ana@example.com")).thenReturn("token-jwt");

        var respuesta = usuarioService.login(new LoginRequest(" ANA@EXAMPLE.COM ", "Secreto123!"));

        assertEquals(1L, respuesta.id());
        assertEquals("Ana", respuesta.nombre());
        assertEquals("ana@example.com", respuesta.email());
        assertEquals("token-jwt", respuesta.token());
        verify(usuarioRepository).findByEmail("ana@example.com");
        verify(jwtService).generarToken(1L, "ana@example.com");
    }

    @Test
    void loginRechazaEmailInexistenteSinEmitirToken() {
        when(usuarioRepository.findByEmail("no-existe@example.com")).thenReturn(Optional.empty());
        LoginRequest request = new LoginRequest("no-existe@example.com", "Secreto123!");

        var exception = assertThrows(CredencialesInvalidasException.class,
        () -> usuarioService.login(request));

        assertEquals("Las credenciales no son válidas", exception.getMessage());
        verify(passwordEncoder, never()).matches(eq("Secreto123!"), eq("hash-encriptado"));
        verify(jwtService, never()).generarToken(anyLong(), eq("no-existe@example.com"));
    }

    @Test
    void loginRechazaPasswordIncorrectaConElMismoMensaje() {
    Usuario usuario = usuario("Ana", "ana@example.com", "hash-encriptado", 1L);
    when(usuarioRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(usuario));
    when(passwordEncoder.matches("Incorrecta1!", "hash-encriptado")).thenReturn(false);
    LoginRequest request = new LoginRequest("ana@example.com", "Incorrecta1!");

    var exception = assertThrows(CredencialesInvalidasException.class,
            () -> usuarioService.login(request));

    assertEquals("Las credenciales no son válidas", exception.getMessage());
    verify(jwtService, never()).generarToken(anyLong(), eq("ana@example.com"));
}
    private Usuario usuario(String nombre, String email, String passwordHash, Long id) {
        Usuario usuario = new Usuario(nombre, email, passwordHash);
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }
}
