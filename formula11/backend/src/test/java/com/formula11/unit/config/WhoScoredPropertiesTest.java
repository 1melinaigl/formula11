package com.formula11.unit.config;

import com.formula11.config.WhoScoredProperties;
import com.formula11.model.Liga;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WhoScoredPropertiesTest {
    @Test
    void exponeValoresDePlaywrightEnMilisegundos() {
        WhoScoredProperties properties = new WhoScoredProperties();

        assertEquals(3000, properties.getPausaMs());
        assertEquals(90000, properties.getTimeoutMs());

        properties.setPausaMs(2500.5);
        properties.setTimeoutMs(45000.25);

        assertEquals(2500.5, properties.getPausaMs());
        assertEquals(45000.25, properties.getTimeoutMs());
        assertTrue(properties.areRetriesPositive());
        assertTrue(properties.isModoValido());
        assertTrue(properties.isZonaFuenteValida());
        assertTrue(properties.isLigasCompletas());
    }

    @Test
    void rechazaConfiguracionInvalida() {
        WhoScoredProperties properties = new WhoScoredProperties();

        properties.setModo("otro");
        properties.setZonaFuente("zona-inexistente");
        properties.setPausaMs(0);
        properties.setTimeoutMs(-1);
        properties.setReintentos(0);
        properties.setLigas(Map.of(Liga.LA_LIGA, 1L));

        assertFalse(properties.isModoValido());
        assertFalse(properties.isZonaFuenteValida());
        assertFalse(properties.isLigasCompletas());
        assertFalse(properties.areRetriesPositive());
    }

    @Test
    void aceptaConfiguracionCompletaConValoresPositivos() {
        WhoScoredProperties properties = new WhoScoredProperties();
        Map<Liga, Long> ligas = new EnumMap<>(Liga.class);
        for (Liga liga : Liga.values()) {
            ligas.put(liga, 1L);
        }
        properties.setLigas(ligas);
        properties.setInicioTemporada(LocalDate.of(2026, 8, 1));

        assertTrue(properties.isLigasCompletas());
        assertNotNull(properties.getInicioTemporada());
        assertTrue(properties.areRetriesPositive());
    }
}
