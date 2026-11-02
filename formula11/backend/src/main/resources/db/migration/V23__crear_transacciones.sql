CREATE TABLE transacciones (
    id BIGSERIAL PRIMARY KEY,
    tipo VARCHAR(10) NOT NULL CHECK (tipo IN ('COMPRA', 'VENTA')),
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    jugador_id BIGINT NOT NULL REFERENCES jugadores(id),
    cantidad INTEGER NOT NULL CHECK (cantidad > 0),
    precio_unitario NUMERIC(19,4) NOT NULL CHECK (precio_unitario >= 0),
    total NUMERIC(19,4) NOT NULL CHECK (total >= 0),
    contraparte_id BIGINT NOT NULL REFERENCES usuarios(id),
    fecha TIMESTAMPTZ NOT NULL,
    estrategia VARCHAR(255),
    version_cotizacion INTEGER
);
CREATE INDEX ix_transacciones_usuario_fecha ON transacciones (usuario_id, fecha DESC);
