package com.formula11.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoOpActualizacionMetricasAdapter implements ActualizacionMetricasPort {
    private static final Logger log = LoggerFactory.getLogger(NoOpActualizacionMetricasAdapter.class);

    @Override
    public void actualizar() {
        log.info("Actualización de métricas no implementada; se usarán las métricas persistidas");
    }
}
