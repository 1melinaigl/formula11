ALTER TABLE jugadores ADD COLUMN external_provider VARCHAR(50);
ALTER TABLE jugadores ADD COLUMN external_id VARCHAR(100);
ALTER TABLE jugadores ADD COLUMN origen VARCHAR(20) NOT NULL DEFAULT 'LEGACY';

CREATE UNIQUE INDEX ux_jugadores_external_identity
    ON jugadores (external_provider, external_id)
    WHERE external_provider IS NOT NULL AND external_id IS NOT NULL;