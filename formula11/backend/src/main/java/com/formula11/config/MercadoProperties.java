package com.formula11.config;

import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import java.math.BigDecimal;

@Validated
@ConfigurationProperties(prefix = "mercado")
public class MercadoProperties {
    @NotNull @DecimalMin("0.0")
    private BigDecimal saldoInicial = BigDecimal.valueOf(100);
    @NotBlank
    private String superusuarioEmail;
    @NotBlank
    private String superusuarioPassword;

    public BigDecimal getSaldoInicial() { return saldoInicial; }
    public void setSaldoInicial(BigDecimal saldoInicial) { this.saldoInicial = saldoInicial; }
    public String getSuperusuarioEmail() { return superusuarioEmail; }
    public void setSuperusuarioEmail(String superusuarioEmail) { this.superusuarioEmail = superusuarioEmail; }
    public String getSuperusuarioPassword() { return superusuarioPassword; }
    public void setSuperusuarioPassword(String superusuarioPassword) { this.superusuarioPassword = superusuarioPassword; }
}
