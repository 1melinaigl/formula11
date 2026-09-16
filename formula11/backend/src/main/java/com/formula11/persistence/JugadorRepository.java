package com.formula11.persistence;

import com.formula11.model.Jugador;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JugadorRepository extends JpaRepository<Jugador, Long> {
    List<Jugador> findAllByOrderByNombreAsc();
}
