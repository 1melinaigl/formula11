package com.formula11.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "cuentas")
public class Cuenta {
    private static final int SCALE = 4;
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true) private Usuario usuario;
    @Column(nullable = false, precision = 19, scale = SCALE) private BigDecimal saldo;
    protected Cuenta() { }
    public Cuenta(Usuario usuario, BigDecimal saldoInicial) {
        this.usuario = java.util.Objects.requireNonNull(usuario, "El usuario es obligatorio");
        this.saldo = dineroNoNegativo(saldoInicial);
    }
    public void debitar(BigDecimal importe) {
        BigDecimal valor = dineroNoNegativo(importe);
        if (saldo.compareTo(valor) < 0) throw new IllegalStateException("El saldo es insuficiente");
        saldo = saldo.subtract(valor).setScale(SCALE, RoundingMode.HALF_UP);
    }
    public void acreditar(BigDecimal importe) { saldo = saldo.add(dineroNoNegativo(importe)).setScale(SCALE, RoundingMode.HALF_UP); }
    private static BigDecimal dineroNoNegativo(BigDecimal valor) {
        if (valor == null || valor.signum() < 0) throw new IllegalArgumentException("El saldo no puede ser negativo");
        return valor.setScale(SCALE, RoundingMode.HALF_UP);
    }
    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public BigDecimal getSaldo() { return saldo; }
}
