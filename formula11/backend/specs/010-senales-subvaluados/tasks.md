# Tasks: Detector de jugadores subvaluados

**Feature**: `010-senales-subvaluados`
**Spec**: [spec.md](spec.md)
**Plan**: [plan.md](plan.md)

## Conventions and hard constraints

- Ejecutar los comandos desde `formula11/backend`.
- Crear la rama desde `dev` con `fix-006-versiones` y la `008` ya mergeadas.
- Única migración nueva: `V19`. No modificar migraciones existentes.
- No modificar ni borrar tests existentes. `RecalculoService` conserva su
  constructor de 5 argumentos para que `RecalculoServiceTest` no cambie.
- En `CotizacionRepository` y `MetricaPartidoRepository` solo se agregan consultas.
- No se crean endpoints de compra: la alerta usa `POST /api/ordenes/compra`.

## Dependency graph

```text
T001 -> T002 -> T003 -> T004 -> T005 -> T006 -> T007 -> T008 -> T009 -> T010 -> T011
```

## Phase 1 - Base

- [ ] **T001 - Registrar baseline**
  - Ejecutar `mvn clean test -Dtest="**/unit/**/*Test"` y registrar fallos previos sin tocar tests.
  - **Depends on**: ninguna.

- [x] **T002 - Migración V19 y entidad `Senal`**
  - Tabla `senal` con unicidad por jugador, fecha y tipo, `CHECK` del tipo e índice
    `(fecha DESC, brecha DESC)`.
  - Entidad `Senal` con validación de estrategia, brecha no negativa y precios
    positivos; `actualizar(...)` conserva el id.
  - **Depends on**: T001.

- [x] **T003 - Repositorios**
  - `SenalRepository` (`findByFecha`, `findMaxFecha`, páginas con `@EntityGraph`).
  - `CotizacionRepository.findByEstrategiaAndFecha` y
    `findPreciosByEstrategiaAndFecha`; `MetricaPartidoRepository.findJugadoresQueJugaron`.
  - **Depends on**: T002.

## Phase 2 - Dominio (US1, US3)

- [x] **T004 - `DetectorSenales` y su test**
  - `ENTRE_ESTRATEGIAS`, `PRECIO_ATRASADO` con mediana de pares, umbral inclusivo,
    mínimo de pares, ventana de score y máximo por tipo.
  - `unit/model/DetectorSenalesTest`.
  - **Depends on**: T001.

- [x] **T005 - `MedidorAciertos` y su test**
  - Variación a una semana, promedio del mercado, aciertos, tasa y diferencia
    promedio; ceros sin datos.
  - `unit/model/MedidorAciertosTest`.
  - **Depends on**: T001.

## Phase 3 - Generación (US1)

- [x] **T006 - `SenalesProperties` y `GeneradorSenalesPort`**
  - `app.senales` con `umbral`, `ventana-score`, `minimo-pares` y
    `maximo-por-tipo`, validadas al arrancar; valores en `application.yml`.
  - **Depends on**: T004.

- [x] **T007 - `SenalService.generar` y conexión con `RecalculoService`**
  - Estrategia activa, jugadores que jugaron, detector y upsert por semana.
  - `RecalculoService`: constructor `@Autowired` con
    `ObjectProvider<GeneradorSenalesPort>`; llamada por semana dentro de `try/catch`.
  - Tests: `unit/service/SenalServiceTest` (generación) y
    `unit/service/RecalculoServiceSenalesTest`.
  - **Depends on**: T003, T006.

## Phase 4 - Lecturas (US2, US3)

- [x] **T008 - `listar` y `aciertos` en `SenalService`**
  - Paginación validada, vigentes por `max(fecha)`, orden fecha, brecha e id.
  - Aciertos agrupando por fecha con la estrategia de la señal.
  - Casos en `SenalServiceTest`.
  - **Depends on**: T005, T007.

- [x] **T009 - DTOs y `SenalController`**
  - `SenalResponse`, `SenalesPageResponse`, `AciertosResponse`; `GET /api/senales` y
    `GET /api/senales/aciertos` con anotaciones de Swagger.
  - **Depends on**: T008.

## Phase 5 - Integración y cierre

- [x] **T010 - Tests de integración y E2E, Postman**
  - `integration/SenalIntegrationTest`: generación, regeneración con ids estables,
    listado y aciertos contra PostgreSQL.
  - `e2e/SenalE2ETest`: `401`, vigentes, todas, `400` y panel vacío.
  - Postman: "Senales vigentes" y "Panel de aciertos".
  - **Depends on**: T009.

- [ ] **T011 - Validación final**
  - Unitarios en local y suite completa en el CI del PR; Quality Gate de Sonar.
  - Seguir [quickstart.md](quickstart.md) en la compu de la demo después del
    recálculo completo de la temporada.
  - **Depends on**: T010.

## Definition of Done

- Cada semana recalculada genera sus señales sin romper el recálculo.
- Los dos endpoints cumplen el contrato.
- Tests unitarios, de integración y E2E en verde; Quality Gate aprobado.
- Swagger y Postman actualizados.
