package com.formula11.persistence;

import com.formula11.model.MetricaPartido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface MetricaPartidoRepository extends JpaRepository<MetricaPartido, Long> {
    List<MetricaPartido> findByFechaPartidoBetween(LocalDate desde, LocalDate hasta);
    boolean existsByPartidoExternoId(Long partidoExternoId);

    // 010: jugadores que jugaron (minutos mayores que cero) entre dos fechas inclusive.
    @Query("select distinct m.jugador.id from MetricaPartido m "
            + "where m.fechaPartido between :desde and :hasta and m.minutos > 0")
    List<Long> findJugadoresQueJugaron(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
