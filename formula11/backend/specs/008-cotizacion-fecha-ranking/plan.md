# Implementation Plan: Detalle de jugador, historial, cotización a una fecha y ranking

**Branch**: `008-cotizacion-fecha-ranking` | **Date**: 2026-10-28 | **Spec**: [spec.md](spec.md)

## Summary

La feature agrega cuatro endpoints de lectura sobre la tabla `cotizacion` de la
`006` y la implementación real de `CotizacionHistoricaPort` que consume la
`009`. No hay escrituras, no hay llamadas a proveedores externos y no se crean
tablas. Toda la lógica de lectura vive en dos services nuevos; la variación
porcentual es una regla de dominio pura en el modelo. Los controllers solo
traducen HTTP, leen el rol del JWT y mapean a los DTOs del contrato.

## Technical Context

- **Language/Version**: Java 21.
- **Framework**: Spring Boot 3.5.5 (Web, Data JPA, Security, Validation), springdoc.
- **Storage**: PostgreSQL 16 con Flyway. Tabla existente `cotizacion`
  (`jugador_id`, `fecha`, `precio NUMERIC(19,4)`, `score NUMERIC(19,8)`,
  `estrategia`, `version`, `pesos`), única por `(jugador_id, estrategia, fecha)`,
  con índice `(jugador_id, fecha DESC)`.
- **Testing**: JUnit 5 y Mockito para unitarios; Testcontainers
  (`jdbc:tc:postgresql:16-alpine`) para integración; MockMvc sobre
  `@SpringBootTest` en el paquete `e2e`.
- **Tiempo**: `Clock` UTC inyectado; la fecha de negocio se obtiene con
  `ClockConfig.ZONA_NEGOCIO` (`America/Argentina/Buenos_Aires`).
- **Volumen esperado**: unos 2.500 jugadores, 2 estrategias y menos de 52
  semanas: menos de 260.000 filas por temporada.
- **Migraciones**: rango de A. `V13`–`V17` las usa la `011`; esta feature usa
  `V18`; `V19` queda libre.
- **Calidad**: Quality Gate de SonarCloud con cobertura del código nuevo de al
  menos 80 %. Los DTOs están excluidos de cobertura por el `pom.xml`.

## Constitution Check

| Principio | Cumplimiento |
|---|---|
| 1. Capas | Controller → Service → Repository. El puerto se implementa en `service`, igual que `CotizacionVigenteService`. Sin scheduler. |
| 1. Caché | No aplica: no hay proveedor externo. La caché de cotización vigente de la `006` no se toca. |
| 2. Modelo rico | La variación porcentual vive en `model/VariacionCotizacion`, sin Spring ni base de datos. |
| 3. Validaciones | Controller: tipos, `@Positive`, `page` y `size`. Service: estrategia permitida, liga y posición del contrato, rango de fechas, existencia del jugador, permiso por rol. |
| 4. Seguridad | Todos los endpoints requieren JWT. Otra estrategia que no sea la activa: solo `SUPERUSUARIO` (`403`). |
| 5. Observabilidad | Errores con `Error` y `correlationId` del filtro existente. Log `DEBUG` de consultas sin datos sensibles. |
| 6. Tests | Unitarios de dominio y services, integración de repositorio y puerto, E2E en `e2e`. Borrar `CotizacionHistoricaFijaAdapterTest` requiere el "sí" explícito. |
| 7. DoD | Swagger con anotaciones y colección de Postman actualizada. |
| 8. Idioma | Nombres de dominio en español sin acentos; mensajes de error en español. |
| 9. Clock | `CotizacionConsultaService` recibe `Clock`; no usa `LocalDate.now()` sin él. |
| 10. Tokens | No aplica. |

Sin violaciones; no hace falta tabla de complejidad.

## Architecture and Design

### Package layout

