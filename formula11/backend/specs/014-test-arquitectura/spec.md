# Feature Specification: Test de arquitectura del backend

**Feature Branch**: `014-test-arquitectura`

**Created**: 2026-11-25

**Status**: Draft

**Input**: User description: "Test de arquitectura del backend con ArchUnit (Entrega 3, persona C)."

## Scope

Agregar una verificación automática de la arquitectura en capas del backend. La estructura de referencia está documentada en el README: las capas son `controller`, `service`, `persistence`, `model` y `dto`; sus áreas funcionales son `usuarios`, `jugadores`, `metricas`, `cotizaciones`, `mercado`, `portfolio` y `senales`. Los paquetes `adapter`, `config`, `exception` y `security` son transversales.

El test debe ubicarse en `src/test/java/com/formula11/unit/arquitectura/ArquitecturaTest.java` para que lo incluya el perfil Maven `unitarios`. Debe detectar dependencias prohibidas, ciclos entre áreas, nombres que no respetan las convenciones y clases colocadas directamente en la raíz de una capa. No se esperan violaciones en el código productivo actual; si ArchUnit encuentra alguna, se corrige en el código productivo. No se permiten excepciones a las reglas salvo justificación y documentación explícitas.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Mantener la arquitectura al incorporar código (Priority: P1)

Como integrante del equipo que agrega funcionalidades al backend, quiero que el build compruebe automáticamente las reglas arquitectónicas para detectar una desviación antes de integrar los cambios.

**Why this priority**: Las nuevas funcionalidades de la Entrega 3 deben mantener la estructura acordada del monolito en capas.

**Independent Test**: Ejecutar el test de arquitectura en la suite normal de Maven y comprobar que termina sin iniciar el contexto de Spring ni requerir una base de datos.

**Acceptance Scenarios**:

1. **Given** que las dependencias y ubicaciones de paquetes respetan las reglas, **When** corre el build normal, **Then** el test de arquitectura pasa.
2. **Given** una dependencia o ubicación que infringe una regla, **When** corre el test de arquitectura, **Then** el test falla e identifica la regla infringida.
3. **Given** el pipeline de CI que ejecuta el build normal, **When** se incorpora una infracción arquitectónica, **Then** el pipeline falla por el test de arquitectura.

### Edge Cases

- Una clase colocada directamente en la raíz de `controller`, `service`, `persistence` o `dto` debe detectarse, aunque no viole una regla de dependencias.
- Las clases de paquetes transversales no deben tratarse como áreas funcionales al comprobar ciclos.
- Las reglas de nombres se aplican según las anotaciones y tipos indicados, no al nombre de cualquier clase que casualmente tenga un sufijo diferente.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El proyecto MUST incluir ArchUnit mediante la dependencia `archunit-junit5` con scope `test`.
- **FR-002**: El test MUST verificar el flujo de dependencias `controller` → `service` → `persistence`: los controllers no dependen de persistence; persistence no depende de controller; y service no depende de controller.
- **FR-003**: El test MUST verificar que `model` no depende de `controller`, `service` ni de `persistence`.
- **FR-004**: El test MUST verificar que `model` no depende de Spring Web ni de `dto`.
- **FR-005**: Las clases de `dto` MUST ser usadas únicamente desde `controller` o desde el propio paquete `dto`.
- **FR-006**: Las clases anotadas con `@RestController` MUST terminar en `Controller`; las clases anotadas con `@Service` MUST terminar en `Service`.
- **FR-007**: Las interfaces de repositorio de Spring Data MUST terminar en `Repository`.
- **FR-008**: Las clases de excepción MUST terminar en `Exception`.
- **FR-009**: El test MUST detectar ciclos entre subpaquetes de área de una misma capa. Las áreas son `usuarios`, `jugadores`, `metricas`, `cotizaciones`, `mercado`, `portfolio` y `senales`; los paquetes transversales no son áreas.
- **FR-010**: Las clases de `controller`, `service`, `persistence` y `dto` MUST estar bajo un subpaquete de área y no directamente en la raíz de su capa. Esta regla no aplica a `model`, que también contiene los subpaquetes de dominio `excepciones`, `valuacion` y `whoscored`.
- **FR-011**: El test MUST ejecutarse como test unitario sin iniciar Spring ni conectarse a una base de datos.
- **FR-012**: El test MUST ubicarse en `src/test/java/com/formula11/unit/arquitectura/ArquitecturaTest.java` y descubrirse en el build normal, con `-Punitarios` y en CI (`mvn verify`), sin configuración especial.
- **FR-013**: La validación de que cada regla falla ante una infracción MUST realizarse manualmente durante la implementación, introduciendo temporalmente una infracción y comprobando que el fallo identifica la regla. No se crearán clases de prueba que infrinjan reglas a propósito; el procedimiento se documenta en `quickstart.md`.
- **FR-014**: Si ArchUnit encuentra una infracción en el código productivo, MUST corregirse. Cualquier excepción a una regla MUST estar justificada y documentada; no se agregarán excepciones por conveniencia.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: La suite normal de tests de Maven ejecuta y pasa el test de arquitectura sin iniciar Spring ni requerir una base de datos.
- **SC-002**: Durante la implementación, cada regla FR-002 a FR-010 se valida manualmente mediante una infracción temporal; el test falla con un mensaje que identifica la regla y la infracción temporal se revierte. No quedan clases de prueba diseñadas para violar reglas.
- **SC-003**: El build normal y el CI detectan el test automáticamente, sin configuración o ejecución manual adicional.
- **SC-004**: La implementación pasa todas las reglas sin cambios esperados en el código productivo; si ArchUnit detecta una infracción, esta se corrige sin excepciones no justificadas.

## Assumptions

- La estructura y el listado de áreas documentados en el README son la referencia vigente para clasificar paquetes.
- Las reglas se verifican sobre las clases productivas del backend; el propio test y otras clases de test no se consideran clases de las capas.
- `model` también contiene subpaquetes de dominio que no son áreas (`excepciones`, `valuacion` y `whoscored`); por eso FR-010 no se aplica a `model`.
- Las dependencias entre `adapter`, `config`, `exception` y `security` no reciben restricciones adicionales en esta especificación, salvo las reglas explícitas que también las alcancen (por ejemplo, el uso de DTOs).
- “Repositorio de Spring Data” se refiere a los tipos que Spring Data reconoce como repositorios, no a cualquier clase de persistencia.
- “Clase de excepción” se refiere a un tipo de excepción (`Throwable`); las demás clases del paquete transversal `exception` no se fuerzan a adoptar el sufijo.
- La regla de sufijo `Service` se aplica únicamente a las clases anotadas con `@Service`. Las clases `@Component` del paquete service `CotizacionCacheInvalidator`, `CotizacionScheduler`, `DemoPortfolioInitializer`, `SuperusuarioInitializer` y `ActualizacionMetricasAdapter` quedan fuera de esa regla intencionalmente.
- Actualmente no se esperan violaciones en el código productivo; solo se modificará si la ejecución de ArchUnit detecta una.
