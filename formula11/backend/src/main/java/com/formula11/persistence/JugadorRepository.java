package com.formula11.persistence;

import com.formula11.model.Jugador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;
import java.util.List;
import com.formula11.model.Liga;

public interface JugadorRepository extends JpaRepository<Jugador, Long>, JpaSpecificationExecutor<Jugador> {
    List<Jugador> findAllByOrderByNombreAsc();
    Optional<Jugador> findByExternalProviderAndExternalId(String externalProvider, String externalId);
    void deleteAllByOrigen(String origen);
    List<Jugador> findAllByOrigen(String origen);
    java.util.Optional<Jugador> findByWhoscoredId(String whoscoredId);
    List<Jugador> findByLigaAndNombreIgnoreCase(Liga liga, String nombre);
    List<Jugador> findByLiga(Liga liga);
}
