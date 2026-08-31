<!-- Sync Impact Report: Version change: 0.0.0 → 1.0.0 | Modified principles: new constitution established for Formula 11 backend | Added sections: Architecture & Technology Constraints; Development Workflow | Removed sections: scaffold placeholders and template examples | Follow-up TODOs: none -->

# Formula11 Backend Constitution

## Core Principles

### I. Layered Architecture is Mandatory
This project MUST follow a strict separation of concerns across Controllers, Services, Repositories, and Adapters. Business logic MUST live in services, persistence in repositories, HTTP/API concerns in controllers, and integration responsibilities in adapters. Any change that crosses layers without an explicit boundary is a violation of this constitution and MUST be rejected during review.

This rule is non-negotiable because it preserves maintainability, testability, and deployment safety in a growing marketplace backend. A monolithic implementation may appear faster initially, but it produces hidden coupling and unreliable business behavior.

### II. Security is Enforced at the API Boundary
All protected resources MUST require JWT-based authentication, and every request MUST be validated against the expected role and authorization rules before business logic is executed. The system MUST expose authenticated endpoints using a secure API key mechanism for integration and partner access where external calls are required.

Secrets, tokens, and keys MUST never be stored in source code or committed configuration. Security checks MUST be enforced in the infrastructure and application layers, not bypassed by client-side assumptions or ad hoc validation.

### III. API Documentation is a First-Class Deliverable
Every public endpoint MUST be documented with Swagger/OpenAPI v3 definitions, including request/response models, authentication requirements, and error handling. The documentation MUST reflect the actual contract of the service and MUST be updated in the same change set as any API behavior change.

This project treats documentation as part of production readiness, not as optional post-development work. A service without a truthful API specification is not implementation-complete.

### IV. Quality, Testing, and Sonar Compliance are Required
All new logic MUST be backed by automatic unit tests, and the project MUST maintain a regression-safe testing culture from the beginning. The codebase MUST be structured to support static analysis and review with SonarCloud, and the quality gate target is fewer than 10 issues in the branch baseline.

No merge should expand technical debt without a justified exception. Shared code, edge conditions, validation rules, and security-sensitive behavior require explicit coverage and review.

## Architecture & Technology Constraints


The backend MUST be implemented with Java and Spring Boot, using PostgreSQL as the persistence layer. The architecture MUST preserve clear boundaries between application, domain, and infrastructure concerns. Persistence, external integrations, and domain services MUST be decoupled from HTTP concerns and remain independently testable.

The project MUST favor explicit contracts, clean domain logic, and deterministic error handling. When new dependencies are introduced, they MUST be justified by maintainability, security, or measurable operational value rather than convenience alone.

## Development Workflow

All work MUST be implemented through small, reviewable changes that preserve the existing architecture and security model. Feature work MUST not bypass validation, documentation, or test expectations. PRs MUST verify that the API contract remains accurate, the security model remains intact, and the code remains within the stated quality thresholds.

The team MUST treat architecture, authentication, documentation, and tests as delivery requirements, not optional follow-up tasks. If a change cannot satisfy these rules, it MUST be redesigned before merging.

## Governance

This Constitution supersedes local preferences, informal shortcuts, and undocumented assumptions. Any deviation from these principles requires explicit approval, documented rationale, and a clear remediation plan. The project MUST not trade long-term maintainability for short-term speed.

All repository changes MUST be reviewed for compliance with these rules. Complexity, workaround logic, skipped tests, undocumented endpoints, or security exceptions are not acceptable without justification and approval.

**Version**: 1.0.0 | **Ratified**: 2026-08-31 | **Last Amended**: 2026-08-31
