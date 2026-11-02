package com.formula11.config;

import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "mercado")
public class MercadoProperties {
    @NotNull @DecimalMin("0.0")
    private BigDecimal saldoInicial = BigDecimal.valueOf(100);
    @NotBlank
    private String superusuarioEmail;
    @NotBlank
    private String superusuarioPassword;
    private List<String> demoUsuarios = new ArrayList<>(List.of(
            "demo1@formula11.com",
            "demo2@formula11.com",
            "demo3@formula11.com",
            "demo4@formula11.com"));
    private LocalDate demoFechaInicio = LocalDate.of(2026, 8, 1);
    // solo la usa la demo del perfil local se configura en application-local.yml
    private String demoPassword;

    public BigDecimal getSaldoInicial() { return saldoInicial; }
    public void setSaldoInicial(BigDecimal saldoInicial) { this.saldoInicial = saldoInicial; }
    public String getSuperusuarioEmail() { return superusuarioEmail; }
    public void setSuperusuarioEmail(String superusuarioEmail) { this.superusuarioEmail = superusuarioEmail; }
    public String getSuperusuarioPassword() { return superusuarioPassword; }
    public void setSuperusuarioPassword(String superusuarioPassword) { this.superusuarioPassword = superusuarioPassword; }
    public List<String> getDemoUsuarios() { return List.copyOf(demoUsuarios); }
    public void setDemoUsuarios(List<String> demoUsuarios) { this.demoUsuarios = new ArrayList<>(demoUsuarios); }
    public String getDemoPassword() { return demoPassword; }
    public void setDemoPassword(String demoPassword) { this.demoPassword = demoPassword; }
    public LocalDate getDemoFechaInicio() { return demoFechaInicio; }
    public void setDemoFechaInicio(LocalDate demoFechaInicio) { this.demoFechaInicio = demoFechaInicio; }
}
