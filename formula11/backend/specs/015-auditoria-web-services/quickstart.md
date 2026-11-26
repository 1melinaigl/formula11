# Quickstart: Auditoría de web services

## Requisitos

- Docker levantado (PostgreSQL local y Testcontainers).
- Backend en `formula11/backend`.

## Tests

```powershell
mvn test -Punitarios -Dtest="*observabilidad*"
mvn test -Pintegracion -Dtest="Auditoria*"
mvn test -Pe2e -Dtest="Auditoria*"
mvn verify
```

Lo esperado: todos en verde y ningún test existente cambia de resultado (SC-004).

## Prueba manual

1. Levantar el backend con el perfil `local`.
2. Hacer un login con una contraseña conocida:

   ```powershell
   curl.exe -s -X POST http://localhost:8080/api/usuarios/login -H "Content-Type: application/json" -H "X-Correlation-Id: demo-123" -d '{\"email\":\"ana@mail.com\",\"password\":\"Secreta123\"}'
   ```

3. Buscar la línea:

   ```powershell
   Select-String -Path logs\auditoria.log -Pattern "demo-123"
   ```

   Tiene que aparecer una sola línea con `operacion=UsuarioController.login`,
   `usuario=anónimo` y `"password":"****"`. `Secreta123` no aparece en el archivo.

4. Con el token del login, llamar a `GET /api/usuarios/{id}/portfolio` y verificar que la
   línea nueva muestra el email en `usuario=`.
5. Verificar que la consola no repite las líneas de auditoría y que sigue mostrando
   `[corr:...]` como antes.

Formato completo de la línea: [contracts/linea-auditoria.md](contracts/linea-auditoria.md).
