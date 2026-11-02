package com.formula11.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "pendiente_vinculacion")
public class PendienteVinculacion {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pendiente_vinculacion_seq")
    @SequenceGenerator(name = "pendiente_vinculacion_seq", sequenceName = "pendiente_vinculacion_seq", allocationSize = 50)
    private Long id;
    @Column(nullable = false, length = 50)
    private String fuente;
    @Column(name = "whoscored_id", length = 100)
    private String whoscoredId;
    @Column(nullable = false, length = 150)
    private String nombre;
    @Column(length = 150)
    private String equipo;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Liga liga;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partido_externo_id")
    private PartidoExterno partidoExterno;
    @Column(name = "fecha_registro", nullable = false)
    private Instant fechaRegistro;
    @Column(nullable = false)
    private boolean resuelto;

    protected PendienteVinculacion() {}

    public PendienteVinculacion(String fuente, String whoscoredId, String nombre,
                                String equipo, Liga liga, Instant fechaRegistro) {
        this.fuente = fuente;
        this.whoscoredId = whoscoredId;
        this.nombre = nombre;
        this.equipo = equipo;
        this.liga = liga;
        this.fechaRegistro = fechaRegistro;
    }

    public Long getId() { return id; }
    public String getFuente() { return fuente; }
    public String getWhoscoredId() { return whoscoredId; }
    public String getNombre() { return nombre; }
    public String getEquipo() { return equipo; }
    public Liga getLiga() { return liga; }
    public PartidoExterno getPartidoExterno() { return partidoExterno; }
    public Instant getFechaRegistro() { return fechaRegistro; }
    public boolean isResuelto() { return resuelto; }
}
