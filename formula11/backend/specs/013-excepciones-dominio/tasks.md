# Tasks: Excepciones propias del dominio en el modelo

**Spec**: [spec.md](spec.md)

- [x] **T001** Crear `model/excepciones/DatoInvalidoException` (con `requerido`) y `CantidadInsuficienteException`.
- [x] **T002** Reemplazar en las 15 clases del modelo `IllegalArgumentException` → `DatoInvalidoException`, `IllegalStateException` → `CantidadInsuficienteException` y `Objects.requireNonNull` → `DatoInvalidoException.requerido`.
- [x] **T003** Mapear las dos excepciones en `GlobalExceptionHandler` (400 y 409) y agregar `ExcepcionesDominioHandlerTest`.
- [x] **T004** Ajustar la excepción esperada en los 7 tests del modelo (con el "sí" de A).
- [ ] **T005** Correr `mvn clean test -Dtest="**/unit/**/*Test"` y el CI completo.
