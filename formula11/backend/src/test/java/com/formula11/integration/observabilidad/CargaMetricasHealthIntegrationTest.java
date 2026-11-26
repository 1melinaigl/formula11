package com.formula11.integration.observabilidad;

import com.formula11.model.metricas.EjecucionMetricas;
import com.formula11.observabilidad.CargaMetricasHealthIndicator;
import com.formula11.persistence.metricas.EjecucionMetricasRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * El health indicator de la carga de métricas contra PostgreSQL. Las cargas del test terminan en 2100
 * para quedar últimas aunque otros tests de la suite hayan guardado cargas, y se borran al final.
 */
@SpringBootTest
@ActiveProfiles("test")
class CargaMetricasHealthIntegrationTest {
    private static final Instant INICIO = Instant.parse("2100-01-01T06:00:00Z");

    @Autowired private CargaMetricasHealthIndicator indicador;
    @Autowired private EjecucionMetricasRepository ejecuciones;

    private final List<Long> creadas = new ArrayList<>();

    @AfterEach
    void borrarCargasDelTest() {
        ejecuciones.deleteAllById(creadas);
    }

    @Test
    void unaCargaFallidaLoDejaCaidoYUnaCompletadaPosteriorLoLevanta() {
        guardarFallida(INICIO.plusSeconds(60));
        guardar(new EjecucionMetricas("EN_CURSO", INICIO.plusSeconds(120), "health-en-curso"));

        Health caido = indicador.health();
        assertEquals(Status.DOWN, caido.getStatus(), caido.getDetails().toString());
        assertEquals("FALLIDA", caido.getDetails().get("estado"));

        EjecucionMetricas completada = new EjecucionMetricas("EN_CURSO", INICIO.plusSeconds(180), "health-ok");
        completada.completar("COMPLETADA", INICIO.plusSeconds(240));
        guardar(completada);

        Health arriba = indicador.health();
        assertEquals(Status.UP, arriba.getStatus(), arriba.getDetails().toString());
        assertEquals("COMPLETADA", arriba.getDetails().get("estado"));
        assertEquals(completada.getId(), arriba.getDetails().get("ejecucionId"));
    }

    private void guardarFallida(Instant fin) {
        EjecucionMetricas fallida = new EjecucionMetricas("EN_CURSO", INICIO, "health-fallida");
        fallida.fallar(fin, "Falla de prueba");
        guardar(fallida);
    }

    private void guardar(EjecucionMetricas ejecucion) {
        creadas.add(ejecuciones.save(ejecucion).getId());
    }
}
