package com.formula11.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/** Señal de jugador subvaluado detectada en una semana. Única por jugador, fecha y tipo. */
@Entity
@Table(name = "senal", uniqueConstraints = @UniqueConstraint(name = "ux_senal_jugador_fecha_tipo",
        columnNames = {"jugador_id", "fecha", "tipo"}))
public class Senal {
    public enum Tipo { ENTRE_ESTRATEGIAS, PRECIO_ATRASADO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jugador_id", nullable = false)
    private Jugador jugador;
    @Column(nullable = false)
    private LocalDate fecha;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Tipo tipo;
    @Column(nullable = false, length = 80)
    private String estrategia;
    @Column(nullable = false, precision = 9, scale = 2)
    private BigDecimal brecha;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal precio;
    @Column(name = "precio_estimado", nullable = false, precision = 19, scale = 4)
    private BigDecimal precioEstimado;
    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;

    protected Senal() {
    }

    public Senal(Jugador jugador, LocalDate fecha, Tipo tipo, String estrategia, BigDecimal brecha,
                 BigDecimal precio, BigDecimal precioEstimado, Instant creadaEn) {
        this.jugador = Objects.requireNonNull(jugador, "El jugador es obligatorio");
        this.fecha = Objects.requireNonNull(fecha, "La fecha es obligatoria");
        this.tipo = Objects.requireNonNull(tipo, "El tipo es obligatorio");
        this.creadaEn = Objects.requireNonNull(creadaEn, "La fecha de creación es obligatoria");
        asignarValores(estrategia, brecha, precio, precioEstimado);
    }

    /** Actualiza los valores de una señal que sigue aplicando al regenerar la semana; conserva el id. */
    public void actualizar(String estrategia, BigDecimal brecha, BigDecimal precio, BigDecimal precioEstimado) {
        asignarValores(estrategia, brecha, precio, precioEstimado);
    }

    private void asignarValores(String estrategia, BigDecimal brecha, BigDecimal precio, BigDecimal precioEstimado) {
        if (estrategia == null || estrategia.isBlank()) {
            throw new IllegalArgumentException("La estrategia es obligatoria");
        }
        if (brecha == null || brecha.signum() < 0) {
            throw new IllegalArgumentException("La brecha no puede ser negativa");
        }
        if (precio == null || precio.signum() <= 0 || precioEstimado == null || precioEstimado.signum() <= 0) {
            throw new IllegalArgumentException("Los precios de la señal deben ser positivos");
        }
        this.estrategia = estrategia;
        this.brecha = brecha.setScale(2, RoundingMode.HALF_UP);
        this.precio = precio.setScale(4, RoundingMode.HALF_UP);
        this.precioEstimado = precioEstimado.setScale(4, RoundingMode.HALF_UP);
    }

    public Long getId() { return id; }
    public Jugador getJugador() { return jugador; }
    public LocalDate getFecha() { return fecha; }
    public Tipo getTipo() { return tipo; }
    public String getEstrategia() { return estrategia; }
    public BigDecimal getBrecha() { return brecha; }
    public BigDecimal getPrecio() { return precio; }
    public BigDecimal getPrecioEstimado() { return precioEstimado; }
    public Instant getCreadaEn() { return creadaEn; }
}
