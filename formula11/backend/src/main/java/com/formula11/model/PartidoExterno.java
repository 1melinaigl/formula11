package com.formula11.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "partido_externo",
        uniqueConstraints = @UniqueConstraint(name = "ux_partido_externo_fuente_id",
                columnNames = {"fuente", "id_externo"}))
public class PartidoExterno {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "partido_externo_seq")
    @SequenceGenerator(name = "partido_externo_seq", sequenceName = "partido_externo_seq", allocationSize = 50)
    private Long id;
    @Column(nullable = false, length = 50)
    private String fuente;
    @Column(name = "id_externo", nullable = false, length = 100)
    private String idExterno;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Liga liga;
    @Column(name = "fecha_partido", nullable = false)
    private LocalDate fechaPartido;
    @Column(name = "equipo_local", length = 150)
    private String equipoLocal;
    @Column(name = "equipo_visitante", length = 150)
    private String equipoVisitante;
    @Column(name = "fecha_carga", nullable = false)
    private Instant fechaCarga;
    @Column(nullable = false, length = 30)
    private String estado = "CARGADO";
    @Column(columnDefinition = "text")
    private String error;

    protected PartidoExterno() {}

    public PartidoExterno(String fuente, String idExterno, Liga liga, LocalDate fechaPartido,
                          String equipoLocal, String equipoVisitante, Instant fechaCarga) {
        this.fuente = fuente;
        this.idExterno = idExterno;
        this.liga = liga;
        this.fechaPartido = fechaPartido;
        this.equipoLocal = equipoLocal;
        this.equipoVisitante = equipoVisitante;
        this.fechaCarga = fechaCarga;
    }

    public Long getId() { return id; }
    public String getFuente() { return fuente; }
    public String getIdExterno() { return idExterno; }
    public Liga getLiga() { return liga; }
    public LocalDate getFechaPartido() { return fechaPartido; }
    public String getEquipoLocal() { return equipoLocal; }
    public String getEquipoVisitante() { return equipoVisitante; }
    public Instant getFechaCarga() { return fechaCarga; }
    public String getEstado() { return estado; }
    public String getError() { return error; }
}
