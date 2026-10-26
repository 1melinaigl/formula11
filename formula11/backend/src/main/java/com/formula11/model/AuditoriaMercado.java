package com.formula11.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "auditoria_mercado")
public class AuditoriaMercado {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "autor_id") private Usuario autor;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "operacion_id", unique = true) private Transaccion operacion;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb") private String antes;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb") private String despues;
    @Column(nullable = false) private Instant timestamp;
    protected AuditoriaMercado() { }
    public AuditoriaMercado(Usuario autor, Transaccion operacion, String antes, String despues, Instant timestamp) {
        this.autor = java.util.Objects.requireNonNull(autor, "El autor es obligatorio");
        this.operacion = java.util.Objects.requireNonNull(operacion, "La operación es obligatoria");
        if (antes == null || despues == null || timestamp == null) throw new IllegalArgumentException("La auditoría está incompleta");
        this.antes = antes; this.despues = despues; this.timestamp = timestamp;
    }
    public Long getId() { return id; }
    public Transaccion getOperacion() { return operacion; }
    public String getAntes() { return antes; }
    public String getDespues() { return despues; }
}
