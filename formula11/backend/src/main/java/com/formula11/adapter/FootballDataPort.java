package com.formula11.adapter;

import com.formula11.model.Liga;
import java.util.List;

public interface FootballDataPort {
    boolean estaConfigurado();
    List<ExternalTeam> obtenerEquipos(Liga liga);
    List<ExternalPlayer> obtenerJugadores(ExternalTeam equipo);
}
