package com.formula11.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "metrica_partido")
public class MetricaPartido {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "metrica_partido_seq")
    @SequenceGenerator(name = "metrica_partido_seq", sequenceName = "metrica_partido_seq", allocationSize = 50)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jugador_id", nullable = false)
    private Jugador jugador;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partido_externo_id")
    private PartidoExterno partidoExterno;
    @Column(name = "fecha_partido", nullable = false)
    private LocalDate fechaPartido;
    private String rival;
    private Integer minutos;
    private Boolean titular;
    private int goles;
    private int asistencias;
    private int tiros;
    @Column(name = "pases_clave")
    private int pasesClave;
    private int regates;
    private int entradas;
    private int intercepciones;
    private int amarillas;
    private int rojas;
    private int atajadas;
    @Column(name = "goles_recibidos")
    private int golesRecibidos;
    @Column(name = "valla_invicta")
    private int vallaInvicta;
    private BigDecimal rating;
    private String fuente;

    protected MetricaPartido() {}

    public MetricaPartido(Jugador jugador, LocalDate fechaPartido, Integer minutos, Boolean titular,
                          int goles, int asistencias, int tiros, int pasesClave, int regates,
                          int entradas, int intercepciones, int amarillas, int rojas, int atajadas,
                          int golesRecibidos, int vallaInvicta, BigDecimal rating, String fuente) {
        this.jugador = jugador;
        this.fechaPartido = fechaPartido;
        this.minutos = minutos;
        this.titular = titular;
        this.goles = goles;
        this.asistencias = asistencias;
        this.tiros = tiros;
        this.pasesClave = pasesClave;
        this.regates = regates;
        this.entradas = entradas;
        this.intercepciones = intercepciones;
        this.amarillas = amarillas;
        this.rojas = rojas;
        this.atajadas = atajadas;
        this.golesRecibidos = golesRecibidos;
        this.vallaInvicta = vallaInvicta;
        this.rating = rating;
        this.fuente = fuente;
    }

    public Long getId() { return id; }
    public Jugador getJugador() { return jugador; }
    public LocalDate getFechaPartido() { return fechaPartido; }
    public Integer getMinutos() { return minutos; }
    public Boolean getTitular() { return titular; }
    public int getGoles() { return goles; }
    public int getAsistencias() { return asistencias; }
    public int getTiros() { return tiros; }
    public int getPasesClave() { return pasesClave; }
    public int getRegates() { return regates; }
    public int getEntradas() { return entradas; }
    public int getIntercepciones() { return intercepciones; }
    public int getAmarillas() { return amarillas; }
    public int getRojas() { return rojas; }
    public int getAtajadas() { return atajadas; }
    public int getGolesRecibidos() { return golesRecibidos; }
    public int getVallaInvicta() { return vallaInvicta; }
    public BigDecimal getRating() { return rating; }
    public String getFuente() { return fuente; }
    public PartidoExterno getPartidoExterno() { return partidoExterno; }

    public void asociarPartidoExterno(PartidoExterno partidoExterno) {
        this.partidoExterno = partidoExterno;
    }
}
