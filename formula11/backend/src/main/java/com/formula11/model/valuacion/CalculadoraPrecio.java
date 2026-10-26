package com.formula11.model.valuacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** Applies the weekly price formula without depending on persistence or Spring. */
public final class CalculadoraPrecio {
    private CalculadoraPrecio() {
    }

    public static PrecioCalculado calcular(BigDecimal anterior, BigDecimal score,
                                            BigDecimal sensibilidad, BigDecimal tope,
                                            BigDecimal piso, boolean jugo) {
        Objects.requireNonNull(score, "El score es obligatorio");
        boolean primera = anterior == null;
        anterior = primera ? BigDecimal.ONE : anterior;
        sensibilidad = sensibilidad == null ? new BigDecimal("0.4") : sensibilidad;
        tope = tope == null ? new BigDecimal("0.20") : tope.abs();
        piso = piso == null ? new BigDecimal("0.10") : piso;
        if (!jugo) return new PrecioCalculado(anterior.max(piso).setScale(4, RoundingMode.HALF_UP), null);
        BigDecimal variacion = sensibilidad.multiply(score.subtract(new BigDecimal("0.5")));
        variacion = variacion.max(tope.negate()).min(tope);
        BigDecimal precio = anterior.multiply(BigDecimal.ONE.add(variacion));
        precio = precio.max(piso).setScale(4, RoundingMode.HALF_UP);
        return new PrecioCalculado(precio, primera || anterior.signum() == 0 ? null
                : precio.divide(anterior, 12, RoundingMode.HALF_UP).subtract(BigDecimal.ONE)
                .setScale(4, RoundingMode.HALF_UP));
    }

    public static PrecioCalculado calcular(BigDecimal anterior, BigDecimal score,
                                            ParametrosPrecio parametros, boolean jugo) {
        return calcular(anterior, score, parametros.sensibilidad(), parametros.topeVariacion(),
                parametros.piso(), jugo);
    }
}
