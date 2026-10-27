package com.formula11.persistence;

import com.formula11.model.EjecucionMetricas;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EjecucionMetricasRepository extends JpaRepository<EjecucionMetricas, Long> {
    List<EjecucionMetricas> findByEstado(String estado);
}
