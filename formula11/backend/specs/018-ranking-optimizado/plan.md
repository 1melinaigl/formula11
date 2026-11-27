# Implementation Plan: Ranking optimizado para consultas frecuentes

**Branch**: `018-ranking-optimizado` | **Date**: 2026-11-26 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/018-ranking-optimizado/spec.md`; technical decisions and index evidence from [research.md](./research.md).

## Summary

Cachear `RankingResultado` con Caffeine usando como clave la estrategia resuelta, la liga y posición ya parseadas, `page` y `size`. El servicio seguirá validando y resolviendo la estrategia activa en cada llamada, pero un hit omitirá las consultas de `CotizacionRepository`. La caché tendrá `expireAfterWrite` de 15 minutos y `maximumSize` de 1000; `CotizacionCacheInvalidator` limpiará tanto la caché vigente como la del ranking al recalcular o activar una estrategia. Agregar V30 con el índice recomendado por el `EXPLAIN` representativo que ya está documentado en `research.md`.

No se cambia el contrato HTTP, las reglas de validación actuales, `RecalculoService` ni `EstrategiaService`. No se repite el `EXPLAIN`.

## Technical Context

**Language/Version**: Java 21

**Primary Dependencies**: Spring Boot 3.5.5, Caffeine, Spring Data JPA, Flyway

**Storage**: PostgreSQL

**Testing**: JUnit 5, Mockito, Maven, PostgreSQL 16 Testcontainers para integración

**Target Platform**: Backend Java/Spring Boot

**Project Type**: API web monolítica en capas

**Performance Goals**: Un hit de caché no invoca `CotizacionRepository`; `EstrategiaRepository.findByActivaTrue` sí se invoca y los permisos se verifican en cada pedido.

**Constraints**: TTL de 15 minutos y máximo 1000 entradas; invalidación explícita como mecanismo principal de frescura; clave basada en valores resueltos; ningún cambio de validaciones, respuesta ni errores HTTP; paquetes nuevos de lógica bajo `service/cotizaciones` y configuración bajo `config`.

**Scale/Scope**: Ranking paginado por estrategia, liga, posición y página; una clave puede representar un resultado vacío. Una migración de índice y cambios acotados a los servicios, configuración y pruebas de cotizaciones.

## Constitution Check

| Principio | Estado | Justificación |
|---|---|---|
| Mantener la arquitectura en capas | Conforme | La configuración del bean queda en `config`; `RankingClave` y la lógica de consulta/invalidador quedan en `service/cotizaciones`. |
| Validar antes de acceder a caché | Conforme | `ranking` conserva el orden actual: validar paginación, parsear filtros y resolver estrategia/autorización antes de armar la clave y llamar `cache.get`. |
| Preservar el contrato | Conforme | No se modifican rutas, parámetros, DTOs, ordenamiento, validaciones ni errores del endpoint. |
| Mantener consistencia de lecturas | Conforme | Invalidar ambas cachés desde el invalidator ya usado por recálculo y activación; no cachear la estrategia activa. |
| Mantener las categorías de pruebas | Conforme | Comportamiento del servicio e invalidator en `unit`; invalidación real y migración en `integration/cotizaciones`. |
| Evitar complejidad innecesaria | Conforme | Un record como clave, un bean Caffeine tipado, el invalidator existente y una única migración nueva; sin cambios en los servicios que ya llaman al invalidator. |

No hay gates constitucionales pendientes. Revalidar `ArquitecturaTest` tras los cambios.

## Design Decisions

### Resolución y clave

Mantener al comienzo de `CotizacionConsultaService.ranking` el flujo existente de validación: `validarPaginacion`, `parsearLiga`, `parsearPosicion` y `resolverEstrategia`. Este último debe seguir consultando `EstrategiaRepository.findByActivaTrue` y comprobando autorización en cada pedido.

Agregar `service/cotizaciones/RankingClave.java` como record con `String estrategia`, `Liga liga`, `String posicion`, `int page` e `int size`. La estrategia puede ser `null` si no hay estrategia activa; la clave como objeto no es nula. Usar `Liga` y la posición ya normalizada por el parser, no los textos crudos. Así, las formas de entrada que el parser actual ya acepta y resuelve al mismo `Liga` comparten entrada. No extender el conjunto de valores HTTP aceptados: por ejemplo, no agregar alias nuevos para liga.

### Caché de ranking

Agregar `config/RankingCacheConfig.java` con un bean tipado `Cache<RankingClave, RankingResultado>`, construido con Caffeine, `expireAfterWrite(Duration.ofMinutes(15))` y `maximumSize(1000)`. El tipo genérico diferencia este bean del cache existente `Cache<String, CotizacionVigentePort.CotizacionVigente>`.

Inyectar el bean en `CotizacionConsultaService`. Después de la validación y resolución de estrategia, construir la clave y devolver `cache.get(clave, k -> calcularRanking(k))`. Extraer a `calcularRanking(RankingClave)` el cálculo existente de fecha máxima, página, precios anteriores, versión y creación de `RankingResultado`. El método seguirá ejecutándose bajo el `@Transactional(readOnly = true)` de la clase. Los resultados vacíos son `RankingResultado` no nulos, por lo que también quedan cacheados.

No guardar ni cachear la estrategia activa. El cálculo de una entrada debe usar la estrategia codificada en su clave y los filtros ya resueltos. En un hit solo se omiten las consultas de `CotizacionRepository`; la consulta de estrategia activa y las validaciones quedan fuera de la función de carga.

### Invalidación

Extender `CotizacionCacheInvalidator` para recibir además `Cache<RankingClave, RankingResultado>`. `invalidate()` ejecutará `invalidateAll()` en ambos caches. No cambiar `RecalculoService` ni `EstrategiaService`: ambos ya llaman al invalidator y la activación ya invalida dentro de su transacción antes del commit.

### Índice

Agregar `backend/src/main/resources/db/migration/V30__indice_ranking_cotizacion.sql` con:

```sql
CREATE INDEX IF NOT EXISTS ix_cotizacion_ranking
    ON cotizacion (estrategia, fecha, score DESC, precio DESC);
