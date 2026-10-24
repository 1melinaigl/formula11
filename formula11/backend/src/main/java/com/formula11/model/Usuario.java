package com.formula11.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.Clock;

@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String nombre;
    @Column(nullable = false, length = 254)
    private String email;
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    @Column(name = "fecha_registro", nullable = false)
    private Instant fechaRegistro;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol = Rol.USUARIO;

    protected Usuario() { }

    public Usuario(String nombre, String email, String passwordHash) {
        this(nombre, email, passwordHash, Clock.systemUTC().instant());
    }

    public Usuario(String nombre, String email, String passwordHash, Instant fechaRegistro) {
        this.nombre = requireText(nombre, "El nombre es obligatorio");
        this.email = normalizarEmail(email);
        this.passwordHash = requireText(passwordHash, "La contraseña es obligatoria");
        this.fechaRegistro = java.util.Objects.requireNonNull(fechaRegistro, "La fecha de registro es obligatoria");
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value.trim();
    }

    public static String normalizarEmail(String email) {
        String normalized = requireText(email, "El email es obligatorio").toLowerCase(java.util.Locale.ROOT);
        if (!normalized.matches("^[^\\s@]++@[^\\s@.]++[.][^\\s@]++$")) throw new IllegalArgumentException("El email no es válido");
        return normalized;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Instant getFechaRegistro() { return fechaRegistro; }
    public Rol getRol() { return rol; }
    public boolean esSuperusuario() { return rol == Rol.SUPERUSUARIO; }
    public void asignarRol(Rol rol) { this.rol = java.util.Objects.requireNonNull(rol, "El rol es obligatorio"); }
}
