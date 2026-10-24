package com.formula11.persistence;

import com.formula11.model.Cuenta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cuenta c where c.usuario.id = :usuarioId")
    Optional<Cuenta> findByUsuarioIdForUpdate(@Param("usuarioId") Long usuarioId);
    Optional<Cuenta> findByUsuarioId(Long usuarioId);
}
