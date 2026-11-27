package com.formula11.service.cotizaciones;

import com.formula11.model.jugadores.Liga;

public record RankingClave(String estrategia, Liga liga, String posicion, int page, int size) {
}
