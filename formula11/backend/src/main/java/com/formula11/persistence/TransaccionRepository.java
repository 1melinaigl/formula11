package com.formula11.persistence;

import com.formula11.model.Transaccion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {
    boolean existsByJugadorId(Long jugadorId);
}
