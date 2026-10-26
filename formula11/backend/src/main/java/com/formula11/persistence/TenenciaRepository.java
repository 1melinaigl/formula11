package com.formula11.persistence;

import com.formula11.model.Tenencia;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

public interface TenenciaRepository extends JpaRepository<Tenencia, Long> {
    @EntityGraph(attributePaths = "jugador")
    List<Tenencia> findByUsuarioIdOrderByJugadorIdAsc(Long usuarioId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Tenencia t where t.usuario.id = :usuarioId and t.jugador.id = :jugadorId")
    Optional<Tenencia> findByUsuarioIdAndJugadorIdForUpdate(@Param("usuarioId") Long usuarioId, @Param("jugadorId") Long jugadorId);
    Optional<Tenencia> findByUsuarioIdAndJugadorId(Long usuarioId, Long jugadorId);
    void deleteByUsuarioIdAndJugadorId(Long usuarioId, Long jugadorId);
    long countByUsuarioId(Long usuarioId);
}
