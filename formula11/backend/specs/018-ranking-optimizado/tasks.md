# Tasks: Ranking optimizado para consultas frecuentes

**Input**: Documentos de diseño en `backend/specs/018-ranking-optimizado/`

**Prerrequisitos**: `plan.md`, `spec.md`, `research.md`

**Verificación completa**: La última tarea ejecuta `mvn clean verify` desde `backend`, con Docker abierto para que Testcontainers pueda iniciar PostgreSQL.

## Formato

`[ID] [P?] [Story] Descripción`

- `[P]` indica tareas independientes que pueden avanzar en paralelo.
- `US1`: cachear consultas repetidas del ranking.
- `US2`: reflejar cambios tras recálculo o activación.
- `US3`: usar y verificar el índice del ranking.

## Fase 1: Infraestructura compartida

**Propósito**: Preparar clave, bean, servicio y limpieza antes de validar los recorridos de usuario.

- [X] T001 [US1] Crear el record `RankingClave` con estrategia resuelta (nullable), `Liga`, posición normalizada, `page` y `size` en `backend/src/main/java/com/formula11/service/cotizaciones/RankingClave.java`; crear el bean `Cache<RankingClave, RankingResultado>` con expiración de 15 minutos y máximo 1000 entradas en `backend/src/main/java/com/formula11/config/RankingCacheConfig.java`.
- [X] T002 [US1] Refactorizar `CotizacionConsultaService.ranking` en `backend/src/main/java/com/formula11/service/cotizaciones/CotizacionConsultaService.java`: mantener validación, parseo y autorización antes del cache lookup; usar `cache.get(clave, k -> calcularRanking(k))` y mover el cálculo existente a un método privado.
- [X] T003 [US2] Ampliar `CotizacionCacheInvalidator` en `backend/src/main/java/com/formula11/service/cotizaciones/CotizacionCacheInvalidator.java` para recibir el cache del ranking e invalidar ambos caches sin modificar `RecalculoService` ni `EstrategiaService`.
- [X] T004 [US3] Crear `backend/src/main/resources/db/migration/V30__indice_ranking_cotizacion.sql` con el índice recomendado en `research.md`: `ix_cotizacion_ranking` sobre `(estrategia, fecha, score DESC, precio DESC)`, sin modificar migraciones existentes.

## Fase 2: US1 - Reutilizar resultados idénticos (P1)

**Objetivo**: Evitar consultas repetidas a `CotizacionRepository` y conservar las validaciones por pedido.

- [X] T005 [US1] Adaptar la construcción de `CotizacionConsultaService` e inyectar un cache Caffeine aislado por test en `backend/src/test/java/com/formula11/unit/service/cotizaciones/CotizacionConsultaServiceTest.java`.
- [X] T006 [US1] En `CotizacionConsultaServiceTest`, comprobar con `verify(..., times(1))` que el segundo pedido idéntico no vuelve a invocar `CotizacionRepository`, mientras `EstrategiaRepository.findByActivaTrue` se verifica con `times(2)`.
- [X] T007 [US1] En `CotizacionConsultaServiceTest`, cubrir claves diferentes por estrategia, liga, posición, página y tamaño; comprobar que los valores de liga ya aceptados que resuelven al mismo `Liga` comparten entrada, sin ampliar el parser HTTP.
- [X] T008 [US1] En `CotizacionConsultaServiceTest`, comprobar que un resultado vacío también se cachea y que `invalidate()` fuerza una nueva consulta.
- [X] T009 [US1] En `CotizacionConsultaServiceTest`, precargar la entrada de otra estrategia y comprobar que el pedido de un usuario no superusuario sigue lanzando `SoloSuperusuarioException` antes de consultar el cache.
- [X] T010 [US2] Adaptar `backend/src/test/java/com/formula11/unit/service/cotizaciones/CotizacionCacheInvalidatorTest.java` para verificar que `invalidate()` llama `invalidateAll()` tanto al cache vigente como al cache del ranking.

## Fase 3: US2 - Datos nuevos tras cambios (P1)

**Objetivo**: Demostrar invalidación con activación y recálculo, y evitar contaminación entre clases de test con contexto Spring compartido.

- [X] T011 [US2] En `backend/src/test/java/com/formula11/integration/cotizaciones/CotizacionConsultaIntegrationTest.java`, inyectar el invalidator e invalidar en `@BeforeEach` antes de sembrar cotizaciones directamente; agregar un caso que active la estrategia alternativa y verifique el ranking nuevo; restaurar la estrategia original después del test.
- [X] T012 [US2] En `backend/src/test/java/com/formula11/integration/cotizaciones/RecalculoServiceIntegrationTest.java`, invalidar el ranking en `@BeforeEach` y agregar un caso que calcule cotizaciones iniciales, guarde el ranking cacheado, cambie las métricas y recalcule el mismo rango; verificar que el score o precio de un mismo jugador difiere después del recálculo.
- [X] T013 [US2] En `backend/src/test/java/com/formula11/e2e/cotizaciones/CotizacionJugadorE2ETest.java`, inyectar `CotizacionCacheInvalidator` e invalidar en `@BeforeEach` antes de sembrar cotizaciones y consultar `GET /api/jugadores/ranking`.

## Fase 4: US3 - Índice del ranking (P2)

**Objetivo**: Confirmar que Flyway instala la migración decidida por el `EXPLAIN` representativo ya documentado.

- [X] T014 [US3] En `CotizacionConsultaIntegrationTest`, consultar `pg_indexes` y comprobar que `ix_cotizacion_ranking` fue creado por V30 con las columnas y dirección de orden esperadas.

## Fase 5: Verificación

**Propósito**: Detectar regresiones funcionales, de aislamiento, arquitectura e integración.

- [X] T015 Ejecutar las pruebas unitarias dirigidas de `CotizacionConsultaServiceTest` y `CotizacionCacheInvalidatorTest`; ejecutar las pruebas de integración dirigidas de `CotizacionConsultaIntegrationTest` y `RecalculoServiceIntegrationTest`, la E2E `CotizacionJugadorE2ETest` y `ArquitecturaTest`. Corregir cualquier regresión sin cambiar el contrato HTTP.
- [X] T016 Ejecutar el build completo `mvn clean verify` desde `backend` con Docker abierto y disponible para Testcontainers; confirmar que finaliza exitosamente. Esta es la última tarea.

## Dependencias y orden

- T001 precede a T002, T003 y a los tests que construyen esos beans/servicios.
- T002 y T003 preceden a T005–T013.
- T004 precede a T014.
- T011, T012 y T013 son modificaciones de clases distintas y pueden implementarse en paralelo una vez listos T002 y T003.
- T011 y T004 preceden a T014, que añade la verificación de V30 a `CotizacionConsultaIntegrationTest`.
- T015 requiere terminar las tareas de implementación y pruebas; T016 es la verificación final, después de T015.

## Cobertura de aislamiento revisada

El barrido encontró consultas al ranking en `CotizacionConsultaIntegrationTest` y `CotizacionJugadorE2ETest`; ambas siembran cotizaciones directamente y quedan incluidas en T011 y T013. `RecalculoServiceIntegrationTest` también se incluye porque el nuevo caso consultará el ranking y su setup limpia/siembra cotizaciones. `CotizacionRepositoryIntegrationTest` y `SenalIntegrationTest` siembran cotizaciones, pero no consultan ranking en su alcance actual; no requieren invalidación adicional mientras eso no cambie.
