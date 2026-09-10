CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    fecha_registro TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX ux_usuarios_email_normalizado ON usuarios (LOWER(BTRIM(email)));

CREATE TABLE jugadores (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    equipo VARCHAR(150) NOT NULL,
    liga VARCHAR(30) NOT NULL CHECK (liga IN ('PREMIER_LEAGUE', 'BUNDESLIGA', 'LA_LIGA', 'SERIE_A', 'LIGUE_1')),
    posicion VARCHAR(80) NOT NULL
);
