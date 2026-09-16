# Quickstart: Entrega 1

## Prerequisites

- Java 17+
- Maven 3.9+
- PostgreSQL 14+
- Git

## Configuration

Set the following environment variables before running the app:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/formula11
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export JWT_SECRET=change-me-very-long-secret-key
export API_KEY_PREFIX=F11
```

## Run the application

```bash
./mvnw spring-boot:run
```

or

```bash
mvn spring-boot:run
```

## Endpoints for this delivery

### Register user

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "juanperez",
    "email": "juan@example.com",
    "password": "Secret123!"
  }'
```

### Get players

```bash
curl -X GET http://localhost:8080/api/players \
  -H "Authorization: Bearer <jwt-token>" \
  -H "X-API-KEY: <api-key>"
```

## Swagger UI

Open the documentation at:

```text
http://localhost:8080/swagger-ui/index.html
```

## Notes

- Do not include market operations in this iteration.
- Keep all security credentials in environment variables or secret management.
- Use the generated ApiKey and JWT as the first-step access model for the API.
