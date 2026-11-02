package com.formula11.service;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class ActualizacionMetricasAdapter implements ActualizacionMetricasPort {
    private final CargaMetricasService cargaMetricasService;

    public ActualizacionMetricasAdapter(CargaMetricasService cargaMetricasService) {
        this.cargaMetricasService = cargaMetricasService;
    }

    @Override
    public void actualizar() {
        cargaMetricasService.actualizarIncremental();
    }
}
