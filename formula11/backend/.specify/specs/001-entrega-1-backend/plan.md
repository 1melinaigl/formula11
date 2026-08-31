# Implementation Plan: Entrega 1 - Backend del mercado de jugadores

**Branch**: `001-entrega-1-backend` | **Date**: 2026-08-31 | **Spec**: `/specs/001-entrega-1-backend/spec.md`

**Input**: Feature specification from `/specs/001-entrega-1-backend/spec.md`

## Summary

La Entrega 1 implementará un backend en Java 17 + Spring Boot con PostgreSQL que exponga dos funcionalidades base: registro de usuarios con generación automática de ApiKey y consulta del catálogo de jugadores mediante GET /players. La solución se diseñará con separación estricta por capas, autenticación basada en JWT y validación por ApiKey, y documentación automática con Swagger/OpenAPI v3. La iteración no incluirá comercio de tokens ni cálculo de cotización.

## Technical Context

**Language/Version**: Java 17 LTS

**Primary Dependencies**:
- Spring Boot 3.x
- Spring Web
- Spring Data JPA
- Spring Security
- PostgreSQL Driver
- Spring Validation
- JJWT (JWT)
- Springdoc OpenAPI UI
- Lombok
- JUnit 5 + Mockito + Spring Boot Test

**Storage**: PostgreSQL 14+

**Testing**: JUnit 5, Mockito, Spring Boot Test, Testcontainers (opcional pero recomendado para integración)

**Target Platform**: Backend REST API ejecutado en entorno server-side con JVM y PostgreSQL

**Project Type**: Web service / REST API backend

**Performance Goals**: GET /players debe responder de forma rápida para listados pequeños/medianos; la P95 de lectura en entorno local debe ser aceptable sin optimizaciones prematuras.

**Constraints**:
- Debe respetar arquitectura por capas obligatoria
- Debe proteger endpoints con autenticación segura
- Debe documentar API con Swagger
- Debe mantener alcance de la Entrega 1 sin mercado ni cotización

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] Architecture: strict layer separation is required by the project constitution.
- [x] Security: JWT + ApiKey strategy mandatory.
- [x] Documentation: Swagger/OpenAPI v3 mandatory.
- [x] Quality: tests and SonarCloud-oriented design required.

No constitutional violations detected for this feature. The implementation remains within the defined scope of the first delivery.

## Project Structure

### Source Code (repository root)

```text
backend/
├── src/
│   ├── main/
│   │   ├── java/com/formula11/
│   │   │   ├── config/
│   │   │   ├── controllers/
│   │   │   ├── services/
│   │   │   ├── repositories/
│   │   │   ├── adapters/
│   │   │   ├── models/
│   │   │   ├── dto/
│   │   │   ├── security/
│   │   │   ├── exceptions/
│   │   │   └── Formula11Application.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── db/migration/
│   └── test/
│       └── java/com/formula11/
│           ├── controllers/
│           ├── services/
│           ├── repositories/
│           ├── security/
│           └── integration/
├── pom.xml
├── .gitignore
└── README.md
```

**Structure Decision**: Se implementará una API REST Spring Boot con paquetes orientados por responsabilidad. Las entidades viven en `models`, la capa de persistencia en `repositories`, la lógica de negocio en `services`, la capa HTTP en `controllers`, y las integraciones externas/implementaciones de infraestructura en `adapters`.

## Phase 0: Research and Design Decisions

### 1. Security model

Para esta entrega, la autenticación será híbrida y explícita:

- `User.apiKey` será un valor único generado al momento del registro.
- `JWT` se utilizará como token de acceso para sesiones autenticadas.
- Los endpoints protegidos validarán al menos el token JWT y/o la ApiKey según la estrategia final elegida.
- En la implementación, la recomendación práctica para la Entrega 1 es: validar `Authorization: Bearer <jwt>` y verificar que el usuario correspondiente posea una ApiKey válida.

Esto permite cumplir con la constitución del proyecto: JWT para autenticación y ApiKey para acceso al resto de endpoints.

### 2. Persistence model

El modelo mínimo se implementará con JPA y PostgreSQL:

- `User`: id, username, email, password, apiKey
- `Player`: id, name, league, team, position, baseValue

Se recomienda usar `@Entity`, `@Table`, `@Column(nullable = false, unique = true)`, y relaciones explícitas de negocio cuando se amplíe el alcance.

