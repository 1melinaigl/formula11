package com.formula11.persistence;

import com.formula11.model.EjecucionMetricaLiga;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EjecucionMetricaLigaRepository extends JpaRepository<EjecucionMetricaLiga, Long> {
    List<EjecucionMetricaLiga> findByEjecucionId(Long ejecucionId);
}
