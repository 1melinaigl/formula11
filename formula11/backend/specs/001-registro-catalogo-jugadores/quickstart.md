# Quickstart: backend Formula11

## Prerrequisitos

- Java 21 y Maven disponibles.
- Docker Desktop iniciado para ejecutar Testcontainers.
- PostgreSQL local iniciado.
- Base `formula11` creada en PostgreSQL.

## Configuración local

El perfil local usa:

```text
SPRING_PROFILES_ACTIVE=local
DB_URL=jdbc:postgresql://localhost:5432/formula11
DB_USERNAME=postgres
DB_PASSWORD=root
JWT_SECRET=<secreto-local-largo>
```

También se documentarán estos valores en `application-local.yml`, según la solicitud. El secreto JWT debe tener longitud suficiente para el algoritmo elegido.

## Ejecutar

Desde `backend`:

```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

La API queda disponible en `http://localhost:8080` y Swagger en `/swagger-ui.html`.

## Flujo manual

1. Registrar un usuario:

```powershell
$body = '{"nombre":"Ana Pérez","email":"ana@example.com","password":"Secreto123!"}'
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/usuarios/registro -ContentType 'application/json' -Body $body
```

2. Copiar `token` de la respuesta y consultar el catálogo:

```powershell
Invoke-RestMethod -Method Get -Uri http://localhost:8080/api/jugadores -Headers @{ Authorization = "Bearer <token>" }
```

## Tests

Desde `backend`, sin activar `local`:

```powershell
mvn test
```

Los tests de integración/E2E levantarán PostgreSQL mediante Testcontainers. No deben usar `application-local.yml` ni la base `formula11` del desarrollador.
