# Implementation Plan: Detector de jugadores subvaluados

**Branch**: `010-senales-subvaluados` | **Date**: 2026-10-30 | **Spec**: [spec.md](spec.md)

## Summary

Al terminar cada semana del recálculo, un service nuevo (`SenalService`) lee las
cotizaciones de las dos estrategias y los jugadores que jugaron, aplica un detector
puro del modelo y guarda las señales en una tabla nueva (`senal`) con upsert por
jugador, semana y tipo. Dos endpoints de lectura exponen las señales y el panel de
aciertos, que también se calcula con una clase pura del modelo. `RecalculoService`
llama a la generación mediante un puerto, sin que un error de señales afecte el
recálculo.

## Technical Context

- **Language/Version**: Java 21, Spring Boot 3.5.5, Spring Data JPA, Flyway, springdoc.
- **Storage**: PostgreSQL 16. Tabla nueva `senal` (V19). Se leen `cotizacion`,
  `metrica_partido`, `estrategia` y `jugadores`.
- **Testing**: JUnit 5 y Mockito; Testcontainers; MockMvc en `e2e`.
- **Volumen**: hasta 40 señales por semana; unas 500 por temporada. La generación
  lee unas 5.300 cotizaciones y unas 1.500 métricas por semana.
- **Dependencias de otras ramas**: se crea sobre `dev` con `fix-006-versiones` y la
  `008` mergeadas. Reutiliza `VariacionCotizacion` de la `008`.
- **Migraciones**: `V19`, la última del rango de A.

## Constitution Check

| Principio | Cumplimiento |
|---|---|
| 1. Capas | Controller → `SenalService` → repositories. `RecalculoService` depende del puerto `GeneradorSenalesPort`, no de la implementación. El scheduler no cambia. |
| 2. Modelo rico | `DetectorSenales` y `MedidorAciertos` son clases puras del modelo; `Senal` valida sus invariantes. |
| 3. Validaciones | Controller: tipos; Service: `page` y `size`; Model: precios positivos y brecha no negativa. |
| 4. Seguridad | JWT obligatorio; cualquier rol. |
| 5. Observabilidad | Log estructurado por semana generada (cantidades por tipo) y warning con `correlationId` si falla. |
| 6. Tests | Unitarios del modelo y services, integración y E2E. No se tocan tests existentes. |
| 7. DoD | Swagger y Postman actualizados. |
| 9. Clock | `SenalService` usa `Clock` para `creadaEn`. |
| 10. Tokens | La compra en un click reutiliza `POST /api/ordenes/compra` y `MercadoService`. |

## Architecture and Design

### Package layout

```text
com.formula11
├── config/SenalesProperties.java                 (nuevo, app.senales)
├── controller/SenalController.java               (nuevo)
├── dto/SenalResponse.java, SenalesPageResponse.java, AciertosResponse.java (nuevos)
├── model/Senal.java                              (nueva entidad)
├── model/senales/DetectorSenales.java            (nuevo, puro)
├── model/senales/MedidorAciertos.java            (nuevo, puro)
├── persistence/SenalRepository.java              (nuevo)
├── persistence/CotizacionRepository.java         (modificado: 2 consultas)
├── persistence/MetricaPartidoRepository.java     (modificado: 1 consulta)
├── service/GeneradorSenalesPort.java             (nuevo)
├── service/SenalService.java                     (nuevo, implementa el puerto)
├── service/SenalLeida.java, PaginaSenales.java   (nuevos, records)
└── service/RecalculoService.java                 (modificado: llama al puerto)
resources/db/migration/V19__crear_senales.sql     (nuevo)
```

### Detector (modelo puro)

`DetectorSenales.detectar(List<Candidato>, Parametros)`:

- `Candidato(jugadorId, posicion, precio, score, precioOtraEstrategia)`, solo
  jugadores que jugaron.
- `ENTRE_ESTRATEGIAS`: si `precioOtraEstrategia > precio` y la brecha llega al umbral.
- `PRECIO_ATRASADO`: pares = misma posición, otro jugador,
  `|score - score del jugador| <= ventanaScore`; con al menos `minimoPares`, la
  mediana de sus precios (4 decimales `HALF_UP`) es el precio estimado.
- `brecha = VariacionCotizacion.porcentaje(precio, precioEstimado)`.
- Por tipo: orden por brecha descendente y jugador ascendente; se cortan los
  primeros `maximoPorTipo`.

### Medición de aciertos (modelo puro)

`MedidorAciertos.medir(List<SemanaAMedir>)` con
`SemanaAMedir(fecha, jugadoresSenalados, preciosSemana, preciosSiguiente)`:

- Variación de un jugador: `(siguiente / semana - 1) * 100` con `DECIMAL64`.
- Mercado: promedio de todos los jugadores con precio en las dos semanas.
- Cada señal con precios en las dos semanas se evalúa; acierta si su variación
  supera la del mercado.
- Redondeo a 2 decimales `HALF_UP` solo en la salida.
- Sin señales evaluadas: `tasaAcierto` y `diferenciaPromedio` en `0.00`.

### Service

`SenalService implements GeneradorSenalesPort`:

