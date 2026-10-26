package com.formula11.persistence;

import com.formula11.model.Transaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {
    boolean existsByJugadorId(Long jugadorId);

    @Query("""
            select t from Transaccion t
            where t.usuario.id = :usuarioId
              and (:tipo is null or t.tipo = :tipo)
              and (:jugadorId is null or t.jugador.id = :jugadorId)
            """)
    Page<Transaccion> buscarHistorial(@Param("usuarioId") Long usuarioId,
                                      @Param("tipo") Transaccion.Tipo tipo,
                                      @Param("jugadorId") Long jugadorId,
                                      Pageable pageable);

    List<Transaccion> findByUsuarioIdOrderByFechaAscIdAsc(Long usuarioId);

    @Query("select min(t.fecha) from Transaccion t where t.usuario.id = :usuarioId")
    Optional<Instant> findFirstFechaByUsuarioId(@Param("usuarioId") Long usuarioId);
}
