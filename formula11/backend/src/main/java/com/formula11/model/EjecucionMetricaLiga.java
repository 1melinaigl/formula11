package com.formula11.model;

import jakarta.persistence.*;

@Entity
@Table(name = "ejecucion_metrica_liga",
        uniqueConstraints = @UniqueConstraint(name = "ux_ejecucion_metrica_liga",
                columnNames = {"ejecucion_id", "liga"}))
public class EjecucionMetricaLiga {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ejecucion_metrica_liga_seq")
    @SequenceGenerator(name = "ejecucion_metrica_liga_seq", sequenceName = "ejecucion_metrica_liga_seq", allocationSize = 50)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ejecucion_id", nullable = false)
    private EjecucionMetricas ejecucion;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Liga liga;
    @Column(name = "partidos_encontrados", nullable = false)
    private int partidosEncontrados;
    @Column(name = "partidos_cargados", nullable = false)
    private int partidosCargados;
    @Column(name = "partidos_fallidos", nullable = false)
    private int partidosFallidos;
    @Column(name = "jugadores_vinculados", nullable = false)
    private int jugadoresVinculados;
    @Column(nullable = false)
    private int pendientes;

    protected EjecucionMetricaLiga() {}

    public EjecucionMetricaLiga(EjecucionMetricas ejecucion, Liga liga) {
        this.ejecucion = ejecucion;
        this.liga = liga;
    }

    public Long getId() { return id; }
    public EjecucionMetricas getEjecucion() { return ejecucion; }
    public Liga getLiga() { return liga; }
    public int getPartidosEncontrados() { return partidosEncontrados; }
    public int getPartidosCargados() { return partidosCargados; }
    public int getPartidosFallidos() { return partidosFallidos; }
    public int getJugadoresVinculados() { return jugadoresVinculados; }
    public int getPendientes() { return pendientes; }
    public void sumarEncontrados() { partidosEncontrados++; }
    public void sumarCargados() { partidosCargados++; }
    public void sumarFallidos() { partidosFallidos++; }
    public void sumarVinculados() { jugadoresVinculados++; }
    public void sumarPendientes() { pendientes++; }
}