- `@Transactional generar(domingo)`: estrategia activa (si no hay, no hace nada);
  cotizaciones de la activa en esa fecha con `@EntityGraph` sobre `jugador`;
  precios de la otra estrategia; jugadores que jugaron entre el lunes y el
  domingo; detector; upsert contra `findByFecha(domingo)`: actualiza las que
  siguen, crea las nuevas y borra las que ya no aplican. Sin cotizaciones de la
  activa, borra las señales de esa fecha.
- `@Transactional(readOnly = true) listar(vigentes, page, size)`: valida la
  paginación; con `vigentes`, filtra por `max(fecha)`; orden
  `fecha DESC, brecha DESC, id ASC`; mapea a `SenalLeida` dentro de la
  transacción.
- `@Transactional(readOnly = true) aciertos()`: agrupa todas las señales por
  fecha (con su estrategia), lee precios de esa fecha y de la siguiente con una
  consulta de proyección y delega en `MedidorAciertos`.

### Integración con el recálculo

`RecalculoService` agrega un constructor `@Autowired` con
`ObjectProvider<GeneradorSenalesPort>` y conserva el constructor actual (sin
generador) para que `RecalculoServiceTest` no cambie. Después de cada
`semanas.procesar(domingo)` llama a cada generador dentro de un `try/catch` que
registra el error y sigue.

### Consultas nuevas

```java
// CotizacionRepository
@EntityGraph(attributePaths = "jugador")
List<Cotizacion> findByEstrategiaAndFecha(String estrategia, LocalDate fecha);

@Query("select c.jugador.id, c.precio from Cotizacion c where c.estrategia = :estrategia and c.fecha = :fecha")
List<Object[]> findPreciosByEstrategiaAndFecha(String estrategia, LocalDate fecha);

// MetricaPartidoRepository
@Query("select distinct m.jugador.id from MetricaPartido m where m.fechaPartido between :desde and :hasta and m.minutos > 0")
List<Long> findJugadoresQueJugaron(LocalDate desde, LocalDate hasta);

// SenalRepository
List<Senal> findByFecha(LocalDate fecha);
@Query("select max(s.fecha) from Senal s") Optional<LocalDate> findMaxFecha();
@EntityGraph(attributePaths = "jugador") Page<Senal> findByFecha(LocalDate fecha, Pageable pageable);
@EntityGraph(attributePaths = "jugador") Page<Senal> findAllBy(Pageable pageable);
```

### Migración V19

```sql
CREATE TABLE senal (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    jugador_id BIGINT NOT NULL REFERENCES jugadores(id),
    fecha DATE NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    estrategia VARCHAR(80) NOT NULL,
    brecha NUMERIC(9, 2) NOT NULL,
    precio NUMERIC(19, 4) NOT NULL,
    precio_estimado NUMERIC(19, 4) NOT NULL,
    creada_en TIMESTAMPTZ NOT NULL,
    CONSTRAINT ux_senal_jugador_fecha_tipo UNIQUE (jugador_id, fecha, tipo),
    CONSTRAINT ck_senal_tipo CHECK (tipo IN ('ENTRE_ESTRATEGIAS', 'PRECIO_ATRASADO'))
);
CREATE INDEX ix_senal_fecha_brecha ON senal (fecha DESC, brecha DESC);
```

## File Change Map

- **Nuevos**: los del package layout, `V19__crear_senales.sql` y los tests
  `unit/model/DetectorSenalesTest`, `unit/model/MedidorAciertosTest`,
  `unit/service/SenalServiceTest`, `unit/service/RecalculoServiceSenalesTest`,
  `integration/SenalIntegrationTest`, `e2e/SenalE2ETest`.
- **Modificados**: `RecalculoService` (constructor y llamada al puerto),
  `CotizacionRepository` y `MetricaPartidoRepository` (consultas nuevas),
  `application.yml` (`app.senales`), Postman.
- **Sin cambios**: tests existentes, `CotizacionScheduler`, contrato, migraciones previas.

## Testing Strategy

- **Unitarios**: detector (cada tipo, umbral exacto, mínimo de pares, ventana,
  mediana par e impar, jugador sin otra estrategia, máximo por tipo), medidor
  (aciertos, mercado, señales sin precio, sin datos), `SenalService` con mocks
  (upsert, sin activa, sin cotizaciones, listados, validación, aciertos) y
  `RecalculoService` con el puerto (llama por semana y no falla si el puerto falla).
- **Integración**: siembra jugadores, métricas y cotizaciones de dos semanas;
  genera, regenera (mismos ids), lista y mide aciertos contra PostgreSQL.
- **E2E**: `401`, `200` vigentes y todas, `400` por `size`, aciertos vacíos y con datos.

## Risks and Mitigations

| Riesgo | Mitigación |
|---|---|
| Demasiadas alertas | Máximo por tipo y solo jugadores que jugaron. |
| Un error en señales rompe el recálculo | `try/catch` en `RecalculoService` y test que lo verifica. |
| Ids cambiantes que repiten alertas | Upsert por jugador, semana y tipo. |
| Conflictos con `fix-006-versiones` y la `008` | Se aplica después de mergear las dos. |
