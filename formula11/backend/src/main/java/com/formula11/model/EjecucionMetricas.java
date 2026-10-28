package com.formula11.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ejecucion_metricas")
public class EjecucionMetricas {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ejecucion_metricas_seq")
    @SequenceGenerator(name = "ejecucion_metricas_seq", sequenceName = "ejecucion_metricas_seq", allocationSize = 50)
    private Long id;
    @Column(nullable = false, length = 30)
    private String estado;
    @Column(nullable = false)
    private Instant inicio;
    private Instant fin;
    @Column(name = "correlation_id", length = 100)
    private String correlationId;
    @Column(columnDefinition = "text")
    private String error;

    protected EjecucionMetricas() {}

    public EjecucionMetricas(String estado, Instant inicio, String correlationId) {
        this.estado = estado;
        this.inicio = inicio;
        this.correlationId = correlationId;
    }

    public Long getId() { return id; }
    public String getEstado() { return estado; }
    public Instant getInicio() { return inicio; }
    public Instant getFin() { return fin; }
    public String getCorrelationId() { return correlationId; }
    public String getError() { return error; }
    public void completar(String estado, Instant fin) { this.estado = estado; this.fin = fin; }
    public void fallar(Instant fin, String error) {
        this.estado = "FALLIDA";
        this.fin = fin;
        this.error = error;
    }
}
