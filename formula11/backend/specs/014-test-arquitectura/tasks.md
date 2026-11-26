# Tasks: Test de arquitectura del backend

**Input**: Design documents from `backend/specs/014-test-arquitectura/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `quickstart.md`

**Organization**: Una única user story (US1). El README ya está actualizado y no forma parte de estas tareas.

## Phase 1: Setup

**Purpose**: Incorporar la dependencia necesaria para compilar el test de arquitectura.

- [X] T001 Agregar `com.tngtech.archunit:archunit-junit5:1.4.1` con scope `test` en `backend/pom.xml`.

## Phase 2: Foundational

No hay tareas fundacionales adicionales. T001 es el único requisito previo para crear el test.

## Phase 3: User Story 1 - Mantener la arquitectura al incorporar código (Priority: P1) 🎯 MVP

**Goal**: Hacer cumplir automáticamente los límites arquitectónicos del backend mediante un test unitario ArchUnit.

**Independent Test**: Ejecutar `mvn test -Punitarios -Dtest=ArquitecturaTest` desde `backend`; debe pasar sin iniciar Spring ni requerir una base de datos.

### Implementación

- [X] T002 [US1] Crear `backend/src/test/java/com/formula11/unit/arquitectura/ArquitecturaTest.java` importando las clases productivas con `ClassFileImporter` y una regla `layeredArchitecture()` para las capas `Controller`, `Service`, `Persistence`, `Model` y `Dto` (FR-002), cada regla con `.because(...)`.
- [X] T003 [US1] Agregar en `backend/src/test/java/com/formula11/unit/arquitectura/ArquitecturaTest.java` las reglas para que `model` no dependa de `controller`, `service`, `persistence`, Spring Web ni `dto` (FR-003, FR-004).
- [X] T004 [US1] Agregar en `backend/src/test/java/com/formula11/unit/arquitectura/ArquitecturaTest.java` la regla que limita el uso de tipos `dto` a `controller` y al propio paquete `dto` (FR-005).
- [X] T005 [US1] Agregar en `backend/src/test/java/com/formula11/unit/arquitectura/ArquitecturaTest.java` las reglas de sufijos para `@RestController`, `@Service`, repositorios Spring Data asignables a `Repository` y excepciones asignables a `Throwable` (FR-006 a FR-008).
- [X] T006 [US1] Agregar en `backend/src/test/java/com/formula11/unit/arquitectura/ArquitecturaTest.java` reglas de ciclos por capa, limitadas a las siete áreas funcionales y excluyendo los subpaquetes no funcionales de `model` (FR-009).
- [X] T007 [US1] Agregar en `backend/src/test/java/com/formula11/unit/arquitectura/ArquitecturaTest.java` la regla que prohíbe clases directamente en las raíces `controller`, `service`, `persistence` y `dto` (FR-010).
- [X] T008 [US1] Desde `backend`, ejecutar `mvn test -Punitarios -Dtest=ArquitecturaTest`; corregir en los archivos productivos que reporte ArchUnit cualquier infracción real y repetir hasta que el test pase.

### Validación manual de reglas

**Prerequisito**: T008 debe pasar antes de introducir infracciones temporales. Para cada regla, seguir `backend/specs/014-test-arquitectura/quickstart.md`, correr el test, confirmar que el fallo identifica la regla y revertir inmediatamente el cambio temporal. No crear clases de test que infrinjan reglas.

- [X] T009 [US1] Validar FR-002 introduciendo temporalmente una dependencia prohibida entre capas —por ejemplo, desde `backend/src/main/java/com/formula11/controller/jugadores/JugadorController.java` hacia `backend/src/main/java/com/formula11/persistence/jugadores/JugadorRepository.java`— y revertirla tras comprobar el fallo.
- [X] T010 [US1] Validar FR-003 introduciendo y revirtiendo por separado dependencias temporales desde `backend/src/main/java/com/formula11/model/jugadores/Jugador.java` hacia `controller`, `service` y `persistence`; comprobar el fallo en cada caso.
- [X] T011 [US1] Validar FR-004 introduciendo y revirtiendo por separado una dependencia temporal a Spring Web y otra a `dto` desde `backend/src/main/java/com/formula11/model/jugadores/Jugador.java`; comprobar que cada una hace fallar la regla.
- [X] T012 [US1] Validar FR-005 haciendo que una clase temporal de `service/jugadores` invoque un método de `backend/src/main/java/com/formula11/dto/jugadores/JugadorResponse.java`; revertir el cambio tras comprobar el fallo.
- [X] T013 [US1] Validar FR-006 creando probes temporales sin sufijo `Controller`/`Service`, anotados respectivamente con `@RestController` y `@Service`; comprobar cada convención por separado y retirar los probes.
- [X] T014 [US1] Validar FR-007 creando una interfaz temporal en `persistence/jugadores` que extiende `JpaRepository` y no termina en `Repository`; comprobar el fallo y retirar el probe.
- [X] T015 [US1] Validar FR-008 creando una excepción temporal en `model/excepciones` sin el sufijo `Exception`; comprobar el fallo y retirar el probe.
- [X] T016 [US1] Validar FR-009 creando dos probes temporales que se referencian entre áreas de `service/jugadores` y `service/cotizaciones`; comprobar el ciclo y retirar ambos probes.
- [X] T017 [US1] Validar FR-010 colocando temporalmente, de a una, una clase en `backend/src/main/java/com/formula11/controller/ArquitecturaProbe.java`, `backend/src/main/java/com/formula11/service/ArquitecturaProbe.java`, `backend/src/main/java/com/formula11/persistence/ArquitecturaProbe.java` y `backend/src/main/java/com/formula11/dto/ArquitecturaProbe.java`; comprobar cada fallo por separado y retirar cada clase temporal.

**Checkpoint**: Las reglas FR-002 a FR-010 fallan ante su infracción temporal y todos los cambios de validación fueron revertidos.

## Phase 4: Polish & Cross-Cutting Concerns

**Purpose**: Confirmar el build completo y que no queden modificaciones temporales en producción.

- [X] T018 Con Docker abierto, ejecutar `mvn verify` desde `backend` para validar el build completo y resolver cualquier fallo del build o de Testcontainers.
- [X] T019 Ejecutar `git status --short` desde la raíz del repositorio y confirmar que no quedan cambios temporales bajo `backend/src/main/java`; no descartar cambios preexistentes o ajenos a esta feature.

## Dependencies & Execution Order

### Orden de fases

- **Setup (T001)**: Sin prerequisitos.
- **US1 (T002-T008)**: T002 depende de T001; T003-T007 dependen de la regla anterior porque se implementan en el mismo archivo; T008 depende de T002-T007.
- **Validación manual (T009-T017)**: T009 depende de T008. Ejecutar las subtareas en orden, una por vez, revirtiendo cada infracción antes de comenzar la siguiente.
- **Verificación final (T018-T019)**: T018 depende de T009-T017; T019 depende de T018.

### Dependencias de user stories

- **US1 (P1)**: Única user story; no depende de otras stories.

### Oportunidades de paralelismo

- No hay tareas paralelizables: las reglas editan el mismo `ArquitecturaTest.java` y las validaciones temporales requieren revertir cada cambio antes de continuar.

## Implementation Strategy

1. Añadir la dependencia ArchUnit.
2. Construir el test por grupos de reglas y correrlo con el perfil unitario.
3. Corregir cualquier violación real encontrada.
4. Verificar manualmente cada regla FR-002 a FR-010 con infracciones temporales, revirtiendo cada una.
5. Ejecutar `mvn verify` con Docker disponible.
6. Confirmar que no quedan cambios productivos temporales en `git status`.

**MVP**: Completar US1, incluyendo el test y la ejecución exitosa de `mvn test -Punitarios -Dtest=ArquitecturaTest`.
