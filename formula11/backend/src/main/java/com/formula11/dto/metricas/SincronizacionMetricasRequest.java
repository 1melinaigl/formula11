package com.formula11.dto.metricas;

import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public record SincronizacionMetricasRequest(@PastOrPresent LocalDate desde) {}
