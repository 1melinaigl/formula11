INSERT INTO cuentas (usuario_id, saldo)
SELECT u.id, 100.0000
FROM usuarios u
WHERE u.rol = 'USUARIO'
  AND NOT EXISTS (SELECT 1 FROM cuentas c WHERE c.usuario_id = u.id);