```text
com.formula11
├── controller/CotizacionJugadorController.java      (nuevo, 4 GET)
├── dto/JugadorDetalleResponse.java                  (nuevo)
├── dto/CotizacionResponse.java                      (nuevo, esquema Cotizacion)
├── dto/HistorialCotizacionesResponse.java           (nuevo)
├── dto/RankingItemResponse.java                     (nuevo)
├── dto/RankingPageResponse.java                     (nuevo)
├── exception/SoloSuperusuarioException.java         (nuevo, 403)
├── exception/GlobalExceptionHandler.java            (modificado: un handler más)
├── model/VariacionCotizacion.java                   (nuevo, puro)
├── persistence/CotizacionRepository.java            (modificado: solo consultas nuevas)
├── service/CotizacionConsultaService.java           (nuevo)
├── service/CotizacionLeida.java                     (nuevo, record de resultado)
├── service/JugadorDetalleResultado.java             (nuevo, record)
├── service/HistorialCotizacionesResultado.java                  (nuevo, record)
├── service/RankingResultado.java                    (nuevo, record con items)
├── service/CotizacionHistoricaService.java          (nuevo, implementa el puerto)
└── adapter/CotizacionHistoricaFijaAdapter.java      (se borra)
resources/db/migration/V18__indice_cotizacion_estrategia_fecha.sql (nuevo)
```

Se usa un controller nuevo con `@RequestMapping("/api/jugadores")` para no
modificar `JugadorController` (feature 003), cuyo listado rechaza parámetros
desconocidos. Spring prioriza la ruta literal `/ranking` sobre
`/{jugadorId}`; igualmente un test E2E lo verifica.

### Modelo: variación

`VariacionCotizacion.porcentaje(BigDecimal anterior, BigDecimal actual)`
devuelve `null` si `anterior` es `null`; si no, `(actual / anterior - 1) * 100`
con `MathContext.DECIMAL64` y redondeo final a 2 decimales `HALF_UP`.
`anterior` siempre es positivo porque la `006` aplica piso de `0,10`; si
llegara un valor no positivo lanza `IllegalArgumentException`.

### Service de consulta

`CotizacionConsultaService` (`@Service`, `@Transactional(readOnly = true)`),
con `JugadorRepository`, `CotizacionRepository`, `EstrategiaRepository` y
`Clock`.

- `resolverEstrategia(String pedida, boolean superusuario)`: valida que el
  código sea `RENDIMIENTO_POSICION` o `CONSISTENCIA` (si no, `400`), obtiene la
  activa con `findByActivaTrue()` y, si la pedida no es la activa y quien
  consulta no es superusuario, lanza `SoloSuperusuarioException`. Sin pedida,
  devuelve la activa, que puede ser `null`.
- `detalle(jugadorId)`: busca el jugador (`404` si no existe) y la última
  cotización de la activa con fecha menor o igual a la fecha de negocio; luego
  la anterior para la variación.
- `historial(jugadorId, estrategia, desde, hasta, superusuario)`: valida el
  rango, busca el jugador, carga la serie completa del jugador y la estrategia
  con `findByJugadorIdAndEstrategiaOrderByFechaAsc` (menos de 52 filas),
  calcula cada variación contra el elemento anterior de la serie y recién
  después filtra por `desde` y `hasta`. Así la primera del rango tiene la
  variación correcta sin otra consulta.
- `vigente(jugadorId, estrategia, fecha, superusuario)`: usa
  `findTopByJugadorIdAndEstrategiaAndFechaLessThanEqualOrderByFechaDesc` con la
  fecha pedida o la de negocio; `404` si no hay; la anterior se busca con la
  misma consulta y `fecha - 1 día`.
- `ranking(estrategia, liga, posicion, page, size, superusuario)`: valida
  paginación y filtros, obtiene la última fecha de la estrategia, pide la página
  con una `Specification` y un `@EntityGraph` sobre `jugador`, ordenada por
  `score DESC, precio DESC, jugador.nombre ASC, jugador.id ASC`. Luego busca en
  una sola consulta las cotizaciones de la fecha anterior para los jugadores de
  la página y calcula variación y puesto.
- La fecha de negocio es `LocalDate.ofInstant(clock.instant(), ZONA_NEGOCIO)`.
- El score se informa con 4 decimales `HALF_UP` y el precio con 4 decimales.

La liga llega con su nombre legible (`La Liga`) y se convierte con un mapa
privado igual al de `JugadorService`; no se modifica `Liga` ni `JugadorService`.

### Implementación del puerto

`CotizacionHistoricaService implements CotizacionHistoricaPort`
(`@Service`, `@Transactional(readOnly = true)`), con `CotizacionRepository` y
`EstrategiaRepository`.

- `fechasCotizacion`: con nulos, rango invertido o sin activa devuelve
  `List.of()`; si no, `findFechasByEstrategiaBetween` (`select distinct`,
  ascendente).
