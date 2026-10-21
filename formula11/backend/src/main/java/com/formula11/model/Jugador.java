package com.formula11.model;

import jakarta.persistence.*;

@Entity
@Table(name = "jugadores")
public class Jugador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 150)
    private String nombre;
    @Column(nullable = false, length = 150)
    private String equipo;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Liga liga;
    @Column(nullable = false, length = 80)
    private String posicion;
    @Column(name = "external_provider", length = 50)
    private String externalProvider;
    @Column(name = "external_id", length = 100)
    private String externalId;
    @Column(nullable = false, length = 20)
    private String origen = "LEGACY";

    protected Jugador() { }

    public Jugador(String nombre, String equipo, Liga liga, String posicion) {
        this.nombre = requireText(nombre, "El nombre del jugador es obligatorio");
        this.equipo = requireText(equipo, "El equipo es obligatorio");
        this.liga = java.util.Objects.requireNonNull(liga, "La liga es obligatoria");
        this.posicion = requireText(posicion, "La posición es obligatoria");
    }

    public Jugador(String nombre, String equipo, Liga liga, String posicion, String externalProvider, String externalId) {
        this(nombre, equipo, liga, posicion);
        actualizarDesdeExterno(externalProvider, externalId, nombre, equipo, liga, posicion);
    }

    public void actualizarDesdeExterno(String externalProvider, String externalId, String nombre, String equipo, Liga liga, String posicion) {
        this.nombre = requireText(nombre, "El nombre del jugador es obligatorio");
        this.equipo = requireText(equipo, "El equipo es obligatorio");
        this.liga = java.util.Objects.requireNonNull(liga, "La liga es obligatoria");
        this.posicion = requireText(posicion, "La posición es obligatoria");
        this.externalProvider = requireText(externalProvider, "El proveedor externo es obligatorio");
        this.externalId = requireText(externalId, "El id externo es obligatorio");
        this.origen = "EXTERNAL";
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value.trim();
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getEquipo() { return equipo; }
    public Liga getLiga() { return liga; }
    public String getPosicion() { return posicion; }
    public String getExternalProvider() { return externalProvider; }
    public String getExternalId() { return externalId; }
    public String getOrigen() { return origen; }
}
