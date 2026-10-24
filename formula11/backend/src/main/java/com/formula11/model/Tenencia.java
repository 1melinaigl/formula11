package com.formula11.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "tenencias", uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "jugador_id"}))
public class Tenencia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "usuario_id", nullable = false) private Usuario usuario;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "jugador_id", nullable = false) private Jugador jugador;
    @Column(nullable = false) private int cantidad;
    @Column(name = "precio_promedio", nullable = false, precision = 19, scale = 4) private BigDecimal precioPromedio;
    protected Tenencia() { }
    public Tenencia(Usuario usuario, Jugador jugador, int cantidad, BigDecimal precioPromedio) {
        this.usuario = java.util.Objects.requireNonNull(usuario, "El usuario es obligatorio");
        this.jugador = java.util.Objects.requireNonNull(jugador, "El jugador es obligatorio");
        if (cantidad < 0) throw new IllegalArgumentException("La cantidad no puede ser negativa");
        this.cantidad = cantidad; this.precioPromedio = dineroNoNegativo(precioPromedio);
    }
    public void comprar(int cantidad, BigDecimal precio) {
        if (cantidad <= 0) throw new IllegalArgumentException("La cantidad debe ser positiva");
        BigDecimal p = dineroNoNegativo(precio);
        BigDecimal anterior = precioPromedio.multiply(BigDecimal.valueOf(this.cantidad));
        this.cantidad += cantidad;
        this.precioPromedio = anterior.add(p.multiply(BigDecimal.valueOf(cantidad)))
                .divide(BigDecimal.valueOf(this.cantidad), 4, RoundingMode.HALF_UP);
    }
    public void vender(int cantidad) {
        if (cantidad <= 0) throw new IllegalArgumentException("La cantidad debe ser positiva");
        if (this.cantidad < cantidad) throw new IllegalStateException("La tenencia es insuficiente");
        this.cantidad -= cantidad;
    }
    private static BigDecimal dineroNoNegativo(BigDecimal valor) {
        if (valor == null || valor.signum() < 0) throw new IllegalArgumentException("El precio promedio no puede ser negativo");
        return valor.setScale(4, RoundingMode.HALF_UP);
    }
    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public Jugador getJugador() { return jugador; }
    public int getCantidad() { return cantidad; }
    public BigDecimal getPrecioPromedio() { return precioPromedio; }
}