- `preciosA`: con colección nula o vacía devuelve `Map.of()`; sin activa, `1.0000`
  para cada id; si no, una consulta `findVigentesA(estrategia, ids, fecha)` con
  subconsulta de `max(fecha)` por jugador. Los ids sin resultado se completan
  con `1.0000`. Devuelve un mapa inmutable con una entrada por id distinto.
- Al existir este bean se borra `CotizacionHistoricaFijaAdapter`; si quedaran
  los dos, Spring fallaría por dos beans del mismo tipo.

### Consultas nuevas en `CotizacionRepository`

Solo se agregan métodos; no se cambia ni borra ninguno existente.

```java
@Query("select max(c.fecha) from Cotizacion c where c.estrategia = :estrategia")
Optional<LocalDate> findMaxFechaByEstrategia(String estrategia);

@Query("select max(c.fecha) from Cotizacion c where c.estrategia = :estrategia and c.fecha < :fecha")
Optional<LocalDate> findFechaAnterior(String estrategia, LocalDate fecha);

@Query("select max(c.version) from Cotizacion c where c.estrategia = :estrategia and c.fecha = :fecha")
Optional<Integer> findVersionByEstrategiaAndFecha(String estrategia, LocalDate fecha);

List<Cotizacion> findByEstrategiaAndFechaAndJugadorIdIn(String estrategia, LocalDate fecha, Collection<Long> jugadorIds);

@Query("select distinct c.fecha from Cotizacion c where c.estrategia = :estrategia and c.fecha between :desde and :hasta order by c.fecha")
List<LocalDate> findFechasByEstrategiaBetween(String estrategia, LocalDate desde, LocalDate hasta);

@Query("""
    select c from Cotizacion c
    where c.estrategia = :estrategia and c.jugador.id in :ids
      and c.fecha = (select max(c2.fecha) from Cotizacion c2
                     where c2.jugador.id = c.jugador.id and c2.estrategia = :estrategia and c2.fecha <= :fecha)
    """)
List<Cotizacion> findVigentesA(String estrategia, Collection<Long> ids, LocalDate fecha);

@EntityGraph(attributePaths = "jugador")
Page<Cotizacion> findAll(Specification<Cotizacion> spec, Pageable pageable);
```

`CotizacionRepository` pasa a extender también `JpaSpecificationExecutor<Cotizacion>`.
Los parámetros usan `@Param` como el resto del repositorio.

### Migración

```sql
-- V18__indice_cotizacion_estrategia_fecha.sql
CREATE INDEX IF NOT EXISTS ix_cotizacion_estrategia_fecha ON cotizacion (estrategia, fecha);
```

### Controller y seguridad

`CotizacionJugadorController` con `@Validated`. Cada método recibe
`Authentication` y calcula `superusuario` con
`authentication.getAuthorities()` contiene `ROLE_SUPERUSUARIO`. `jugadorId`
lleva `@Positive`; `page` y `size` se validan en el service como en
`JugadorService`. Las fechas se reciben como `LocalDate` (`AAAA-MM-DD`); un
formato inválido da `400` por `MethodArgumentTypeMismatchException`.

`SoloSuperusuarioException` se mapea a `403` con el mensaje "Solo el
superusuario puede consultar una estrategia que no es la activa". El resto de
los errores usa handlers existentes: `IllegalArgumentException` → `400`,
`RecursoNoEncontradoException` → `404`.

## File Change Map

### Files to add

- Los archivos nuevos del package layout.
- `V18__indice_cotizacion_estrategia_fecha.sql`.
- Tests: `unit/model/VariacionCotizacionTest`,
  `unit/service/CotizacionConsultaServiceTest`,
  `unit/service/CotizacionHistoricaServiceTest`,
  `integration/CotizacionConsultaIntegrationTest`,
  `e2e/CotizacionJugadorE2ETest`.

### Files to modify

- `persistence/CotizacionRepository.java`: solo métodos nuevos y
  `JpaSpecificationExecutor`.
- `exception/GlobalExceptionHandler.java`: un handler nuevo.
- `postman/formula11-registro-catalogo.json`: carpeta con los cuatro requests.

### Files to delete

- `adapter/CotizacionHistoricaFijaAdapter.java` (FR-009a de la `009`).
- `unit/adapter/CotizacionHistoricaFijaAdapterTest.java`, solo con el "sí"
  explícito del equipo.

