-- 018: índice para la página del ranking; permite leer ordenado por score y precio dentro de (estrategia, fecha).
CREATE INDEX IF NOT EXISTS ix_cotizacion_ranking
    ON cotizacion (estrategia, fecha, score DESC, precio DESC);
