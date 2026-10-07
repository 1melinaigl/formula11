package com.formula11.persistence;

import com.formula11.model.Jugador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;
import java.util.List;

public interface JugadorRepository extends JpaRepository<Jugador, Long>, JpaSpecificationExecutor<Jugador> {
    List<Jugador> findAllByOrderByNombreAsc();
    Optional<Jugador> findByExternalProviderAndExternalId(String externalProvider, String externalId);
    void deleteAllByOrigen(String origen);
}
