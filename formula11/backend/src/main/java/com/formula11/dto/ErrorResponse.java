package com.formula11.dto;

import java.time.Instant;

public record ErrorResponse(String mensaje, Instant timestamp, String correlationId) { }
