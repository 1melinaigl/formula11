# Quickstart: Login de usuarios

## Prerrequisitos

- Java 21 y Maven disponibles.
- Docker disponible y ejecutándose para Testcontainers PostgreSQL.
- Ejecutar comandos desde `backend/`.

## Validación automatizada

Ejecutar toda la suite:

```powershell
mvn test
```

La suite debe cubrir:

- `UsuarioServiceTest`: éxito, email normalizado, email inexistente, contraseña incorrecta y token emitido con `JwtService`.
- `UsuarioRepositoryIntegrationTest` o una prueba de integración equivalente: persistencia y búsqueda de email normalizado contra PostgreSQL real.
- `LoginUsuarioE2ETest`: endpoint público, respuesta exitosa, uso del JWT en un endpoint protegido, `400` para campos vacíos y `401` indistinguible para email inexistente/contraseña incorrecta.

## Validación manual

1. Iniciar la aplicación con PostgreSQL configurado.
2. Registrar un usuario:

```powershell
$body = '{"nombre":"Ana","email":"ana@example.com","password":"Secreto123!"}'
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/usuarios/registro -ContentType 'application/json' -Body $body
```

3. Ejecutar login con el mismo email escrito con espacios y mayúsculas:

```powershell
$login = '{"email":" ANA@EXAMPLE.COM ","password":"Secreto123!"}'
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/usuarios/login -ContentType 'application/json' -Body $login
```

Resultado esperado: `200` y JSON con `id`, `nombre`, `email` normalizado y `token`.

4. Probar contraseña incorrecta y email inexistente. Ambos deben responder `401` con el mismo `mensaje`, `timestamp` y `correlationId` según el formato de error existente, sin token.
5. Probar email vacío, contraseña ausente o email inválido. Deben responder `400`.
6. Copiar el token exitoso como `Bearer` y consultar `GET /api/jugadores`; la solicitud debe ser autenticada.

## Contrato y documentación

- Contrato: [contracts/openapi.yaml](contracts/openapi.yaml).
- Swagger generado: `http://localhost:8080/swagger-ui.html`.
- Colección: `postman/formula11-registro-catalogo.json`, solicitud `Iniciar sesión`.
