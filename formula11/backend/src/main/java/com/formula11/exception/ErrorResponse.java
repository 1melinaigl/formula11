package com.formula11.exception;

import java.time.Instant;

public record ErrorResponse(String mensaje, Instant timestamp, String correlationId) { }
