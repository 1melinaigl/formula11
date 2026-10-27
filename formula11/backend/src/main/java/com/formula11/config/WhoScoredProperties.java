package com.formula11.config;

import com.formula11.model.Liga;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.Map;

@Validated
@ConfigurationProperties(prefix = "app.whoscored")
public class WhoScoredProperties {
    @NotBlank
    private String modo = "automatico";
    @NotNull
    private Map<Liga, Long> ligas = new EnumMap<>(Map.of(
            Liga.PREMIER_LEAGUE, 25544L,
            Liga.BUNDESLIGA, 25666L,
            Liga.LA_LIGA, 25662L,
            Liga.SERIE_A, 25518L,
            Liga.LIGUE_1, 25554L));
    @NotNull
    private LocalDate inicioTemporada = LocalDate.of(2026, 8, 1);
    @NotBlank
    private String zonaFuente = "Europe/London";
    @NotBlank
    private String perfilDir = "target/whoscored-perfil";
    private boolean headless;
    @NotNull
    @Positive
    private Duration pausa = Duration.ofSeconds(3);
    @NotNull
    @Positive
    private Duration timeout = Duration.ofSeconds(90);
    private int reintentos = 3;
    @NotBlank
    private String carpetaPaginas = "target/whoscored-paginas";

    public String getModo() { return modo; }
    public void setModo(String modo) { this.modo = modo; }
    public Map<Liga, Long> getLigas() { return ligas; }
    public void setLigas(Map<Liga, Long> ligas) { this.ligas = ligas; }
    public LocalDate getInicioTemporada() { return inicioTemporada; }
    public void setInicioTemporada(LocalDate inicioTemporada) { this.inicioTemporada = inicioTemporada; }
    public String getZonaFuente() { return zonaFuente; }
    public void setZonaFuente(String zonaFuente) { this.zonaFuente = zonaFuente; }
    public String getPerfilDir() { return perfilDir; }
    public void setPerfilDir(String perfilDir) { this.perfilDir = perfilDir; }
    public boolean isHeadless() { return headless; }
    public void setHeadless(boolean headless) { this.headless = headless; }
    public Duration getPausa() { return pausa; }
    public void setPausa(Duration pausa) { this.pausa = pausa; }
    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration timeout) { this.timeout = timeout; }
    public int getReintentos() { return reintentos; }
    public void setReintentos(int reintentos) { this.reintentos = reintentos; }
    public String getCarpetaPaginas() { return carpetaPaginas; }
    public void setCarpetaPaginas(String carpetaPaginas) { this.carpetaPaginas = carpetaPaginas; }

    @AssertTrue(message = "app.whoscored.modo debe ser automatico o archivos")
    public boolean isModoValido() {
        return "automatico".equalsIgnoreCase(modo) || "archivos".equalsIgnoreCase(modo);
    }

    @AssertTrue(message = "app.whoscored.zona-fuente debe ser una zona horaria válida")
    public boolean isZonaFuenteValida() {
        try {
            ZoneId.of(zonaFuente);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    @AssertTrue(message = "app.whoscored.ligas debe contener las cinco ligas con stage IDs positivos")
    public boolean isLigasCompletas() {
        return ligas != null && ligas.size() == Liga.values().length
                && java.util.Arrays.stream(Liga.values()).allMatch(liga -> {
                    Long stageId = ligas.get(liga);
                    return stageId != null && stageId > 0;
                });
    }

    @AssertTrue(message = "app.whoscored.pausa y timeout deben ser positivos")
    public boolean areDurationsPositive() {
        return pausa != null && !pausa.isNegative() && !pausa.isZero()
                && timeout != null && !timeout.isNegative() && !timeout.isZero();
    }

    @AssertTrue(message = "app.whoscored.reintentos debe ser positivo")
    public boolean areRetriesPositive() {
        return reintentos > 0;
    }
}
