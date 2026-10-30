package com.formula11.persistence;

import com.formula11.model.Cotizacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.*;

public interface CotizacionRepository extends JpaRepository<Cotizacion, Long>, JpaSpecificationExecutor<Cotizacion> {
    @Query("select max(c.fecha) from Cotizacion c")
    Optional<LocalDate> findMaxFecha();
    Optional<Cotizacion> findTopByJugadorIdAndEstrategiaAndFechaLessThanEqualOrderByFechaDesc(Long jugadorId, String estrategia, LocalDate fecha);
    List<Cotizacion> findByJugadorIdAndEstrategiaOrderByFechaAsc(Long jugadorId, String estrategia);
    List<Cotizacion> findByFecha(LocalDate fecha);
    List<Cotizacion> findByFechaLessThanOrderByFechaDesc(LocalDate fecha);
    @Query("select c from Cotizacion c where c.jugador.id = :jugadorId and c.estrategia = :estrategia and c.fecha < :fecha order by c.fecha desc")
    List<Cotizacion> findPrevious(@Param("jugadorId") Long jugadorId, @Param("estrategia") String estrategia, @Param("fecha") LocalDate fecha);
    void deleteByFecha(LocalDate fecha);

    // 008: consultas de lectura para detalle, historial, ranking y CotizacionHistoricaPort.
    @Query("select max(c.fecha) from Cotizacion c where c.estrategia = :estrategia")
    Optional<LocalDate> findMaxFechaByEstrategia(@Param("estrategia") String estrategia);

    @Query("select max(c.fecha) from Cotizacion c where c.estrategia = :estrategia and c.fecha < :fecha")
    Optional<LocalDate> findFechaAnterior(@Param("estrategia") String estrategia, @Param("fecha") LocalDate fecha);

    @Query("select max(c.version) from Cotizacion c where c.estrategia = :estrategia and c.fecha = :fecha")
    Optional<Integer> findVersionByEstrategiaAndFecha(@Param("estrategia") String estrategia, @Param("fecha") LocalDate fecha);

    List<Cotizacion> findByEstrategiaAndFechaAndJugadorIdIn(String estrategia, LocalDate fecha, Collection<Long> jugadorIds);

    @Query("select distinct c.fecha from Cotizacion c where c.estrategia = :estrategia "
            + "and c.fecha between :desde and :hasta order by c.fecha")
    List<LocalDate> findFechasByEstrategiaBetween(@Param("estrategia") String estrategia,
                                                  @Param("desde") LocalDate desde,
                                                  @Param("hasta") LocalDate hasta);

    @Query("select c from Cotizacion c where c.estrategia = :estrategia and c.jugador.id in :ids "
            + "and c.fecha = (select max(c2.fecha) from Cotizacion c2 where c2.jugador.id = c.jugador.id "
            + "and c2.estrategia = :estrategia and c2.fecha <= :fecha)")
    List<Cotizacion> findVigentesA(@Param("estrategia") String estrategia,
                                   @Param("ids") Collection<Long> ids,
                                   @Param("fecha") LocalDate fecha);

    @Override
    @EntityGraph(attributePaths = "jugador")
    Page<Cotizacion> findAll(Specification<Cotizacion> spec, Pageable pageable);
}
