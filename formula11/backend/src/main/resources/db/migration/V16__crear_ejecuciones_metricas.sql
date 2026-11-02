CREATE SEQUENCE ejecucion_metricas_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE ejecucion_metrica_liga_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE ejecucion_metricas (
    id BIGINT NOT NULL DEFAULT nextval('ejecucion_metricas_seq') PRIMARY KEY,
    estado VARCHAR(30) NOT NULL,
    inicio TIMESTAMPTZ NOT NULL,
    fin TIMESTAMPTZ,
    correlation_id VARCHAR(100),
    error TEXT
);

CREATE TABLE ejecucion_metrica_liga (
    id BIGINT NOT NULL DEFAULT nextval('ejecucion_metrica_liga_seq') PRIMARY KEY,
    ejecucion_id BIGINT NOT NULL REFERENCES ejecucion_metricas(id),
    liga VARCHAR(30) NOT NULL CHECK (liga IN ('PREMIER_LEAGUE', 'BUNDESLIGA', 'LA_LIGA', 'SERIE_A', 'LIGUE_1')),
    partidos_encontrados INTEGER NOT NULL DEFAULT 0,
    partidos_cargados INTEGER NOT NULL DEFAULT 0,
    partidos_fallidos INTEGER NOT NULL DEFAULT 0,
    jugadores_vinculados INTEGER NOT NULL DEFAULT 0,
    pendientes INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT ux_ejecucion_metrica_liga UNIQUE (ejecucion_id, liga)
);
