CREATE TABLE cuentas (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL UNIQUE REFERENCES usuarios(id),
    saldo NUMERIC(19,4) NOT NULL CHECK (saldo >= 0)
);
