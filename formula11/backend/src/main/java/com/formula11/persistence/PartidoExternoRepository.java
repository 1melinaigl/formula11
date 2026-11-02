package com.formula11.persistence;

import com.formula11.model.Liga;
import com.formula11.model.PartidoExterno;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PartidoExternoRepository extends JpaRepository<PartidoExterno, Long> {
    boolean existsByFuenteAndIdExterno(String fuente, String idExterno);
    Optional<PartidoExterno> findByFuenteAndIdExterno(String fuente, String idExterno);
    List<PartidoExterno> findByLigaAndFechaPartidoBetween(Liga liga, LocalDate desde, LocalDate hasta);
}
