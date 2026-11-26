package com.formula11.persistence.metricas;

import com.formula11.model.metricas.EjecucionMetricas;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EjecucionMetricasRepository extends JpaRepository<EjecucionMetricas, Long> {
    List<EjecucionMetricas> findByEstado(String estado);

    /** La carga con el fin más reciente entre los estados pedidos (016, health de la carga de métricas). */
    Optional<EjecucionMetricas> findFirstByEstadoInAndFinNotNullOrderByFinDescIdDesc(Collection<String> estados);
}
