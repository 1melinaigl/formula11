package com.formula11.unit.controller;

import com.formula11.controller.CotizacionController;
import com.formula11.dto.*;
import com.formula11.service.RecalculoService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CotizacionControllerTest {
    @Test
    void exponeElRecálculoConRespuesta200() throws Exception {
        RecalculoService service = mock(RecalculoService.class);
        when(service.recalcular(any())).thenReturn(new RecalculoResponse("COMPLETADO", 1, 2, 0,
                List.of(), Instant.now(), Instant.now(), "corr-test"));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new CotizacionController(service)).build();

        mvc.perform(post("/api/cotizaciones/recalcular")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADO"))
                .andExpect(jsonPath("$.correlationId").value("corr-test"));
    }

}
