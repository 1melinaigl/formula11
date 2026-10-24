package com.formula11.adapter;

import com.formula11.model.Jugador;
import com.formula11.service.CotizacionVigentePort;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.Instant;

@Component
public class CotizacionFijaAdapter implements CotizacionVigentePort {
    @Override
    public Cotizacion vigente(Jugador jugador, Instant fecha) {
        return new Cotizacion(new BigDecimal("1.0000"), null, null, null);
    }
}
