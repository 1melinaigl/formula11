# Research: Test de arquitectura del backend

## Decisions

### Dependencia ArchUnit

- **Decision**: Agregar `com.tngtech.archunit:archunit-junit5:1.4.1` con scope `test`.
- **Rationale**: Es la dependencia solicitada para ejecutar reglas ArchUnit con JUnit 5. Al ser test-only no cambia las dependencias de runtime.
- **Alternatives considered**: Comprobaciones manuales de imports o pruebas propias por reflexión; no ofrecen reglas declarativas de arquitectura ni mensajes ArchUnit.

### Importación de clases

- **Decision**: Importar `com.formula11` con `ClassFileImporter` y `ImportOption.Predefined.DO_NOT_INCLUDE_TESTS` en un `@BeforeAll`, y verificar cada regla en un método `@Test` de JUnit.
- **Rationale**: Las reglas se aplican al código de producción y no se contaminan con los propios tests o sus fixtures. La clase de arquitectura queda bajo `unit/arquitectura` para que Maven la encuentre.
- **Hallazgo**: Con campos `@ArchTest` y `@AnalyzeClasses`, Surefire 3.5.3 reportaba 0 tests y no fallaba ante una infracción, aunque el lanzador de JUnit ejecutado a mano sí descubría y ejecutaba las 14 reglas. Por eso se usan métodos `@Test`.
- **Regla de DTOs**: Se verifica con `noClasses().that().resideOutsideOfPackages(controller, dto).should().dependOnClassesThat().resideInAPackage(dto)`. `onlyBeAccessed()` no detectaba un campo de tipo DTO en un service, porque solo considera accesos (llamadas y lecturas), no tipos de campos, parámetros ni retornos.
- **Alternatives considered**: Arrancar Spring o importar clases de test; no es necesario para verificar dependencias estáticas y añadiría contexto ajeno a un test unitario.

### Modelo de capas y accesos transversales observados

- **Decision**: Usar `layeredArchitecture().consideringOnlyDependenciesInLayers()` y definir únicamente `Controller`, `Service`, `Persistence`, `Model` y `Dto`. Los paquetes transversales no participan en ese grafo. `Service` puede ser accedido por `Controller` y `Dto`: la segunda dirección es una excepción acotada, confirmada por los mapeadores existentes.
- **Rationale**: La API limita la comprobación a dependencias entre las capas definidas, de modo que no se convierten los paquetes de soporte en capas ni se amplía la arquitectura acordada.
- **Evidence**: `config/CotizacionCacheConfig` referencia `service.cotizaciones.CotizacionVigentePort` (`src/main/java/com/formula11/config/CotizacionCacheConfig.java:5,13`). `exception/GlobalExceptionHandler` referencia `service.usuarios.UsuarioService` para manejar `EmailDuplicadoException` (`src/main/java/com/formula11/exception/GlobalExceptionHandler.java:6,73-74`). Las búsquedas de `service` en `adapter` y `security` no encontraron referencias.
- **Current core-layer scan**: La primera ejecución del test encontró 167 dependencias de DTOs hacia tipos de `service`, incluidos resultados usados por mapeadores de respuestas como `CotizacionLeida`, `EstrategiaResultado` y `PaginaJugadores`. No encontró dependencias de `controller` a `persistence`, de `persistence` a `controller`/`service`/`dto`, ni de `model` a `controller`/`service`/`persistence`/`dto` o Spring Web. Tampoco encontró usos de tipos DTO desde `service`, `persistence`, `model` ni los paquetes transversales revisados.
- **Alternatives considered**: Añadir `Config` y `Exception` como capas o excepciones puntuales a la regla de servicio. No se elige: esos paquetes están definidos como transversales, no como capas, y la configuración solicitada limita explícitamente el análisis a dependencias entre las cinco capas.
- **Resolution**: Se permite que `Dto` acceda a `Service` porque los DTOs existentes convierten resultados de servicio en respuestas HTTP, y esa dependencia no contradice la regla de acceso de tipos DTO (los tipos del paquete `dto` siguen siendo usados solo por `controller` y `dto`). Esta excepción concreta evita mover o duplicar mapeadores y no habilita dependencias desde `Service` hacia `Dto`. Las restantes dependencias prohibidas que detecte el test deben corregirse en el código productivo; no se agregan otras excepciones sin justificación explícita.

### Regla de dependencias del dominio

- **Decision**: Comprobar por separado que `model` no dependa de `controller`, `service` ni `persistence`, y que tampoco dependa de Spring Web ni de `dto`.
- **Rationale**: El modelo es independiente de las capas externas y de la infraestructura HTTP; `FR-003` y `FR-004` declaran estas restricciones.
- **Alternatives considered**: Confiar únicamente en la regla global de capas; se rechaza porque la regla explícita deja claro el límite de `model` y produce un diagnóstico identificable.

### DTOs, nombres y ubicación de clases

- **Decision**: Usar reglas ArchUnit por anotación y asignabilidad para los sufijos; restringir el acceso a DTOs a controller/dto; comprobar los paquetes raíz con nombres exactos, sin patrones recursivos.
- **Rationale**: Las comprobaciones reflejan el alcance intencional de cada convención y evitan exigir sufijos a componentes que no tienen la anotación correspondiente o a clases bajo subpaquetes.
- **Alternatives considered**: Aplicar los sufijos por paquete o a todos los componentes; se rechaza porque sobregeneraliza las convenciones. Los `@Component` de service descritos en la spec quedan fuera de la regla `Service`.

### Ciclos entre áreas

- **Decision**: Comprobar ciclos por capa, separando las siete áreas funcionales y no agrupando paquetes de distintos niveles en el mismo slice.
- **Rationale**: La regla es “sin ciclos entre subpaquetes de área de una misma capa”. `model/excepciones`, `model/valuacion` y `model/whoscored` no son áreas y no deben convertirse en slices funcionales accidentalmente.
- **Alternatives considered**: Un matcher genérico sobre todos los subpaquetes de las cinco capas; se rechaza porque mezclaría capas y categorizaría como áreas los subpaquetes especiales de `model`.

## Validation Decisions

- El test se ejecuta con `mvn test -Punitarios`, `mvn test` y `mvn verify`; la ruta `src/test/java/com/formula11/unit/arquitectura` coincide con el include existente del perfil `unitarios`.
- La capacidad de fallo de cada regla se comprueba manualmente durante la implementación con infracciones temporales en producción, revertidas tras cada ejecución. No se dejan clases deliberadamente inválidas en el repositorio.
- No se esperan cambios productivos. Si una regla encuentra una violación real, se corrige el código y se vuelve a ejecutar la suite.
