# Tasks: Entrega 1 - Backend del mercado de jugadores

**Input**: Design documents from `/specs/001-entrega-1-backend/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, quickstart.md

**Tests**: Tests are included because the feature specification explicitly requires automated unit tests.

## Format: `[ID] [P?] [Story] Description`

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Initial project configuration and base infrastructure.

- [x] T001 [P] [Setup] Create the Spring Boot project structure under `backend/src/main/java/com/formula11` and `backend/src/test/java/com/formula11`
- [x] T002 [P] [Setup] Initialize Maven project and add Spring Boot + PostgreSQL + Security + JPA + Validation + Swagger + JWT dependencies in `backend/pom.xml`
- [x] T003 [P] [Setup] Configure base application properties in `backend/src/main/resources/application.yml` with PostgreSQL connection and JWT settings
- [x] T004 [P] [Setup] Add base `Formula11Application.java` entry point and repository package structure
- [x] T005 [Setup] Configure `.gitignore`, base README, and project resource folders for the backend

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Persistence and core technical foundation required before feature implementation.

- [x] T006 [P] [Foundation] Create base package structure: `config`, `controllers`, `services`, `repositories`, `models`, `adapters`, `dto`, `security`, `exceptions`
- [x] T007 [Foundation] Configure PostgreSQL datasource and JPA settings in `application.yml` and `application-dev.yml`
- [x] T008 [Foundation] Create base exception handling and response model strategy for validation and security errors
- [x] T009 [P] [Foundation] Add Spring Security configuration for stateless JWT authentication and ApiKey validation
- [x] T010 [Foundation] Define environment variable configuration for JWT secret, datasource credentials, and API key prefix

**Checkpoint**: Foundation ready for model and business implementation.

---

## Phase 3: User Story 1 - Registro de usuario y generación de ApiKey (Priority: P1) 🎯 MVP

**Goal**: Allow a user to register and receive a valid ApiKey and JWT for protected access.

**Independent Test**: A client can register with valid data and receive an ApiKey and JWT in a single flow.

### Tests for User Story 1

- [x] T011 [P] [US1] Create failing test for user registration success in `backend/src/test/java/com/formula11/controllers/UserControllerTest.java`
- [x] T012 [P] [US1] Create failing test for duplicate email/username validation in `backend/src/test/java/com/formula11/services/UserServiceTest.java`
- [x] T013 [P] [US1] Create failing test for unauthorized access to protected endpoint in `backend/src/test/java/com/formula11/security/SecurityConfigTest.java`

### Implementation for User Story 1

- [x] T014 [P] [US1] Create `User` entity in `backend/src/main/java/com/formula11/models/User.java`
- [x] T015 [P] [US1] Create `CreateUserRequest` and `UserResponse` DTOs in `backend/src/main/java/com/formula11/dto/`
- [x] T016 [US1] Create `UserRepository` in `backend/src/main/java/com/formula11/repositories/UserRepository.java`
- [x] T017 [US1] Implement `UserService` in `backend/src/main/java/com/formula11/services/UserService.java`
- [x] T018 [US1] Implement password hashing and validation logic inside `UserService`
- [x] T019 [US1] Implement ApiKey generation logic and unique value assignment in `UserService`
- [x] T020 [US1] Implement JWT generation and claims creation in `backend/src/main/java/com/formula11/security/JwtService.java`
- [x] T021 [US1] Implement `UserController` POST endpoint in `backend/src/main/java/com/formula11/controllers/UserController.java`
- [x] T022 [US1] Add Swagger annotations to `UserController` and DTOs for endpoint documentation
- [x] T023 [US1] Add validation error handling for invalid registration payloads

**Checkpoint**: User Story 1 should be fully functional and independently testable.

---

## Phase 4: User Story 2 - Consulta del catálogo de jugadores (Priority: P1)

**Goal**: Expose a protected endpoint to retrieve the player list.

**Independent Test**: A valid API user can request GET /players and receive the expected list of player records.

### Tests for User Story 2

- [x] T024 [P] [US2] Create failing test for GET /players success with valid auth in `backend/src/test/java/com/formula11/controllers/PlayerControllerTest.java`
- [x] T025 [P] [US2] Create failing test for GET /players denies invalid or missing ApiKey/JWT in `backend/src/test/java/com/formula11/security/PlayerSecurityTest.java`
- [x] T026 [P] [US2] Create failing repository-level test for fetching all players in `backend/src/test/java/com/formula11/repositories/PlayerRepositoryTest.java`

### Implementation for User Story 2

- [x] T027 [P] [US2] Create `Player` entity in `backend/src/main/java/com/formula11/models/Player.java`
- [x] T028 [P] [US2] Create `PlayerRepository` in `backend/src/main/java/com/formula11/repositories/PlayerRepository.java`
- [x] T029 [P] [US2] Create `PlayerResponse` DTO in `backend/src/main/java/com/formula11/dto/PlayerResponse.java`
- [x] T030 [US2] Implement `PlayerService` in `backend/src/main/java/com/formula11/services/PlayerService.java`
- [x] T031 [US2] Implement `PlayerController` GET endpoint in `backend/src/main/java/com/formula11/controllers/PlayerController.java`
- [x] T032 [US2] Add Swagger documentation annotations for the player catalog API
- [x] T033 [US2] Seed or initialize a minimal set of players for the first delivery in `backend/src/main/resources/data.sql` or a migration folder

**Checkpoint**: User Stories 1 and 2 are both functional and independently testable.

---

## Phase 5: Cross-Cutting Quality and Finalization

**Purpose**: Ensure code quality, documentation, and automated validation.

- [x] T034 [P] [Quality] Run unit tests and fix failures for services, controllers, and security flow
- [x] T035 [P] [Quality] Validate Swagger UI exposes all endpoints for the first delivery
- [x] T036 [P] [Quality] Confirm API contracts and response payloads match the specification and DTOs
- [x] T037 [P] [Quality] Review code to ensure layer separation and clean package structure are preserved
- [x] T038 [Quality] Validate that no marketplace, token-trading, or quotation logic was introduced in this iteration
- [x] T039 [Quality] Verify the project is ready for SonarCloud-style quality checks with minimal issues

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1**: No dependencies; setup can start first
- **Phase 2**: Depends on Phase 1 completion and blocks all feature work
- **Phase 3**: Depends on Phase 2 completion
- **Phase 4**: Depends on Phase 2 completion and may build on Phase 3 output
- **Phase 5**: Depends on Phases 3 and 4 completion

### Within Each Story

- Tests must be written first and then made to fail
- Models before repositories
- Repositories before services
- Services before controllers
- Validation and documentation before final quality pass

### Parallel Opportunities

- T001, T002, T003, T004, and T005 can run in parallel
- T006 and T009 can run in parallel once base folders exist
- T011, T012, and T013 can be developed in parallel for US1
- T024, T025, and T026 can be developed in parallel for US2
- T034 through T039 are final quality checks and can be grouped at the end
