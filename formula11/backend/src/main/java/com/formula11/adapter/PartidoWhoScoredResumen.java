package com.formula11.adapter;

import com.formula11.model.Liga;

import java.time.ZonedDateTime;

public record PartidoWhoScoredResumen(long idExterno, ZonedDateTime startTime,
                                      String equipoLocal, String equipoVisitante,
                                      String elapsed, Liga liga) {}
