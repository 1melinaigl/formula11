package com.formula11.persistence;

import com.formula11.model.Estrategia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EstrategiaRepository extends JpaRepository<Estrategia, Long> {
    List<Estrategia> findByCodigoOrderByVersionDesc(String codigo);
    Optional<Estrategia> findTopByCodigoOrderByVersionDesc(String codigo);
    Optional<Estrategia> findByCodigoAndVersion(String codigo, int version);
    Optional<Estrategia> findByActivaTrue();
}
