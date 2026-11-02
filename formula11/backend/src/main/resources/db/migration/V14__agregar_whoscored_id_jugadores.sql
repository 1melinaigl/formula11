ALTER TABLE jugadores ADD COLUMN whoscored_id VARCHAR(100);

CREATE UNIQUE INDEX ux_jugadores_whoscored_id
    ON jugadores (whoscored_id)
    WHERE whoscored_id IS NOT NULL;
