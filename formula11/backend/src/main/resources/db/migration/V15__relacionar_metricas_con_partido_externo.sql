ALTER TABLE metrica_partido ADD COLUMN partido_externo_id BIGINT;

ALTER TABLE metrica_partido
    ADD CONSTRAINT fk_metrica_partido_externo
    FOREIGN KEY (partido_externo_id) REFERENCES partido_externo(id);

CREATE INDEX ix_metrica_partido_externo ON metrica_partido (partido_externo_id);
