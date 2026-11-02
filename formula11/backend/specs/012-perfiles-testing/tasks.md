# Tasks: Perfiles de testing separados

**Feature**: `012-perfiles-testing`
**Spec**: [spec.md](spec.md)

- [x] **T001 - Perfiles de Maven**: `unitarios`, `integracion` y `e2e` en `pom.xml`, cada uno con el `include` de surefire de su paquete.
- [x] **T002 - Perfil de Spring `e2e`**: `src/test/resources/application-e2e.yml`.
- [x] **T003 - Tests e2e con su perfil**: `@ActiveProfiles("e2e")` en los 11 tests del paquete `e2e` (solo la anotación, con el "sí" de A).
- [ ] **T004 - Validación**: correr los tres perfiles en local y confirmar el CI en verde.
