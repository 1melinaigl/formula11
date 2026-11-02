# Feature Specification: Excepciones propias del dominio en el modelo

**Feature Branch**: `sacar-illegal-modelo`

**Created**: 2026-11-02

**Status**: Draft

**Input**: Corrección de la Entrega 1: "sacar las illegal del modelo".

## Scope

Las clases del paquete `model` lanzaban excepciones genéricas de Java (`IllegalArgumentException`, `IllegalStateException` y `NullPointerException` vía `Objects.requireNonNull`) para sus invariantes. La constitución (punto 3) pide que el modelo lance excepciones propias. Esta corrección cambia solo el modelo y el handler; los services, controllers y adapters no cambian.

## Clarifications

### Session 2026-11-02

- Q: ¿Qué excepciones se crean? → A: Dos, en `com.formula11.model.excepciones`: `DatoInvalidoException` (un dato viola una regla: obligatorio, rango o formato) y `CantidadInsuficienteException` (no alcanzan los créditos de una cuenta o los tokens de una tenencia).
- Q: ¿Cambian los códigos HTTP? → A: No. `DatoInvalidoException` responde 400, como antes `IllegalArgumentException`, y `CantidadInsuficienteException` responde 409, como antes `IllegalStateException`.
- Q: ¿Se tocan tests existentes? → A: Sí, con el "sí" de A (2026-11-02): solo la excepción que esperan los tests del modelo (`MercadoModelTest`, `UsuarioTest`, `JugadorTest`, `VariacionCotizacionTest`, `DetectorSenalesTest`, `MatchCentreParserTest`, `NormalizadorNombreTest`).
- Q: ¿Y las excepciones genéricas de services y adapters? → A: Quedan como están; la corrección pedida es sobre el modelo.

## Requirements

- **FR-001**: Ninguna clase de `com.formula11.model` MUST lanzar `IllegalArgumentException`, `IllegalStateException` ni usar `Objects.requireNonNull`.
- **FR-002**: Los valores obligatorios MUST validarse con `DatoInvalidoException.requerido(valor, mensaje)`, con el mismo mensaje en español que antes.
- **FR-003**: `GlobalExceptionHandler` MUST responder `DatoInvalidoException` con 400 y `CantidadInsuficienteException` con 409, con el esquema `Error`.
- **FR-004**: Las excepciones del modelo MUST vivir en el modelo: no dependen de Spring ni de otras capas.

## Success Criteria

- **SC-001**: `grep` de `Illegal` y `requireNonNull` en `src/main/java/com/formula11/model` no devuelve resultados.
- **SC-002**: Todos los tests pasan y los códigos HTTP de los e2e no cambian.
