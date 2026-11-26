# Implementation Plan: Test de arquitectura del backend

**Branch**: `014-test-arquitectura` | **Date**: 2026-11-25 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/014-test-arquitectura/spec.md`

## Summary

Agregar un único test unitario ArchUnit que haga cumplir las dependencias entre las cinco capas, las reglas de DTOs, nombres, ciclos entre áreas y ubicación de clases. Se incorpora `com.tngtech.archunit:archunit-junit5:1.4.1` en scope `test`. El test se ubica bajo `unit` para incluirse en la suite normal, `-Punitarios` y CI (`mvn verify`). No se prevén cambios de código productivo; si ArchUnit revela una infracción, se corrige.

## Technical Context

**Language/Version**: Java 21

**Primary Dependencies**: Spring Boot 3.5.5; ArchUnit JUnit 5 (`com.tngtech.archunit:archunit-junit5:1.4.1`, scope `test`)

**Storage**: No aplica; el test inspecciona bytecode y no accede a PostgreSQL.

**Testing**: JUnit 5 / Maven Surefire. Ejecución objetivo: `mvn test -Punitarios`, `mvn test` y `mvn verify`.

**Target Platform**: Backend Java/Spring Boot

**Project Type**: API web monolítica en capas

**Performance Goals**: El test debe ejecutarse como parte de la suite unitaria y no debe arrancar Spring ni una base de datos.

**Constraints**: Un solo archivo Java de test en `src/test/java/com/formula11/unit/arquitectura/ArquitecturaTest.java`. No se esperan cambios productivos. Cada regla ArchUnit se verifica en un método `@Test` de JUnit y explica su propósito con `.because(...)`.

**Scale/Scope**: Cinco capas (`controller`, `service`, `persistence`, `model`, `dto`), siete áreas funcionales y paquetes transversales fuera de las capas.

## Constitution Check

| Principio | Estado | Justificación |
|---|---|---|
| Mantener la arquitectura en capas y el modelo independiente | Conforme | El test hace cumplir los límites acordados, incluyendo que `model` no dependa de `controller`, `service` ni `persistence`. |
| Tests unitarios sin framework ni base de datos | Conforme | ArchUnit importa las clases de producción; no se usa `@SpringBootTest`, contexto Spring ni Testcontainers. |
| Separación entre pruebas unitarias, integración y E2E | Conforme | El test se ubica en `unit/arquitectura`, dentro del patrón incluido por el perfil `unitarios`. |
| Convenciones de idioma y nomenclatura | Conforme | La documentación permanece en español; las reglas de sufijos se limitan a los tipos anotados o asignables especificados. |
| Evitar complejidad innecesaria | Conforme | Se añade una dependencia de test y un único archivo de test; no se prevén refactors productivos ni infraestructura adicional. |

No hay gates constitucionales pendientes.

## Design Decisions

### Carga de clases y ubicación

Crear `src/test/java/com/formula11/unit/arquitectura/ArquitecturaTest.java` importando una sola vez (`@BeforeAll`) las clases de `com.formula11` con `ClassFileImporter` y `ImportOption.Predefined.DO_NOT_INCLUDE_TESTS`. Así las reglas se evalúan sobre clases productivas, mientras que los tests y fixtures no interfieren en las convenciones.

Cada regla se verifica en su propio método `@Test` (los ciclos, en un `@ParameterizedTest` por capa). No se usan campos `@ArchTest`: Surefire no ejecuta el motor JUnit de ArchUnit en este proyecto y los reporta como 0 tests, aunque el lanzador de JUnit sí los descubre. Con métodos `@Test`, Maven y CI reportan cada regla por separado (14 tests). Cada regla lleva un `.because(...)` breve y específico para que el motivo figure en el mensaje de fallo.

### Dependencias entre capas

Definir `Controller`, `Service`, `Persistence`, `Model` y `Dto` en `layeredArchitecture().consideringOnlyDependenciesInLayers()` usando los paquetes `com.formula11.<capa>..`.

Establecer los límites requeridos por la spec:

- `Controller` no es accedido por ninguna de las capas definidas.
- `Service` es accedido por `Controller` y `Dto`. Se permite `Dto` porque los mapeadores de respuestas existentes consumen resultados de `Service`; ver la justificación y el alcance en [research.md](./research.md). La regla no permite que `Service` dependa de `Dto`.
- `Persistence` solo es accedido por `Service`, lo que también impide el acceso directo de `Controller` a repositorios.
- `Model` solo es accedido por `Controller`, `Service`, `Persistence` y `Dto`.
- Una regla separada impide que `Model` dependa de `controller`, `service` o `persistence`.

La regla de DTOs se declara separadamente para permitir el acceso desde `controller` y `dto` únicamente. Las dependencias de paquetes transversales no asignados a estas capas no se incluyen en las comprobaciones entre capas; los usos actuales se documentan en `research.md`.

### Resto de reglas

- `model` no depende de `org.springframework.web..` ni de `com.formula11.dto..`.
- Sufijos: `@RestController` → `Controller`; `@Service` → `Service`; asignables a Spring Data `Repository` → `Repository`; asignables a `Throwable` → `Exception`.
- Verificar ciclos por separado para cada capa, agrupando solo sus subpaquetes de área (`usuarios`, `jugadores`, `metricas`, `cotizaciones`, `mercado`, `portfolio`, `senales`). Excluir de la agrupación los subpaquetes de dominio no funcionales de `model`: `excepciones`, `valuacion` y `whoscored`.
- Rechazar clases colocadas directamente en las raíces exactas `com.formula11.controller`, `com.formula11.service`, `com.formula11.persistence` y `com.formula11.dto`. No extender esta regla a subpaquetes ni a `model`.

### Verificación de cada regla

Durante la implementación se introducirá temporalmente una infracción por regla en código productivo y se ejecutará el test para comprobar que falla con un mensaje identificable. Cada cambio temporal se revierte inmediatamente. No se crearán clases de test diseñadas para infringir las reglas; el procedimiento está documentado en [quickstart.md](./quickstart.md).

## Project Structure

### Documentation (this feature)

```text
specs/014-test-arquitectura/
├── spec.md
├── plan.md
├── research.md
└── quickstart.md
```

No se requiere `data-model.md` porque la feature no incorpora datos ni persistencia. No se requieren contratos externos porque el test es interno al build.

### Source Code

```text
backend/
├── pom.xml                                            # ArchUnit test dependency
├── src/
│   ├── main/java/com/formula11/                      # Existing production packages
│   └── test/java/com/formula11/unit/arquitectura/
│       └── ArquitecturaTest.java                     # Single new Java test file
└── specs/014-test-arquitectura/                       # Feature documentation

README.md                                              # Architecture section
```

**Structure Decision**: Mantener la estructura Maven existente. El test usa el paquete `unit` para ser detectado por el perfil Maven existente, sin cambios a la configuración de CI.

## Complexity Tracking

La única excepción al acceso exclusivo de `Controller` a `Service` es `Dto`, justificada por los mapeadores de respuestas existentes y documentada en [research.md](./research.md). No se agregan componentes adicionales ni otras excepciones.
