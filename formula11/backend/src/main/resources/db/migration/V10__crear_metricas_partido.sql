CREATE SEQUENCE metrica_partido_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE metrica_partido (
    id BIGINT NOT NULL DEFAULT nextval('metrica_partido_seq') PRIMARY KEY,
    jugador_id BIGINT NOT NULL REFERENCES jugadores(id),
    fecha_partido DATE NOT NULL,
    rival VARCHAR(150),
    minutos INTEGER,
    titular BOOLEAN,
    goles INTEGER NOT NULL DEFAULT 0,
    asistencias INTEGER NOT NULL DEFAULT 0,
    tiros INTEGER NOT NULL DEFAULT 0,
    pases_clave INTEGER NOT NULL DEFAULT 0,
    regates INTEGER NOT NULL DEFAULT 0,
    entradas INTEGER NOT NULL DEFAULT 0,
    intercepciones INTEGER NOT NULL DEFAULT 0,
    amarillas INTEGER NOT NULL DEFAULT 0,
    rojas INTEGER NOT NULL DEFAULT 0,
    atajadas INTEGER NOT NULL DEFAULT 0,
    goles_recibidos INTEGER NOT NULL DEFAULT 0,
    valla_invicta INTEGER NOT NULL DEFAULT 0,
    rating NUMERIC(8, 4),
    fuente VARCHAR(100),
    CONSTRAINT ux_metrica_partido_jugador_fecha UNIQUE (jugador_id, fecha_partido)
);

CREATE INDEX ix_metrica_partido_jugador_fecha ON metrica_partido (jugador_id, fecha_partido);
