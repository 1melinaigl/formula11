# Quickstart: sincronización externa del catálogo

## Configuración

La aplicación arranca sin apikey y el catálogo local sigue funcionando. Para habilitar la sincronización, define la clave solo en el entorno del proceso:

```powershell
$env:FOOTBALL_API_KEY = "<clave-local-no-commitear>"
$env:FOOTBALL_API_BASE_URL = "https://api.football-data.org/v4"
```

Opcionales:

- `FOOTBALL_API_KEY_HEADER` (default `X-Auth-Token`)
- `FOOTBALL_API_CONNECT_TIMEOUT` (default `3s`)
- `FOOTBALL_API_READ_TIMEOUT` (default `10s`)
- `FOOTBALL_API_RESPONSE_TIMEOUT` (default `15s`)
- `FOOTBALL_API_CACHE_TTL` (default `15m`)
- `FOOTBALL_API_CACHE_MAXIMUM_SIZE` (default `500`)
- `FOOTBALL_API_MINIMUM_REQUEST_INTERVAL` (default `0ms`; debe ajustarse al plan del proveedor)

Nunca escribas la clave en `application.yml`, `application-local.yml`, Postman, logs o commits.

## Arranque

```powershell
Set-Location backend
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

Sin `FOOTBALL_API_KEY`, el arranque es válido y `GET /api/jugadores` solo devuelve el catálogo local.

## Sincronización manual

Obtén un JWT mediante el registro/login existente y ejecútalo contra el endpoint protegido:

```powershell
$headers = @{ Authorization = "Bearer <jwt-local>" }
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/jugadores/sincronizacion -Headers $headers
```

La respuesta contiene `COMPLETADA` o `PARCIAL`, resultados por liga, cantidades creadas/actualizadas/omitidas y `correlationId`. La apikey no forma parte de la solicitud ni de la respuesta.

## Validación

```powershell
mvn test
```

Los tests del adapter y service usan mocks; los tests de persistencia/E2E usan Testcontainers y nunca llaman a Football-Data.org real.
