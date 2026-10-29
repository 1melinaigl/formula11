CREATE SEQUENCE pendiente_vinculacion_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE pendiente_vinculacion (
    id BIGINT NOT NULL DEFAULT nextval('pendiente_vinculacion_seq') PRIMARY KEY,
    fuente VARCHAR(50) NOT NULL,
    whoscored_id VARCHAR(100),
    nombre VARCHAR(150) NOT NULL,
    equipo VARCHAR(150),
    liga VARCHAR(30) NOT NULL CHECK (liga IN ('PREMIER_LEAGUE', 'BUNDESLIGA', 'LA_LIGA', 'SERIE_A', 'LIGUE_1')),
    partido_externo_id BIGINT REFERENCES partido_externo(id),
    fecha_registro TIMESTAMPTZ NOT NULL,
    resuelto BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX ix_pendiente_vinculacion_liga_resuelto
    ON pendiente_vinculacion (liga, resuelto);
