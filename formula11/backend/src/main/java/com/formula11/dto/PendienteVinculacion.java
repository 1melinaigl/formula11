package com.formula11.dto;

import com.formula11.model.Liga;

import java.time.Instant;

public record PendienteVinculacion(String whoscoredId, String nombre, String equipo,
                                   Liga liga, Instant detectadoEn) {}
