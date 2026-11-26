# Implementation Plan: Actuator y Prometheus

**Branch**: `entrega3-observabilidad` | **Date**: 2026-11-26 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/016-actuator-prometheus/spec.md`

## Summary

Se amplía lo que expone Actuator: `health`, `info`, `metrics`, `prometheus` y `loggers`.
`health` queda público, solo con el estado. El resto queda para el superusuario, en un
bloque aparte de `SecurityConfig`. Un `HealthIndicator` propio (`cargaMetricas`) da `DOWN`
si la última carga de métricas de WhoScored terminó `FALLIDA`. Se suma
`micrometer-registry-prometheus` y un componente `MetricasNegocio` con dos métricas: el
contador `formula11.mercado.operaciones` por tipo (`compra`, `venta`, `p2p`) y el timer
`formula11.cotizaciones.recalculo` por resultado. Un aspecto (`MetricasNegocioAspect`)
las alimenta desde `MercadoService` y `RecalculoService` sin tocar sus constructores ni
sus tests. Opcional: un docker-compose con un Prometheus local.

## Technical Context

- **Language/Version**: Java 21, Spring Boot 3.5.5.
- **Primary Dependencies**: nueva `micrometer-registry-prometheus` (versión del parent).
  Ya están `spring-boot-starter-actuator` y `spring-boot-starter-aop` (015).
- **Storage**: sin cambios de esquema. Una consulta derivada nueva en
  `EjecucionMetricasRepository`.
- **Testing**: JUnit 5, Mockito, `SimpleMeterRegistry`; Testcontainers (perfil `test`);
  MockMvc en `e2e` con `@AutoConfigureObservability` para Prometheus.
- **Target Platform**: el mismo backend. Docker solo para el Prometheus local opcional.
- **Performance Goals**: un incremento de contador por operación y una muestra de timer por
  recálculo; el health hace una consulta ordenada por fecha de fin sobre una tabla chica.
- **Constraints**: no cambiar respuestas de la API (SC-005). Las métricas nunca rompen una
  operación (FR-009). No se modifican tests existentes.
- **Scale/Scope**: 5 endpoints de Actuator, 2 métricas de negocio y 1 health indicator.
- **Dependencias de otras ramas**: A incrementa el tipo `p2p` desde su feature (ver
  [contracts/metricas-negocio.md](contracts/metricas-negocio.md)). Archivos compartidos:
  `pom.xml` (solo la dependencia), `application.yml` (lo cambia B) y `SecurityConfig`
  (bloque aparte con comentario).
- **Migraciones**: ninguna.

## Constitution Check

| Principio | Cumplimiento |
|---|---|
| 1. Capas | El health indicator pide los datos a `CargaMetricasService`, sin leer el repositorio directamente. `MetricasNegocio` y el aspecto son transversales (`observabilidad`): ningún service depende de ellos y no llaman a repositorios. |
| 2. Modelo rico | No toca el modelo. El tipo de operación se lee de la `Transaccion` ya confirmada. |
| 3. Validaciones | Sin cambios. |
| 4. Seguridad | Todo Actuator, salvo `health`, pide superusuario. `health` público muestra solo el estado. No se exponen `env`, `beans` ni `heapdump`. |
| 5. Observabilidad | Es el objetivo: health checks y métricas clave, incluida la latencia del recálculo. |
| 6. Tests | Unit, integración y E2E, felices y borde. No se modifica ni borra ningún test existente (R5). |
| 7. DoD | No hay endpoints nuevos de la API: Swagger no cambia. Se suma a Postman una carpeta "Actuator" con health, info, prometheus y loggers. |
| 8. Idioma | Clases y textos en español (`MetricasNegocio`, `TipoOperacionMercado`); términos técnicos en inglés (`HealthIndicator`, `Timer`). |
| 9. Clock | No aplica: el timer usa el reloj de Micrometer para medir intervalos, y el health solo lee fechas guardadas. |
| 10. Tokens | No cambia comprar ni vender: el aspecto observa el resultado de los mismos métodos reutilizables. |

**Resultado**: pasa, sin violaciones. Se vuelve a chequear después del diseño: igual.

## Project Structure

### Documentation (this feature)

```text
specs/016-actuator-prometheus/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── actuator.md
│   └── metricas-negocio.md
├── checklists/requirements.md
└── tasks.md              (lo crea /speckit-tasks)
```

### Source Code

```text
formula11/backend/
├── pom.xml                                              (modificado: micrometer-registry-prometheus)
├── docker/prometheus/                                   (nuevo, opcional)
│   ├── docker-compose.yml
│   ├── prometheus.yml
│   └── token.example
├── src/main/java/com/formula11/
│   ├── observabilidad/
│   │   ├── TipoOperacionMercado.java                    (nuevo, enum)
│   │   ├── MetricasNegocio.java                         (nuevo, @Component)
│   │   ├── MetricasNegocioAspect.java                   (nuevo, @Aspect)
│   │   └── CargaMetricasHealthIndicator.java            (nuevo, HealthIndicator)
│   ├── security/SecurityConfig.java                     (modificado: bloque /actuator/** y 403 JSON)
│   ├── service/metricas/CargaMetricasService.java       (modificado: ultimaTerminada())
│   └── persistence/metricas/EjecucionMetricasRepository.java (modificado: consulta derivada)
├── src/main/resources/application.yml                   (modificado: management.* e info.app.*)
└── src/test/java/com/formula11/
    ├── unit/observabilidad/
    │   ├── MetricasNegocioTest.java                     (nuevo)
    │   ├── MetricasNegocioAspectTest.java               (nuevo)
    │   └── CargaMetricasHealthIndicatorTest.java        (nuevo)
    ├── unit/service/metricas/
    │   └── CargaMetricasUltimaTerminadaTest.java        (nuevo, no toca CargaMetricasServiceTest)
    ├── integration/observabilidad/
    │   ├── MetricasNegocioIntegrationTest.java          (nuevo)
    │   └── CargaMetricasHealthIntegrationTest.java      (nuevo)
    └── e2e/observabilidad/
        ├── ActuatorHealthE2ETest.java                   (nuevo)
        ├── ActuatorSeguridadE2ETest.java                (nuevo)
        └── ActuatorPrometheusE2ETest.java               (nuevo)
```

Además: `.gitignore` de la raíz (`formula11/backend/docker/prometheus/token`), la colección
de Postman (carpeta "Actuator") y el `README.md` (sección "Observabilidad").

**Structure Decision**: todo lo nuevo va en el paquete transversal `observabilidad` que creó
la 015. Lo único que se toca fuera de él es un método de consulta en el service y el
repositorio de métricas, el bloque de `SecurityConfig` y la configuración.

## Architecture and Design

### Health

```text
GET /actuator/health (público)
  → HealthEndpoint agrega: db, diskSpace, ping, ..., cargaMetricas
      cargaMetricas → CargaMetricasHealthIndicator.health()
                        → CargaMetricasService.ultimaTerminada()
                            → EjecucionMetricasRepository.findFirstByEstadoInAndFinNotNullOrderByFinDescIdDesc([COMPLETADA, PARCIAL, FALLIDA])
                        FALLIDA → DOWN ; otro / vacío → UP  (+ detalles)
  → sin superusuario: {"status":"UP|DOWN"} ; superusuario: + components con details
```

### Métricas de negocio

```text
POST /api/mercado/... → MercadoController → [proxy]
   MetricasNegocioAspect (HIGHEST_PRECEDENCE + 10, afuera)
     → TransactionInterceptor → MercadoService.comprar/vender → commit
   @AfterReturning(ResultadoOperacion r) → MetricasNegocio.registrarOperacionMercado(COMPRA|VENTA)
                                              → counter formula11.mercado.operaciones{tipo}

RecalculoService.recalcular(..)  (manual o CotizacionScheduler)
   MetricasNegocioAspect @Around → Timer.Sample → proceed → stop(timer{resultado=ok|error})

Feature P2P de A → MetricasNegocio.registrarOperacionMercado(P2P)   (después del commit si hay transacción)
```

### Decisiones clave (detalle en [research.md](research.md))

- Exposición `health,info,metrics,prometheus,loggers`; detalle del health solo para `SUPERUSUARIO` (R1).
- `EndpointRequest` en `SecurityConfig`, health público y el resto `hasRole("SUPERUSUARIO")`; 403 JSON (R2).
- Versión desde `@project.version@` en `application.yml` (R3).
- `cargaMetricas` en `DOWN` solo por `FALLIDA` (R4).
- Aspecto en lugar de inyectar `MeterRegistry` en los services, para no tocar sus tests (R5).
- Incremento después del commit (R6); los tres tipos en cero desde el arranque (R7).

### Configuración

`application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,loggers
  endpoint:
    health:
      show-details: when_authorized
      roles: SUPERUSUARIO
  info:
    env:
      enabled: true
info:
  app:
    nombre: Formula11
    version: '@project.version@'
```

## Riesgos

| Riesgo | Mitigación |
|---|---|
| Un test deja una carga `FALLIDA` y otros ven el health en `DOWN` | Los tests que la crean la borran en `@AfterEach`; ningún test existente consulta el health. |
| El aspecto corre dentro de la transacción y cuenta una operación que después no se confirma | `@Order(HIGHEST_PRECEDENCE + 10)` y el `afterCommit` de `MetricasNegocio` (R6). |
| `@project.version@` sin filtrar si se corre desde el IDE sin Maven | Se ve el texto literal en `info`, sin romper nada. Con `mvn` siempre se filtra. |
| `ArquitecturaTest` de C | Solo controla las capas; `observabilidad` no está en ninguna y el health indicator usa un service, no un repositorio. |
| A agrega `P2P` a `Transaccion.Tipo` y lo confirma con otro método | El mapeo por nombre a `TipoOperacionMercado` lo cubre. Si usa otro service, llama a `MetricasNegocio` directo (contrato). |

## Release notes (para el PR)

- Actuator y Prometheus: `/actuator/health` público, que da `DOWN` si falló la última carga
  de métricas de WhoScored. `info`, `metrics`, `prometheus` y `loggers` quedan solo para el
  superusuario. Hay métricas nuevas `formula11_mercado_operaciones_total{tipo}` y
  `formula11_cotizaciones_recalculo_seconds`. Para probarlo: hacer una compra y consultar
  `GET /actuator/prometheus` con el token del superusuario.

## Complexity Tracking

Sin violaciones de la constitución.
