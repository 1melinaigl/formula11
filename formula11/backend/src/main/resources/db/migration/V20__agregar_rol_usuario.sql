ALTER TABLE usuarios ADD COLUMN rol VARCHAR(20);
UPDATE usuarios SET rol = 'USUARIO' WHERE rol IS NULL;
ALTER TABLE usuarios ALTER COLUMN rol SET NOT NULL;
ALTER TABLE usuarios ADD CONSTRAINT ck_usuarios_rol CHECK (rol IN ('USUARIO', 'SUPERUSUARIO'));
CREATE UNIQUE INDEX ux_usuarios_superusuario ON usuarios (rol) WHERE rol = 'SUPERUSUARIO';
