package com.formula11.persistence;

import com.formula11.model.Liga;
import com.formula11.model.PendienteVinculacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PendienteVinculacionRepository extends JpaRepository<PendienteVinculacion, Long> {
    Page<PendienteVinculacion> findByResueltoFalse(Pageable pageable);
    Page<PendienteVinculacion> findByLigaAndResueltoFalse(Liga liga, Pageable pageable);
}
