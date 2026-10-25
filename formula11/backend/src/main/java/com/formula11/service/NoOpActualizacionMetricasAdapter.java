package com.formula11.service;

import org.springframework.stereotype.Component;

@Component
public class NoOpActualizacionMetricasAdapter implements ActualizacionMetricasPort {
    @Override
    public void actualizar() {}
}
