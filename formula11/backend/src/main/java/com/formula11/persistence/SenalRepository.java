package com.formula11.persistence;

import com.formula11.model.Senal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SenalRepository extends JpaRepository<Senal, Long> {
    List<Senal> findByFecha(LocalDate fecha);

    @Query("select max(s.fecha) from Senal s")
    Optional<LocalDate> findMaxFecha();

    @EntityGraph(attributePaths = "jugador")
    Page<Senal> findByFecha(LocalDate fecha, Pageable pageable);

    @EntityGraph(attributePaths = "jugador")
    Page<Senal> findAllBy(Pageable pageable);
}
