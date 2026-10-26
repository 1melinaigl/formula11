# Tasks: Portfolio e historial de operaciones

**Input**: [spec.md](./spec.md), [plan.md](./plan.md), [data-model.md](./data-model.md)  
**Branch**: `009-portfolio-historial`

## Conventions

- `[P]` indica que la tarea puede ejecutarse en paralelo con otras de la misma fase.
- No modificar ni borrar tests previos sin autorización.
- Importes a escala 4 y porcentajes a escala 2 con `HALF_UP`.
- Fechas de transacciones convertidas a calendario usando UTC.
- `fechaValuacion` se calcula como `LocalDate.now(clock)` en UTC.
- No modificar `openapi.yaml` ni agregar esquemas al contrato.

## Phase 1 - Setup

- [x] T001 [P] Revisar `specs/005-contratos-entrega2/contracts/openapi.yaml` (fuente contractual) y Postman para confirmar rutas, parámetros, respuestas y ejemplos de los tres endpoints.
- [x] T002 [P] Definir records/clases de resultados de dominio inmutables fuera de `dto`: portfolio, posición, evolución, punto y página de transacciones.
- [x] T003 [P] Agregar `CotizacionHistoricaPort` en `com.formula11.service` con la firma exacta de la especificación.

## Phase 2 - Foundational

- [x] T004 Implementar `CotizacionHistoricaFijaAdapter` en `adapter`, activo en todos los perfiles, con lunes semanales inclusivos, precio `1.0000` y tests unitarios.
- [x] T005 Agregar a `TenenciaRepository` la lectura de tenencias del usuario ordenada por `jugador.id`, y a `TransaccionRepository` las consultas paginadas filtrables, con todos los filtros aplicados en la base de datos, el orden `fecha DESC, id DESC`, la lista cronológica y la primera fecha.
- [x] T006 Agregar a `AuditoriaMercadoRepository` una consulta de solo lectura por ids de operación (`findByOperacionIdIn(Collection<Long>)`) que recupere las auditorías de una página en una sola consulta; mantenerlo append-only, sin `update` ni `delete`, y agregar `AuditoriaMercado.getOperacion()` junto con los accesores necesarios.
- [x] T007 Implementar el parser tipado de `despues.saldoUsuario`, el factory `OperacionResponse.from(Transaccion, BigDecimal saldoPosterior)` y el record de dominio `OperacionHistorial(Transaccion, saldoPosterior)`; mantener `from(ResultadoOperacion)` y tratar auditoría ausente como error interno (`IllegalStateException`, 500). El saldo se resuelve en el service, no en el paquete `dto`.
- [x] T008 Crear `AccesoPortfolioDenegadoException`, mapearla a `403` en `GlobalExceptionHandler` y agregar tests unitarios del handler.
- [x] T009 Verificar con `EXPLAIN` las consultas contra PostgreSQL; no se pudo ejecutar al no estar disponible PostgreSQL y no se creó V26.

## Phase 3 - User Story 1: Consultar portfolio actual

- [x] T010 Implementar `PortfolioService.portfolio` con autorización, cuenta, tenencias positivas, `CotizacionVigentePort`, `Clock`, cálculos, redondeo y resultados de dominio.
- [x] T011 Crear `PortfolioResponse` y `PosicionPortfolioResponse` y mapear el resultado usando `JugadorResponse`.
- [x] T012 Crear `PortfolioController` para `GET /api/usuarios/{usuarioId}/portfolio`; resolver el id mediante `usuarioService.obtenerIdPorEmail` y mantenerlo sin lógica de negocio.
- [x] T013 [P] Agregar tests unitarios de portfolio con cantidades cero, ganancias negativas, invertido cero, cotizaciones faltantes, redondeo y `Clock.fixed`.
- [x] T014 Agregar test E2E MockMvc de portfolio para `200`, `401`, `403` y `404`.

## Phase 4 - User Story 2: Consultar evolución

