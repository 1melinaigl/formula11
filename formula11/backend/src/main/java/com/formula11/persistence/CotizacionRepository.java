package com.formula11.persistence;

import com.formula11.model.Cotizacion;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.*;

public interface CotizacionRepository extends JpaRepository<Cotizacion, Long> {
    @Query("select max(c.fecha) from Cotizacion c")
    Optional<LocalDate> findMaxFecha();
    Optional<Cotizacion> findTopByJugadorIdAndEstrategiaAndFechaLessThanEqualOrderByFechaDesc(Long jugadorId, String estrategia, LocalDate fecha);
    List<Cotizacion> findByFecha(LocalDate fecha);
    @Query("select c from Cotizacion c where c.jugador.id = :jugadorId and c.estrategia = :estrategia and c.fecha < :fecha order by c.fecha desc")
    List<Cotizacion> findPrevious(@Param("jugadorId") Long jugadorId, @Param("estrategia") String estrategia, @Param("fecha") LocalDate fecha);
    void deleteByFecha(LocalDate fecha);
}
