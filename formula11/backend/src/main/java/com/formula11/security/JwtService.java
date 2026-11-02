package com.formula11.security;

import com.formula11.model.Rol;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationSeconds;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration:3600}") long expirationSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        this.expirationSeconds = expirationSeconds;
    }

    public String generarToken(Long usuarioId, String email) {
        return generarToken(usuarioId, email, Rol.USUARIO);
    }

    public String generarToken(Long usuarioId, String email, Rol rol) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim("usuarioId", usuarioId)
                .claim("rol", rol.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(key)
                .compact();
    }

    public String extraerEmail(String token) {
        return parsear(token).getSubject();
    }

    public boolean esValido(String token) {
        try {
            parsear(token);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public Long extraerUsuarioId(String token) {
        return parsear(token).get("usuarioId", Long.class);
    }

    public Rol extraerRol(String token) {
        String claim = parsear(token).get("rol", String.class);
        return claim == null ? Rol.USUARIO : Rol.valueOf(claim);
    }

    private Claims parsear(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
