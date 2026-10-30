-- 008: índice de lectura para el ranking y las fechas de cotización por estrategia.
CREATE INDEX IF NOT EXISTS ix_cotizacion_estrategia_fecha ON cotizacion (estrategia, fecha);
