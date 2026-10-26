package com.formula11.model;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "estrategia", uniqueConstraints = @UniqueConstraint(name = "ux_estrategia_codigo_version", columnNames = {"codigo", "version"}))
public class Estrategia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String codigo;
    @Column(nullable = false) private String nombre;
    @Column(nullable = false) private int version;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb") private Map<String, Map<String, Double>> pesos;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb") private Map<String, Double> parametros;
    @Column(nullable = false) private boolean activa;
    @Column(name = "creada_en", nullable = false) private Instant creadaEn;

    protected Estrategia() {}
    public Estrategia(String codigo, String nombre, int version, Map<String, Map<String, Double>> pesos,
                      Map<String, Double> parametros, boolean activa, Instant creadaEn) {
        this.codigo = codigo; this.nombre = nombre; this.version = version; this.pesos = pesos;
        this.parametros = parametros; this.activa = activa; this.creadaEn = creadaEn;
    }
    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public int getVersion() { return version; }
    public Map<String, Map<String, Double>> getPesos() { return pesos; }
    public Map<String, Double> getParametros() { return parametros; }
    public boolean isActiva() { return activa; }
    public Instant getCreadaEn() { return creadaEn; }
    public void activar(boolean activa) { this.activa = activa; }
}
