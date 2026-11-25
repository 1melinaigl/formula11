package com.formula11.unit.controller;

import com.formula11.controller.metricas.MetricasController;
import com.formula11.model.EjecucionMetricas;
import com.formula11.service.metricas.CargaMetricasService;
import com.formula11.service.metricas.PaginaPendientes;
import com.formula11.service.metricas.ResumenEjecucion;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MetricasControllerTest {
    @Test
    void iniciaCargaCon202() throws Exception {
        CargaMetricasService service = mock(CargaMetricasService.class);
        EjecucionMetricasRepositoryFixture fixture = new EjecucionMetricasRepositoryFixture();
        when(service.iniciar(null)).thenReturn(fixture.ejecucion);
        when(service.resumen(any(), anyList())).thenReturn(
                new ResumenEjecucion(1L, "EN_CURSO", fixture.ejecucion.getInicio(),
                        null, List.of(), "corr"));
        MockMvc mvc = mvc(service);

        mvc.perform(post("/api/metricas/sincronizacion"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.estado").value("EN_CURSO"))
                .andExpect(jsonPath("$.correlationId").value("corr"));
    }

    @Test
    void consultaEstadoYPendientes() throws Exception {
        CargaMetricasService service = mock(CargaMetricasService.class);
        EjecucionMetricas ejecucion = new EjecucionMetricas("COMPLETADA",
                Instant.parse("2026-10-28T00:00:00Z"), "corr");
        when(service.estado(7L)).thenReturn(
                new ResumenEjecucion(7L, "COMPLETADA", ejecucion.getInicio(),
                        null, List.of(), "corr"));
        when(service.pendientes(any())).thenReturn(new PaginaPendientes(List.of(), 0, 20, 0, 0));
        MockMvc mvc = mvc(service);

        mvc.perform(get("/api/metricas/sincronizacion/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADA"));
        mvc.perform(get("/api/metricas/pendientes?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    private static MockMvc mvc(CargaMetricasService service) {
        return MockMvcBuilders.standaloneSetup(new MetricasController(service)).build();
    }

    private static final class EjecucionMetricasRepositoryFixture {
        private final EjecucionMetricas ejecucion = new EjecucionMetricas("EN_CURSO",
                Instant.parse("2026-10-28T00:00:00Z"), "corr");
    }
}
