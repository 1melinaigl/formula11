# Tasks: Detalle de jugador, historial, cotización a una fecha y ranking

**Feature**: `008-cotizacion-fecha-ranking`  
**Spec**: [spec.md](spec.md)  
**Plan**: [plan.md](plan.md)

## Conventions and hard constraints

- Ejecutar los comandos desde `formula11/backend`.
- Crear la rama desde `dev` con la `011` ya mergeada.
- Única migración nueva: `V18`. No modificar V1–V3, V10–V17 ni V20–V25.
- No modificar `CotizacionHistoricaPort`, `CotizacionVigentePort`,
  `CotizacionVigenteService`, `PortfolioService`, `Cotizacion`, `Jugador`, `Liga`,
  `JugadorController` ni `JugadorService`.
- En `CotizacionRepository` solo se agregan métodos y `JpaSpecificationExecutor`;
  no se cambia ni borra ninguno existente.
- No modificar ni borrar tests existentes sin el "sí" explícito del equipo
  (constitución, punto 6). Las únicas excepciones son las de T007, con el "sí"
  de A del 2026-10-29.
- Los tests de integración no usan `deleteAll` sobre `jugadores` ni `cotizacion`:
  limpian por los ids que crean.

## Dependency graph

```text
T001 -> T002 -> T003
T001 -> T004
T003 -> T005 -> T006 -> T007
T004 + T003 -> T008 -> T009 -> T010 -> T011
T008 -> T012 -> T013
T011 + T013 + T006 -> T014 -> T015 -> T016
```

## Phase 1 - Base

- [ ] **T001 - Registrar baseline**
  - Ejecutar `mvn clean test -Dtest="**/unit/**/*Test"` en la rama nueva y
    registrar cualquier fallo preexistente sin tocar esos tests.
  - **Depends on**: ninguna.

- [x] **T002 - Crear migración V18**
  - `src/main/resources/db/migration/V18__indice_cotizacion_estrategia_fecha.sql`
    con `CREATE INDEX IF NOT EXISTS ix_cotizacion_estrategia_fecha ON cotizacion (estrategia, fecha);`.
  - **Depends on**: T001.

- [x] **T003 - Agregar consultas a `CotizacionRepository`**
  - Extender `JpaSpecificationExecutor<Cotizacion>`.
  - Agregar `findMaxFechaByEstrategia`, `findFechaAnterior`,
    `findVersionByEstrategiaAndFecha`, `findByEstrategiaAndFechaAndJugadorIdIn`,
    `findFechasByEstrategiaBetween`, `findVigentesA` y el `findAll(Specification, Pageable)`
    con `@EntityGraph(attributePaths = "jugador")`, según el plan.
  - **Depends on**: T002.

- [x] **T004 - Crear `VariacionCotizacion` y su test**
  - `model/VariacionCotizacion.porcentaje(anterior, actual)`: `null` sin anterior,
    `(actual / anterior - 1) * 100` con 2 decimales `HALF_UP`,
    `IllegalArgumentException` si el anterior no es positivo.
  - `unit/model/VariacionCotizacionTest`: primera, suba, baja, sin cambio,
    redondeo en el límite y anterior no positivo.
  - **Depends on**: T001.

## Phase 2 - Puerto para la 009 (US5)

- [x] **T005 - Implementar `CotizacionHistoricaService`**
  - `service/CotizacionHistoricaService implements CotizacionHistoricaPort`,
    `@Service`, `@Transactional(readOnly = true)`.
  - Reglas de FR-010 y FR-010a: nulos, rango invertido y sin activa devuelven vacío;
    ids sin cotización valen `1.0000`; una entrada por id distinto; una sola
    consulta para todos los ids.
  - `unit/service/CotizacionHistoricaServiceTest` con mocks.
  - **Depends on**: T003.

- [x] **T006 - Borrar el adapter provisorio**
  - Borrar `adapter/CotizacionHistoricaFijaAdapter.java` (FR-009a de la `009`).
  - Verificar que no queden referencias en `src/main`.
  - **Depends on**: T005.

- [x] **T007 - Borrar `CotizacionHistoricaFijaAdapterTest` (requiere "sí")**
  - Pedir el "sí" explícito del equipo y registrarlo en la sesión de
    Clarifications del spec antes de borrar
    `unit/adapter/CotizacionHistoricaFijaAdapterTest.java`.
  - Sin el "sí", la tarea queda bloqueada y T006 no se puede mergear, porque el
    test no compila sin la clase.
  - Con el mismo "sí", agregar en `e2e/PortfolioEvolucionE2ETest` un
    `@MockBean CotizacionHistoricaPort` que reproduzca el adapter provisorio
    (un lunes, precio 1) para `evolucionReconstruyeTransaccionesEnUtc`, sin
    cambiar sus aserciones.
  - **Depends on**: T006.

