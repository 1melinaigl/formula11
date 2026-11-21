package com.formula11.adapter.whoscored;

import com.formula11.model.Liga;

import java.time.ZonedDateTime;

public record DatosPartidoWhoScored(long idExterno, Liga liga, ZonedDateTime startTime,
                                    String matchCentreData,
                                    String matchCentreEventTypeJson) {}
