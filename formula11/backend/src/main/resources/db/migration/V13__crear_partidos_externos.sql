CREATE SEQUENCE partido_externo_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE partido_externo (
    id BIGINT NOT NULL DEFAULT nextval('partido_externo_seq') PRIMARY KEY,
    fuente VARCHAR(50) NOT NULL,
    id_externo VARCHAR(100) NOT NULL,
    liga VARCHAR(30) NOT NULL CHECK (liga IN ('PREMIER_LEAGUE', 'BUNDESLIGA', 'LA_LIGA', 'SERIE_A', 'LIGUE_1')),
    fecha_partido DATE NOT NULL,
    equipo_local VARCHAR(150),
    equipo_visitante VARCHAR(150),
    fecha_carga TIMESTAMPTZ NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'CARGADO',
    error TEXT,
    CONSTRAINT ux_partido_externo_fuente_id UNIQUE (fuente, id_externo)
);

CREATE INDEX ix_partido_externo_liga_fecha ON partido_externo (liga, fecha_partido);
