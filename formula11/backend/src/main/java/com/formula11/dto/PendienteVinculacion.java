package com.formula11.dto;

import java.time.Instant;

/** Jugador de WhoScored sin vincular. La liga va con su nombre legible, como exige el contrato. */
public record PendienteVinculacion(String whoscoredId, String nombre, String equipo,
                                   String liga, Instant detectadoEn) {}
