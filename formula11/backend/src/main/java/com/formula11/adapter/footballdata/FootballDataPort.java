package com.formula11.adapter.footballdata;

import com.formula11.model.jugadores.Liga;
import java.util.List;

public interface FootballDataPort {
    boolean estaConfigurado();
    List<ExternalTeam> obtenerEquipos(Liga liga);
    List<ExternalPlayer> obtenerJugadores(ExternalTeam equipo);
}
