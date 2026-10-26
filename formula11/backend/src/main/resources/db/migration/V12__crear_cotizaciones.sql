CREATE SEQUENCE cotizacion_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE cotizacion (
    id BIGINT NOT NULL DEFAULT nextval('cotizacion_seq') PRIMARY KEY,
    jugador_id BIGINT NOT NULL REFERENCES jugadores(id),
    fecha DATE NOT NULL,
    precio NUMERIC(19, 4) NOT NULL,
    score NUMERIC(19, 8) NOT NULL,
    estrategia VARCHAR(80) NOT NULL,
    version INTEGER NOT NULL,
    pesos JSONB NOT NULL,
    CONSTRAINT ux_cotizacion_jugador_estrategia_fecha UNIQUE (jugador_id, estrategia, fecha)
);

CREATE INDEX ix_cotizacion_jugador_fecha ON cotizacion (jugador_id, fecha DESC);