### 3. Validation rules

En esta entrega se validarán:

- username obligatorio y único
- email obligatorio, único y formateado
- password obligatorio con longitud mínima razonable
- ApiKey único y generado vía UUID o valor aleatorio seguro
- player required fields not null

### 4. OpenAPI / Swagger

Se configurará Springdoc con:

- `springdoc-openapi-starter-webmvc-ui`
- properties para títulos, descripción y versión de la API
- anotaciones `@Operation`, `@ApiResponse`, `@Parameter`, `@Schema` en controladores y DTOs

### 5. Testing strategy

Se implementarán tests mínimos por capa:

- unit tests para servicio de usuario y servicio de player
- tests de controlador con MockMvc
- tests de seguridad para errores de autenticación
- tests de repositorio con JPA/H2 para validación simple
- testing de integración para flujo registro + GET /players

## Phase 1: Data Model and Contracts

### Entity model

```java
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String apiKey;
}
```

```java
@Entity
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String league;

    @Column(nullable = false)
    private String team;

    @Column(nullable = false)
    private String position;

    @Column(nullable = false)
    private BigDecimal baseValue;
}
```

### API contract

#### POST /api/users
- Crea un usuario
- Body: username, email, password
- Validations: required fields and uniqueness
- Response: user info + JWT + ApiKey

#### GET /api/players
- Requiere autenticación
- Returns: list of players
- Response body: array of Player DTOs

## Dependency Plan (Maven)

```xml
<properties>
    <java.version>17</java.version>
    <spring-boot.version>3.3.3</spring-boot.version>
    <jjwt.version>0.11.5</jjwt.version>
    <springdoc.version>2.6.0</springdoc.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>${jjwt.version}</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>${jjwt.version}</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>${jjwt.version}</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>${springdoc.version}</version>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## Implementation Strategy

### User registration flow

1. Controller receives `POST /api/users` with username, email, password.
2. DTO validation checks required fields and email format.
3. Service validates duplicates and hashes password with BCrypt.
4. Service generates unique ApiKey with `UUID.randomUUID()` or a secure random token.
5. Service creates `User` entity and persists it.
6. Service generates JWT containing user id and claims.
7. Response returns user summary, ApiKey, and JWT.

### Security configuration

- `SecurityConfig` configures stateless session management.
- `JwtAuthenticationFilter` reads `Authorization: Bearer <jwt>`.
- `ApiKeyAuthenticationFilter` reads custom header (`X-API-KEY`) or similar.
- Protected routes require the authentication chain and role checks.
- Passwords are stored with BCrypt.

### Player catalog flow

1. Controller handles `GET /api/players`.
2. Service calls repository to fetch all players.
3. Repository returns `List<Player>`.
4. Controller maps entities to DTOs.
5. API returns list serialized in JSON with Swagger docs.

### Testing base configuration

`src/test/resources/application-test.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
  security:
    user:
      password: none
```

Recommended test classes:

- `UserServiceTest`
- `PlayerServiceTest`
- `UserControllerTest`
- `SecurityConfigTest`
- `UserRegistrationIntegrationTest`

## Complexity Tracking

> No constitutional violations requiring justification.

## Execution Plan

### Phase 1: Base project and persistence
- Create Spring Boot project with Java 17 and Maven
- Add required dependencies
- Configure PostgreSQL connection
- Create JPA entities and repositories
- Bootstrap player seed data

### Phase 2: Security and user management
- Add Spring Security config
- Implement JWT generation/validation
- Implement ApiKey generation and persistence
- Create user registration endpoint and DTOs

### Phase 3: Catalog endpoint and docs
- Implement GET /players service and controller
- Add Swagger/OpenAPI annotations
- Add global exception handling
- Validate response contracts

### Phase 4: Testing and quality gate
- Add unit tests for service logic
- Add controller tests for HTTP responses
- Add security tests for unauthorized access
- Run Maven test and check test coverage and SonarCloud-style quality issues

## Exit Criteria

This feature is complete when all of the following are true:

- User creation works with password hashing and generated ApiKey
- JWT issuance works for authenticated sessions
- GET /players returns valid player data for authorized callers
- Swagger UI documents the endpoints
- Unit and integration tests pass
- Architecture remains within the required layer boundaries

**Out of Scope for this delivery**:
- token market
- buy/sell operations
- quote calculation
- financial logic beyond base value representation
