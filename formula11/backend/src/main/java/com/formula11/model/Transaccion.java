package com.formula11.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Entity
@Table(name = "transacciones")
public class Transaccion {
    public enum Tipo { COMPRA, VENTA }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private Tipo tipo;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "usuario_id") private Usuario usuario;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "jugador_id") private Jugador jugador;
    @Column(nullable = false) private int cantidad;
    @Column(name = "precio_unitario", nullable = false, precision = 19, scale = 4) private BigDecimal precioUnitario;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal total;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "contraparte_id") private Usuario contraparte;
    @Column(nullable = false) private Instant fecha;
    private String estrategia;
    @Column(name = "version_cotizacion") private Integer versionCotizacion;
    protected Transaccion() { }
    public Transaccion(Tipo tipo, Usuario usuario, Jugador jugador, int cantidad, BigDecimal precioUnitario,
                       Usuario contraparte, Instant fecha, String estrategia, Integer versionCotizacion) {
        if (tipo == null || usuario == null || jugador == null || contraparte == null || fecha == null) throw new IllegalArgumentException("La transacción está incompleta");
        if (cantidad <= 0) throw new IllegalArgumentException("La cantidad debe ser positiva");
        this.tipo = tipo; this.usuario = usuario; this.jugador = jugador; this.cantidad = cantidad;
        this.precioUnitario = dinero(precioUnitario); this.total = dinero(this.precioUnitario.multiply(BigDecimal.valueOf(cantidad)));
        this.contraparte = contraparte; this.fecha = fecha; this.estrategia = estrategia; this.versionCotizacion = versionCotizacion;
    }
    private static BigDecimal dinero(BigDecimal valor) {
        if (valor == null || valor.signum() < 0) throw new IllegalArgumentException("El importe no puede ser negativo");
        return valor.setScale(4, RoundingMode.HALF_UP);
    }
    public Long getId() { return id; }
    public Tipo getTipo() { return tipo; }
    public Usuario getUsuario() { return usuario; }
    public Jugador getJugador() { return jugador; }
    public int getCantidad() { return cantidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public BigDecimal getTotal() { return total; }
    public Instant getFecha() { return fecha; }
    public Usuario getContraparte() { return contraparte; }
    public String getEstrategia() { return estrategia; }
    public Integer getVersionCotizacion() { return versionCotizacion; }
}
