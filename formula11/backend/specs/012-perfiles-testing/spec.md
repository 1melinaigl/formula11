# Feature Specification: Perfiles de testing separados para unitarios y e2e

**Feature Branch**: `012-perfiles-testing`

**Created**: 2026-11-02

**Status**: Draft

**Input**: Punto core pendiente de la Entrega 2: "Separar profiles de testing para unitarios y e2e".

## Scope

Separar la ejecución y la configuración de los tres tipos de test de la constitución (punto 6): unitarios, integración y end to end. Sin perfil, Maven sigue corriendo todos, que es lo que hace el CI.

## Clarifications

### Session 2026-11-02

- Q: ¿Qué se separa? → A: Dos cosas. La ejecución, con perfiles de Maven (`unitarios`, `integracion`, `e2e`), y la configuración, con un perfil de Spring `e2e` propio para los tests del paquete `e2e`. Los de integración siguen con el perfil `test` y los unitarios no levantan Spring.
- Q: ¿Se pueden tocar los tests existentes? → A: Sí, solo la anotación `@ActiveProfiles("test")` → `@ActiveProfiles("e2e")` en los tests del paquete `e2e` (confirmado por A el 2026-11-02).
- Q: ¿Cambia el CI? → A: No. Sigue corriendo `mvn verify` sin perfil, con todos los tests y Sonar.

## Requirements

- **FR-001**: `mvn test -Punitarios` MUST correr solo `**/unit/**/*Test.java`, sin Docker.
- **FR-002**: `mvn test -Pintegracion` MUST correr solo `**/integration/**/*Test.java`.
- **FR-003**: `mvn test -Pe2e` MUST correr solo `**/e2e/**/*Test.java`.
- **FR-004**: Sin perfil, Maven MUST correr todos los tests.
- **FR-005**: Los tests del paquete `e2e` MUST usar el perfil de Spring `e2e` (`application-e2e.yml`), con su propia base de Testcontainers, carpeta de perfil de WhoScored y nivel de log.
- **FR-006**: Los tests de integración MUST seguir usando el perfil `test`.

## Success Criteria

- **SC-001**: Cada perfil de Maven ejecuta solo su tipo de test y todos pasan.
- **SC-002**: El CI sigue en verde sin cambios.

## Quickstart

```powershell
cd formula11\backend
mvn clean test -Punitarios     # rápido, sin Docker
mvn clean test -Pintegracion   # necesita Docker
mvn clean test -Pe2e           # necesita Docker
mvn clean verify               # todo, como el CI
```
