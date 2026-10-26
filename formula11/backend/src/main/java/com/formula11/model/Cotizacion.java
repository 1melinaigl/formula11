package com.formula11.model;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Entity
@Table(name = "cotizacion", uniqueConstraints = @UniqueConstraint(name = "ux_cotizacion_jugador_estrategia_fecha", columnNames = {"jugador_id", "estrategia", "fecha"}))
public class Cotizacion {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cotizacion_seq")
    @SequenceGenerator(name = "cotizacion_seq", sequenceName = "cotizacion_seq", allocationSize = 50)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "jugador_id", nullable = false) private Jugador jugador;
    @Column(nullable = false) private LocalDate fecha;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal precio;
    @Column(nullable = false, precision = 19, scale = 8) private BigDecimal score;
    @Column(nullable = false) private String estrategia;
    @Column(nullable = false) private int version;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb") private Map<String, Map<String, Double>> pesos;
    protected Cotizacion() {}
    public Cotizacion(Jugador jugador, LocalDate fecha, BigDecimal precio, BigDecimal score, String estrategia, int version, Map<String, Map<String, Double>> pesos) {
        this.jugador = jugador; this.fecha = fecha; this.precio = precio; this.score = score; this.estrategia = estrategia; this.version = version; this.pesos = pesos;
    }
    public Long getId() { return id; } public Jugador getJugador() { return jugador; } public LocalDate getFecha() { return fecha; }
    public BigDecimal getPrecio() { return precio; } public BigDecimal getScore() { return score; } public String getEstrategia() { return estrategia; }
    public int getVersion() { return version; } public Map<String, Map<String, Double>> getPesos() { return pesos; }
}
