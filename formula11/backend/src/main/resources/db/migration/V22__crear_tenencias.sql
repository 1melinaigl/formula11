CREATE TABLE tenencias (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    jugador_id BIGINT NOT NULL REFERENCES jugadores(id),
    cantidad INTEGER NOT NULL CHECK (cantidad >= 0),
    precio_promedio NUMERIC(19,4) NOT NULL CHECK (precio_promedio >= 0),
    CONSTRAINT ux_tenencias_usuario_jugador UNIQUE (usuario_id, jugador_id)
);
CREATE INDEX ix_tenencias_jugador ON tenencias (jugador_id);