- [x] T015 Implementar `PortfolioService.evolucion` con defaults, validación de intervalo, transacciones UTC, reconstrucción de tenencias y precios por fecha mediante `CotizacionHistoricaPort`.
- [x] T016 Crear `EvolucionPortfolioResponse` y el DTO de punto, y agregar `/portfolio/evolucion` al controller.
- [x] T017 [P] Agregar tests unitarios de evolución con compras, ventas, promedio ponderado, serie vacía, defaults, `desde > hasta`, redondeo y `Clock.fixed`.
- [x] T018 Agregar test de integración con Testcontainers para transacciones, cotizaciones históricas y reconstrucción UTC.
- [x] T019 Agregar test E2E MockMvc de evolución para `200`, `400`, `401` y `403`, incluyendo intervalo sin cotizaciones.

## Phase 5 - User Story 3: Consultar historial

- [x] T020 Implementar `PortfolioService.historial` con filtros opcionales AND, paginación, orden `fecha DESC, id DESC` (pasado en el `Pageable`) y resultado de dominio: con una única consulta `findByOperacionIdIn` sobre los ids de la página, arma cada `OperacionHistorial` con el saldo de `AuditoriaSaldoParser.parsear(auditoria.getDespues())`.
- [x] T021 Mapear `HistorialTransaccionesResultado` a `TransaccionesPageResponse` con `OperacionResponse.from(transaccion, saldoPosterior)`; el controller no consulta repositorios.
- [x] T022 Completar `GET /api/usuarios/{usuarioId}/transacciones` con validación de `page`, `size`, tipo y `@Positive jugadorId` (incluyendo `400` para valores no numéricos), devolviendo `200` vacío para jugador positivo inexistente.
- [x] T023 [P] Agregar tests unitarios de filtros combinados, página vacía, orden por fecha/id, saldo posterior desde JSONB y auditoría faltante.
- [x] T024 Agregar test de integración con Testcontainers que verifique la consulta por ids de auditoría y no exponga operaciones de otro usuario.
- [x] T025 Agregar test E2E MockMvc de historial para `200`, filtros, `400`, `401`, `403` y jugador inexistente con página vacía.

## Phase 6 - Demo local

- [ ] T026 Agregar `mercado.demo.usuarios` con los cuatro emails por defecto y `mercado.demo.fecha-inicio` a `application-local.yml`, junto con su configuración tipada sin activar la demo en otros perfiles.
- [ ] T027 Agregar `MercadoService.comprar(usuarioId, jugadorId, cantidad, Clock)` delegando al mismo núcleo transaccional y validaciones.
- [ ] T028 Implementar `DemoPortfolioInitializer` con `@Profile("local")` y `@Order` posterior a `SuperusuarioInitializer`, creando los cuatro usuarios configurados por email y comprando cinco jugadores con un `Clock.fixed` configurable; decidir la idempotencia por la existencia de esos emails.
- [ ] T029 [P] Agregar tests de idempotencia: primer arranque crea exactamente los cuatro usuarios configurados y sus operaciones; segundo arranque no duplica datos al encontrar esos emails y otros perfiles no ejecutan la carga.

## Phase 7 - Integration and Polish

- [ ] T030 [P] Verificar Swagger/OpenAPI y ejemplos de los tres endpoints contra el contrato existente, sin modificar `openapi.yaml`, códigos HTTP ni esquema `Error`.
- [ ] T031 Ejecutar la suite focalizada de unit, integración y E2E; corregir regresiones sin eliminar tests existentes.
- [ ] T032 Verificar compilación, perfil `local`, arranques repetidos e idempotencia de demo; ejecutar `git diff --check`.

## Dependencies

- T002 y T003 habilitan T004-T008.
- T005 y T006 habilitan T010, T015 y T020.
- T007 habilita T021.
- T008 habilita T012, T016 y T022.
- T010-T012 habilitan T013-T014.
- T015-T016 habilitan T017-T019.
- T020-T022 habilitan T023-T025.
- T026-T028 habilitan T029.
- T010-T025 y T029 habilitan T030-T032.
