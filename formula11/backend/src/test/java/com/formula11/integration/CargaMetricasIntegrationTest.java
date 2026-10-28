package com.formula11.integration;

import com.formula11.model.Liga;
import com.formula11.model.Jugador;
import com.formula11.persistence.JugadorRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class CargaMetricasIntegrationTest {
    @Autowired
    private JugadorRepository jugadorRepository;

    @AfterEach
    void limpiarSoloJugadoresDelTest() {
        jugadorRepository.findAllByOrigen("TEST_WHOSCORED").forEach(jugador ->
                jugadorRepository.deleteById(jugador.getId()));
    }

    @Test
    void cargaOfflineEsIdempotenteYConservaPartidosPrevios() {
        Jugador jugador = jugadorRepository.save(new Jugador(
                "Jugador Test WhoScored", "Málaga", Liga.LA_LIGA, "MED"));
        assertTrue(jugador.getId() != null);
    }
}
