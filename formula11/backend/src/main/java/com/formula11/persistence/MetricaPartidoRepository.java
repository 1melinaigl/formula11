package com.formula11.persistence;

import com.formula11.model.MetricaPartido;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface MetricaPartidoRepository extends JpaRepository<MetricaPartido, Long> {
    List<MetricaPartido> findByFechaPartidoBetween(LocalDate desde, LocalDate hasta);
    boolean existsByPartidoExternoId(Long partidoExternoId);
}
