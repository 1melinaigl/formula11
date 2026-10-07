package com.formula11.unit.adapter;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.formula11.adapter.FootballApiProperties;
import com.formula11.adapter.FootballDataAdapter;
import com.formula11.adapter.FootballDataException;
import com.formula11.model.Liga;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FootballDataAdapterTest {
    @Test
    void apikeyAusenteNoIntentaLlamarAlProveedor() {
        FootballApiProperties properties = new FootballApiProperties();
        properties.setApiKey("  ");
        FootballDataAdapter adapter = new FootballDataAdapter(properties, Caffeine.newBuilder().build());

        FootballDataException exception = assertThrows(FootballDataException.class, () -> adapter.obtenerEquipos(Liga.LA_LIGA));

        assertEquals(FootballDataException.Tipo.NO_CONFIGURADO, exception.getTipo());
    }
}