```

La decisión y el plan comparativo ya están en la sección **EXPLAIN con datos representativos** de `research.md`: V18 filtra estrategia/fecha y ordena una cantidad mayor de filas; V30 permite lectura ordenada por score y precio y deja el desempate por nombre para el `Incremental Sort`. No modificar V18 ni volver a ejecutar EXPLAIN como parte de este plan.

### Tests y persistencia de `Jugador`

- Actualizar `CotizacionConsultaServiceTest` para construir e inyectar una caché Caffeine aislada por test.
- Verificar hit de igual clave: el segundo llamado no repite ninguna consulta de `CotizacionRepository`, pero `EstrategiaRepository.findByActivaTrue` se invoca en ambos pedidos.
- Cubrir claves distintas, resultados vacíos cacheados, nueva consulta después de `invalidate()` y rechazo de no-superusuario para una estrategia alternativa aunque exista una entrada precargada.
- Actualizar `CotizacionCacheInvalidatorTest` para comprobar que se invaliden ambas cachés.
- Adaptar cualquier otro `new CotizacionConsultaService(...)` o `new CotizacionCacheInvalidator(...)` que encuentre el compilador/búsqueda al nuevo parámetro de constructor.
- En `CotizacionConsultaIntegrationTest`, invalidar ambas cachés en `@BeforeEach` antes de sembrar cotizaciones directamente. Agregar el caso de activación: calentar el ranking, activar la estrategia alternativa sembrada por el test y comprobar que el siguiente pedido sin estrategia explícita refleja el ranking de la nueva activa; restaurar la estrategia original en la limpieza del test. Comprobar además en `pg_indexes` que Flyway creó V30 con la definición esperada.
- En `CotizacionJugadorE2ETest`, invalidar las cachés en `@BeforeEach` antes de sembrar cotizaciones directamente y consultar `GET /api/jugadores/ranking`.
- En `RecalculoServiceIntegrationTest`, invalidar las cachés en `@BeforeEach`; agregar un caso que calcule cotizaciones iniciales, consulte y guarde el ranking cacheado, cambie los datos de métricas y recalcule el mismo rango. Comparar un mismo jugador antes y después y exigir que su `score` o `precio` sea distinto en el pedido posterior al recálculo.
- El barrido de tests de integración/E2E encontró otros seeders directos (`CotizacionRepositoryIntegrationTest` y `SenalIntegrationTest`), pero no consultan el ranking; no requieren invalidación adicional mientras mantengan ese alcance.
- Mantener el mapeo de `RankingResultado.Item` a la respuesta limitado a los campos simples de `Jugador`; no acceder a relaciones lazy ni mutar la entidad cacheada.

## Implementation Sequence

1. Añadir `RankingClave` y el bean Caffeine tipado en `config`; comprobar que Spring resuelve los dos beans `Cache` por sus tipos genéricos.
2. Refactorizar `CotizacionConsultaService.ranking` para preservar la validación/resolución antes del cache lookup y mover solo la construcción actual del ranking a `calcularRanking`.
3. Ampliar `CotizacionCacheInvalidator` para limpiar ambos caches y adaptar las construcciones manuales en los tests unitarios.
4. Añadir y probar la migración V30 sin alterar migraciones existentes.
5. Completar pruebas unitarias de hit/miss, normalización, filtros, resultados vacíos, invalidación y autorización.
6. Completar la integración de activación en `CotizacionConsultaIntegrationTest`, el recálculo con cambio observable de datos en `RecalculoServiceIntegrationTest` y la comprobación de V30.
7. Aislar el ranking compartido entre tests invalidándolo desde `@BeforeEach` en `CotizacionConsultaIntegrationTest`, `CotizacionJugadorE2ETest` y `RecalculoServiceIntegrationTest`.
8. Ejecutar tests dirigidos, `ArquitecturaTest` y el build completo `mvn clean verify` con Docker disponible para Testcontainers.

## Project Structure

### Documentation (this feature)

```text
backend/specs/018-ranking-optimizado/
├── spec.md
├── research.md
└── plan.md
```

No se necesita `data-model.md`: no se agregan entidades ni columnas. No se modifica un contrato externo ni se necesita carpeta `contracts/`. Las pruebas de carga son opcionales y quedan fuera de este plan.

### Source Code

```text
backend/
├── src/main/java/com/formula11/
│   ├── config/
│   │   └── RankingCacheConfig.java                       # Bean Cache<RankingClave, RankingResultado>
│   └── service/cotizaciones/
│       ├── RankingClave.java                             # Clave de caché normalizada
│       ├── CotizacionConsultaService.java                # Lookup y cálculo del ranking
│       └── CotizacionCacheInvalidator.java               # Invalida ambas cachés
├── src/main/resources/db/migration/
│   └── V30__indice_ranking_cotizacion.sql                # Índice de ranking
└── src/test/java/com/formula11/
    ├── unit/service/cotizaciones/
    │   ├── CotizacionConsultaServiceTest.java            # Hit/miss, permisos y claves
    │   └── CotizacionCacheInvalidatorTest.java           # Limpieza de ambas cachés
    ├── integration/cotizaciones/
        ├── CotizacionConsultaIntegrationTest.java        # Activación, aislamiento y V30
        └── RecalculoServiceIntegrationTest.java          # Frescura tras recálculo
    └── e2e/cotizaciones/
        └── CotizacionJugadorE2ETest.java                 # Aislamiento del ranking E2E
```

Los nombres de archivo para tests son los existentes; ajustar las rutas exactas solo si el código actual ubica algún constructor adicional en otra clase de test.

**Structure Decision**: Mantener el backend Maven monolítico y sus paquetes existentes. La configuración queda transversal en `config`; la clave, el servicio y el invalidator permanecen bajo `service/cotizaciones`; la migración es la única modificación de esquema.

## Verification

Ejecutar desde `backend`:

```powershell
mvn -Punitarios -Dtest=CotizacionConsultaServiceTest,CotizacionCacheInvalidatorTest test
mvn -Dtest=CotizacionConsultaIntegrationTest,RecalculoServiceIntegrationTest test
mvn -Dtest=CotizacionJugadorE2ETest test
mvn -Punitarios -Dtest=ArquitecturaTest test
mvn clean verify
```

Ejecutar desde `backend`, con Docker abierto y disponible para Testcontainers. Si la selección de tests por clase no está habilitada por el perfil Maven, ejecutar `mvn test -Punitarios` para la suite unitaria. No volver a correr EXPLAIN: usar la decisión representativa ya documentada en `research.md`.

## Complexity Tracking

No hay excepciones a la arquitectura ni complejidad adicional que requiera justificación.
