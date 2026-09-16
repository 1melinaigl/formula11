# Data Model: Entrega 1

## Entities

### User

Represents a registered user in the system.

| Field | Type | Constraints | Notes |
|---|---|---|---|
| id | Long | PK, generated | Internal identifier |
| username | String | unique, not null | Display login name |
| email | String | unique, not null | User email |
| password | String | not null | Stored hashed |
| apiKey | String | unique, not null | Generated on registration |

### Player

Represents a football player in the catalog used by the first delivery.

| Field | Type | Constraints | Notes |
|---|---|---|---|
| id | Long | PK, generated | Internal identifier |
| name | String | not null | Player's full name |
| league | String | not null | League or competition |
| team | String | not null | Team name |
| position | String | not null | e.g. Forward, Midfielder |
| baseValue | BigDecimal | not null | Base value for representation |

## Relationships

- `User` and `Player` are independent in this iteration.
- No market, ownership, or transaction relationship is modeled in this release.

## Persistence rules

- Both entities persist in PostgreSQL using Spring Data JPA.
- `User.username`, `User.email`, and `User.apiKey` must be unique.
- Passwords must never be persisted in clear text.
- `Player` records are expected to be seeded or loaded by SQL scripts for the initial API use case.
