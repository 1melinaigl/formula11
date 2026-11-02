CREATE TABLE auditoria_mercado (
    id BIGSERIAL PRIMARY KEY,
    autor_id BIGINT NOT NULL REFERENCES usuarios(id),
    operacion_id BIGINT NOT NULL UNIQUE REFERENCES transacciones(id),
    antes JSONB NOT NULL,
    despues JSONB NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL
);
