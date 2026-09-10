package com.formula11.model;

public enum Liga {
    PREMIER_LEAGUE("Premier League"),
    BUNDESLIGA("Bundesliga"),
    LA_LIGA("La Liga"),
    SERIE_A("Serie A"),
    LIGUE_1("Ligue 1");

    private final String nombreLegible;

    Liga(String nombreLegible) {
        this.nombreLegible = nombreLegible;
    }

    public String getNombreLegible() {
        return nombreLegible;
    }
}
