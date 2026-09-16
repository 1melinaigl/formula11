# Research Notes: Entrega 1

## 1. Java and Spring Boot version

The project will use Java 17 LTS and Spring Boot 3.x because it is the current stable baseline for Spring ecosystem projects and aligns well with modern Security and JPA features.

## 2. Security strategy

The backend must satisfy the constitutional requirement for JWT and ApiKey.

Decision:
- JWT will be used for bearer-token authentication after registration/login.
- ApiKey will be stored on the User entity and enforced as part of the authentication chain for protected API access.
- The user registration flow generates both values in a single step: an ApiKey and a JWT.

Reason:
- JWT is the standard for stateless app auth.
- ApiKey is a stable identifier for endpoint access and future integrations.

## 3. Persistence strategy

PostgreSQL will be used with Spring Data JPA and entities for `User` and `Player`.

Decision:
- Use native JPA entities and repository interfaces.
- Use `@Column(unique = true)` for username/email/apiKey.
- Use `BigDecimal` for `baseValue` to preserve financial precision even if this is not a final market calculation.

## 4. Swagger strategy

Use Springdoc OpenAPI UI to expose the full API documentation automatically.

Decision:
- Configure API metadata in `application.yml`.
- Annotate controllers and DTOs with `@Operation`, `@ApiResponse`, and `@Schema`.
- Require API docs to remain updated with each release.

## 5. Testing strategy

Use JUnit 5 + Mockito + Spring Boot test slices.

Decision:
- Unit tests for service logic
- Controller tests with MockMvc
- Spring Security tests for unauthorized access
- Optional Testcontainers for PostgreSQL integration tests

## 6. Scope boundaries

This delivery is intentionally limited to:
- user creation
- API key generation
- JWT token issuance
- player catalog read access

Out of scope:
- purchase/sale market
- token trading
- price/cotization calculations