## Phase 3 - Lecturas por jugador (US1, US2, US3)

- [x] **T008 - Crear `SoloSuperusuarioException` y records de resultado**
  - `exception/SoloSuperusuarioException` y su handler `403` en
    `GlobalExceptionHandler`, con el mensaje "Solo el superusuario puede
    consultar una estrategia que no es la activa".
  - Records en `service`: `CotizacionLeida(jugadorId, fecha, precio, score,
    variacion, estrategia, version)`, `JugadorDetalleResultado`,
    `HistorialCotizacionesResultado`, `RankingResultado` (con sus ítems).
  - **Depends on**: T003, T004.

- [x] **T009 - Implementar `CotizacionConsultaService` para detalle, historial y vigente**
  - `resolverEstrategia`, fecha de negocio con `Clock` y `ZONA_NEGOCIO`,
    `404` de jugador, rango inválido `400`, historial con variación calculada
    antes de filtrar el rango, vigente con la anterior a `fecha - 1 día`.
  - Precio y score con 4 decimales `HALF_UP`.
  - **Depends on**: T008.

- [x] **T010 - Crear DTOs y `CotizacionJugadorController` (detalle, historial, vigente)**
  - DTOs `JugadorDetalleResponse`, `CotizacionResponse`,
    `HistorialCotizacionesResponse` con los nombres del contrato.
  - Controller `@Validated` en `/api/jugadores`, `@Positive` en `jugadorId`,
    rol tomado de `Authentication`, anotaciones de Swagger con los códigos del contrato.
  - **Depends on**: T009.

- [x] **T011 - Tests de US1, US2 y US3**
  - `unit/service/CotizacionConsultaServiceTest` con mocks y `Clock.fixed`:
    los casos del plan para detalle, historial y vigente.
  - `e2e/CotizacionJugadorE2ETest`: `401`, `200`, `400` (id negativo, fecha
    inválida, `desde > hasta`), `403` y `404`.
  - **Depends on**: T010.

## Phase 4 - Ranking (US4)

- [x] **T012 - Implementar el ranking en el service**
  - Validar `page`, `size`, liga legible y posición.
  - Última fecha de la estrategia; página con `Specification` y orden
    `score DESC, precio DESC, jugador.nombre ASC, jugador.id ASC`; variación
    con una sola consulta a la fecha anterior; `puesto = page * size + i + 1`.
  - Sin cotizaciones: página vacía con `version` y `fecha` en `null`.
  - **Depends on**: T008.

- [x] **T013 - Endpoint y tests del ranking**
  - DTOs `RankingItemResponse` y `RankingPageResponse`; `GET /api/jugadores/ranking`.
  - Casos unitarios del ranking en `CotizacionConsultaServiceTest`.
  - E2E: `200`, filtros, `403`, `400` y que `/ranking` no caiga en `/{jugadorId}`.
  - **Depends on**: T012.

## Phase 5 - Integración, documentación y cierre

- [x] **T014 - Test de integración de consultas y puerto**
  - `integration/CotizacionConsultaIntegrationTest`: jugadores y cotizaciones
    propios (dos estrategias, tres domingos), ranking con filtros, desempate y
    paginación, `findVigentesA`, `findFechasByEstrategiaBetween` y el puerto real.
  - Limpieza por ids creados.
  - **Depends on**: T011, T013, T006.

- [x] **T015 - Postman y Swagger**
  - Agregar a `postman/formula11-registro-catalogo.json` una carpeta `008` con
    los cuatro requests y variables de `jugadorId` y token.
  - Revisar en `/swagger-ui.html` que los cuatro endpoints muestren parámetros y
    respuestas del contrato.
  - **Depends on**: T014.

- [ ] **T016 - Validación final**
  - `mvn clean test -Dtest="**/unit/**/*Test"` en local.
  - PR contra `dev`; CI y Quality Gate de SonarCloud en verde.
  - Seguir [quickstart.md](quickstart.md) en la compu de la demo después de la
    carga de la `011` y el recálculo de la `006`.
  - **Depends on**: T015.

## Definition of Done

- Los cuatro endpoints cumplen el contrato, incluidos los códigos de error.
- `CotizacionHistoricaPort` tiene una sola implementación y la evolución de la
  `009` muestra precios reales.
- Tests unitarios, de integración y E2E en verde; Quality Gate aprobado.
- Swagger y Postman actualizados.