### Test existente adaptado (con el "sí" de A)

- `e2e/PortfolioEvolucionE2ETest.java`: `@MockBean CotizacionHistoricaPort`
  con el comportamiento del adapter provisorio, porque su caso feliz esperaba
  el lunes y el precio 1. No cambian sus aserciones.

### Files not to modify

- `service/CotizacionHistoricaPort.java`, `service/CotizacionVigentePort.java`,
  `service/CotizacionVigenteService.java`, `service/PortfolioService.java`.
- `model/Cotizacion.java`, `model/Jugador.java`, `model/Liga.java`.
- `controller/JugadorController.java`, `service/JugadorService.java`.
- Migraciones V1–V3, V10–V17 y V20–V25; DTOs y tests de otras features.

## Implementation Phases

### Phase 1 - Base

1. Migración V18 y consultas nuevas del repositorio.
2. `VariacionCotizacion` con sus tests.
3. `SoloSuperusuarioException` y su handler.

### Phase 2 - Puerto para la 009 (US5)

1. `CotizacionHistoricaService` con tests unitarios e integración.
2. Borrar el adapter provisorio (y su test, con el "sí").

### Phase 3 - Lecturas por jugador (US1, US2, US3)

1. Records de resultado y `CotizacionConsultaService` para detalle, historial y vigente.
2. DTOs y controller.
3. Tests unitarios y E2E.

### Phase 4 - Ranking (US4)

1. Consulta paginada, variación y puesto.
2. Endpoint y tests.

### Phase 5 - Cierre

1. Postman y Swagger.
2. `mvn clean test -Dtest="**/unit/**/*Test"` local y suite completa en el CI.

## Testing Strategy

### Unit tests (sin Spring ni base)

- `VariacionCotizacionTest`: primera (`null`), suba, baja, sin cambio, redondeo
  `HALF_UP` en el límite y anterior no positivo.
- `CotizacionConsultaServiceTest` con mocks y `Clock.fixed`: estrategia por
  defecto, código inválido (`400`), `USUARIO` con otra estrategia (`403`),
  `USUARIO` con la activa explícita, superusuario con otra, jugador inexistente
  (`404`), detalle sin cotizaciones (`null`), historial con rango y variación
  de la primera del rango, `desde > hasta`, vigente sin fecha usa la fecha de
  negocio, vigente sin datos (`404`), ranking vacío, puestos de la segunda
  página, liga y posición inválidas, paginación inválida.
- `CotizacionHistoricaServiceTest` con mocks: nulos, rango invertido, sin
  activa, ids sin cotización con `1.0000`, ids repetidos.

### Integration tests (Testcontainers)

- `CotizacionConsultaIntegrationTest`: siembra jugadores propios y
  cotizaciones de dos estrategias y tres domingos; verifica la página del
  ranking con filtros y desempate, `findVigentesA`, `findFechasByEstrategiaBetween`
  y el puerto real. Limpia solo lo que crea, por ids.

### E2E (MockMvc, paquete `e2e`)

- `CotizacionJugadorE2ETest`: `401` sin JWT; `200` de los cuatro endpoints con
  `USUARIO`; `403` con otra estrategia como `USUARIO` y `200` como
  `SUPERUSUARIO`; `404` de jugador inexistente; `400` por id negativo, fecha
  inválida y `size=0`; `/api/jugadores/ranking` no cae en `/{jugadorId}`.

### Validation commands

```powershell
cd formula11\backend
mvn clean test -Dtest="**/unit/**/*Test"
```

La suite completa con Testcontainers la corre el CI del PR.

## Risks and Mitigations

| Riesgo | Mitigación |
|---|---|
| Dos beans de `CotizacionHistoricaPort` si no se borra el adapter provisorio | Se borra en la misma fase; el contexto de los tests E2E lo detecta. |
| Conflictos con la `009` en `dev` | No se tocan archivos de la `009`; solo se borra el adapter acordado. |
| Fechas domingo frente a los lunes del adapter provisorio | La `009` no depende del día: usa las fechas que devuelve el puerto. Se avisa a B. |
| `IN` con muchos ids en `preciosA` | El portfolio de un usuario tiene pocos jugadores. |
| Orden inestable del ranking | Desempate total hasta `jugador.id`. |
