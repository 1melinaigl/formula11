package com.formula11.e2e;

import com.formula11.dto.RecalculoResponse;
import com.formula11.dto.RecalculoRequest;
import com.formula11.service.RecalculoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class CotizacionE2ETest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecalculoService recalculo;

    @Test
    void recalculateRequiereAutenticacion() throws Exception {
        mockMvc.perform(post("/api/cotizaciones/recalcular"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    @WithMockUser(roles = "USUARIO")
    void usuarioNoPuedeRecalcular() throws Exception {
        mockMvc.perform(post("/api/cotizaciones/recalcular"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").exists())
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    @WithMockUser(roles = "SUPERUSUARIO")
    void rangoInvalidoDevuelve400ConErrorContractual() throws Exception {
        when(recalculo.recalcular(any(RecalculoRequest.class)))
                .thenAnswer(invocation -> {
                    RecalculoRequest request = invocation.getArgument(0);
                    if (!request.rangoValido()) {
                        throw new IllegalArgumentException("Desde y hasta deben enviarse juntas y desde no puede ser posterior a hasta");
                    }
                    return null;
                });
        mockMvc.perform(post("/api/cotizaciones/recalcular")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"desde":"2026-01-19","hasta":"2026-01-11"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    @WithMockUser(roles = "SUPERUSUARIO")
    void recalculateExitosoDevuelveEsquemaContractual() throws Exception {
        when(recalculo.recalcular(isNull())).thenReturn(new RecalculoResponse(
                "COMPLETADO", 1, 4, 0,
                List.of(new RecalculoResponse.EstrategiaVersionResponse("RENDIMIENTO_POSICION", 1)),
                Instant.parse("2026-01-11T00:00:00Z"),
                Instant.parse("2026-01-11T00:01:00Z"), "corr-e2e"));

        mockMvc.perform(post("/api/cotizaciones/recalcular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADO"))
                .andExpect(jsonPath("$.semanasCalculadas").value(1))
                .andExpect(jsonPath("$.jugadoresCotizados").value(4))
                .andExpect(jsonPath("$.estrategias[0].codigo").value("RENDIMIENTO_POSICION"))
                .andExpect(jsonPath("$.inicio").exists())
                .andExpect(jsonPath("$.fin").exists())
                .andExpect(jsonPath("$.correlationId").value("corr-e2e"));
    }

    @Test
    @WithMockUser(roles = "SUPERUSUARIO")
    void conflictoDeRecacluloDevuelve409() throws Exception {
        when(recalculo.recalcular(any(RecalculoRequest.class)))
                .thenThrow(new IllegalStateException("Ya hay un recálculo en curso"));

        mockMvc.perform(post("/api/cotizaciones/recalcular")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"desde":"2026-01-04","hasta":"2026-01-18"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya hay un recálculo en curso"))
                .andExpect(jsonPath("$.correlationId").exists());
    }
}
