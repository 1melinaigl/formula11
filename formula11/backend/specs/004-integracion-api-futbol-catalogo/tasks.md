---

description: "Task list for Football-Data.org catalog synchronization"
---

# Tasks: Integración con API externa de fútbol para poblar el catálogo

**Input**: Design documents from `/specs/004-integracion-api-futbol-catalogo/`

**Prerequisites**: `plan.md`, `spec.md`; el contrato OpenAPI y el quickstart se generan/actualizan durante la implementación.

**Scope**: Backend únicamente. No modificar frontend, paginación existente, scheduler, scraping, cotizaciones ni mercado de tokens.

**Tests**: Obligatorios con JUnit 5, Mockito, MockRestServiceServer/mocks HTTP, `@DataJpaTest`, Testcontainers PostgreSQL y MockMvc. Ningún test puede llamar a Football-Data.org/FutbolOK real ni requerir una apikey real.

## Phase 1: Research and setup

**Purpose**: Confirmar el contrato real del proveedor y preparar dependencias/configuración sin secretos.

- [ ] T001 Documentar en `research.md` los endpoints Football-Data.org v4 para competencias, equipos y jugadores, competition codes de las cinco ligas, header requerido, límites del plan y comportamiento de `Retry-After`; registrar cualquier diferencia de FutbolOK.
- [ ] T002 [P] Añadir Spring Cache/Caffeine y las propiedades de versión necesarias en `backend/pom.xml`, sin incorporar dependencias de scraping ni clientes que oculten el límite de requests.
- [ ] T003 [P] Crear `FootballApiProperties` bajo `backend/src/main/java/com/formula11/adapter/` con URL base, header, apikey opcional, timeouts, TTL, capacidad, rate limit y mapeo de ligas.
- [ ] T004 [P] Configurar `application.yml` y `application-local.yml` para leer `FOOTBALL_API_KEY` y demás variables sin valores reales, logs de secretos ni validación que bloquee el arranque.
- [ ] T005 Verificar que la aplicación inicia sin `FOOTBALL_API_KEY` y que `GET /api/jugadores` continúa disponible usando solo la base local.

**Checkpoint**: El proyecto compila, arranca sin apikey y la configuración externa no expone secretos.

## Phase 2: Foundational persistence and adapter contracts

**Purpose**: Crear las identidades y puertos que permiten sincronizar sin duplicados ni acoplamiento HTTP.

- [ ] T006 Crear la migración Flyway `V3__identidad_externa_jugadores.sql` con `external_provider`, `external_id` y el origen/estado legacy necesario, incluyendo índice único compuesto para identidades externas.
- [ ] T007 [P] Definir `ExternalTeam`, `ExternalPlayer`, `LigaSyncResult` y excepciones internas de integración en el paquete `adapter`, sin DTOs propietarios del proveedor fuera de esa capa.
- [ ] T008 [P] Definir `FootballDataPort` con operaciones normalizadas para equipos y jugadores por liga/equipo, documentando que el service no conoce URLs, headers ni códigos HTTP.
- [ ] T009 Actualizar `Jugador` y `JugadorRepository` para persistir y buscar por `(externalProvider, externalId)`, soportar actualización de nombre/equipo/liga/posición y proteger la constraint ante concurrencia.
- [ ] T010 Diseñar y ejecutar la estrategia de migración/limpieza de filas creadas por `V2__seed_jugadores.sql`, comprobando que las filas legacy no coexistan con sus equivalentes externos.
- [ ] T011 [P] Crear `CacheConfig` con cachés separadas para competencias/equipos/jugadores, TTL y capacidad configurables, y política que solo almacene respuestas válidas.

**Checkpoint**: El modelo puede identificar un jugador externo sin duplicarlo y el adapter tiene un puerto independiente del proveedor.

## Phase 3: User Story 1 - Adapter seguro y resiliente (Priority: P1)

**Goal**: Encapsular Football-Data.org v4, autenticar con apikey externa, respetar límites y traducir fallas.

**Independent Test**: Mockear el HTTP del proveedor y comprobar headers, URL, timeouts, caché, rate limit y traducción de cada error sin tráfico externo real.

### Tests for User Story 1

- [ ] T012 [P] [US1] Probar en `backend/src/test/java/com/formula11/unit/adapter/FootballDataAdapterTest.java` que la apikey se envía en el header configurado, nunca en URL/cuerpo/logs y que la ausencia de apikey evita la llamada.
- [ ] T013 [P] [US1] Probar respuestas válidas de equipos/jugadores para las cinco ligas y su mapeo a `ExternalTeam`/`ExternalPlayer` normalizados.
- [ ] T014 [P] [US1] Probar traducción de `401`, `403`, `429`, `5xx`, timeout y cuerpo inválido a excepciones internas sanitizadas con `correlationId`.
- [ ] T015 [P] [US1] Probar que `Retry-After`, timeout, cantidad de reintentos y pausa mínima respetan la configuración y no generan reintentos ilimitados.
- [ ] T016 [P] [US1] Probar cache hit, cache miss, TTL y conservación de la última respuesta válida cuando el proveedor devuelve error.

### Implementation for User Story 1

- [ ] T017 [US1] Implementar `FootballDataAdapter` con `RestClient`, usando `FootballApiProperties`, header configurable y DTOs privados del proveedor dentro de `adapter`.
- [ ] T018 [US1] Implementar el mapeo de competition codes y nombres de las cinco ligas, incluyendo validación de respuestas incompletas y rechazo de registros sin identidad/datos mínimos.
- [ ] T019 [US1] Implementar timeouts de conexión/lectura/respuesta, traducción de estados HTTP, manejo de `Retry-After` y rate limit secuencial/configurable.
- [ ] T020 [US1] Integrar caché por proveedor/liga/equipo/parámetros relevantes, evitando cachear apikeys, headers sensibles o errores.
- [ ] T021 [US1] Añadir logs estructurados por llamada con `correlationId`, proveedor, liga, operación, latencia, estado sanitizado y cache hit, excluyendo secretos y cuerpos completos.

**Checkpoint**: El adapter opera con HTTP mockeado, tolera fallas y no filtra credenciales.

## Phase 4: User Story 2 - Sincronización manual e idempotente (Priority: P1)

**Goal**: Orquestar las cinco ligas y persistir altas/actualizaciones sin duplicados.

**Independent Test**: Mockear el port con resultados distintos por liga, ejecutar el service dos veces y verificar altas, actualizaciones, omisiones y estado parcial.

### Tests for User Story 2

- [ ] T022 [P] [US2] Probar en `backend/src/test/java/com/formula11/unit/service/SincronizacionServiceTest.java` la sincronización completa de las cinco ligas, cantidades creadas/actualizadas y estado `COMPLETADA`.
- [ ] T023 [P] [US2] Probar service sin apikey/configuración, fallo total y fallo parcial, verificando estados `NO_EJECUTADA`/error y `PARCIAL` sin borrar datos locales.
- [ ] T024 [P] [US2] Probar jugador repetido, jugador actualizado y jugador incompleto, verificando upsert por identidad externa y omisión reportada.
- [ ] T025 [P] [US2] Probar que el service solo usa `FootballDataPort` y repository, sin conocer URLs, headers ni tipos HTTP del proveedor.

### Implementation for User Story 2

- [ ] T026 [US2] Implementar `SincronizacionService` para recorrer las cinco ligas, validar modelos normalizados, ejecutar transacción por liga y acumular `LigaSyncResult`.
- [ ] T027 [US2] Implementar upsert local por `(externalProvider, externalId)`, diferenciando creados, actualizados, omitidos y sin cambios.
- [ ] T028 [US2] Implementar preservación de datos locales ante fallas: una liga fallida no borra sus jugadores y las ligas exitosas se confirman sin depender de una transacción global frágil.
- [ ] T029 [US2] Implementar estados globales `COMPLETADA`, `PARCIAL` y `NO_EJECUTADA`, con mensajes en español y errores sanitizados.
- [ ] T030 [US2] Añadir protección contra ejecuciones concurrentes de sincronización si el diseño lo requiere para evitar carreras de upsert y violaciones de rate limit.

**Checkpoint**: La sincronización repetida es idempotente y un fallo de una liga no rompe ni borra el catálogo local.

## Phase 5: User Story 3 - Endpoint protegido y contrato operativo (Priority: P1)

**Goal**: Exponer la sincronización manual con JWT, respuesta resumida y documentación.

**Independent Test**: Invocar MockMvc con JWT válido, ausente, inválido, sin apikey, éxito parcial y fallo total; verificar status, resumen y ausencia de secretos.

### Tests for User Story 3

- [ ] T031 [P] [US3] Probar en `backend/src/test/java/com/formula11/e2e/JugadorSincronizacionE2ETest.java` el endpoint `POST /api/jugadores/sincronizacion` con JWT válido y resumen de cinco ligas.
- [ ] T032 [P] [US3] Probar rechazo `401` sin JWT, JWT inválido/vencido y confirmar que el adapter no se invoca.
- [ ] T033 [P] [US3] Probar respuesta `200 PARCIAL`, `503` por falta de configuración/fallo total y `ErrorResponse` con `correlationId`, sin apikey ni stack trace.
- [ ] T034 [P] [US3] Probar que `GET /api/jugadores` continúa devolviendo catálogo local mientras el proveedor falla y que no se modifica la respuesta de paginación existente.

### Implementation for User Story 3

- [ ] T035 [US3] Crear `SincronizacionResponse` y `LigaSyncResultResponse` sin exponer DTOs externos, apikeys, headers sensibles ni cuerpos del proveedor.
- [ ] T036 [US3] Implementar `JugadorSincronizacionController` con `POST /api/jugadores/sincronizacion`, delegación exclusiva al service y anotaciones Swagger de seguridad/status.
- [ ] T037 [US3] Mapear errores de configuración, proveedor, rate limit y fallo total a estados HTTP y mensajes estándar mediante `GlobalExceptionHandler`.
- [ ] T038 [US3] Confirmar que `SecurityConfig` mantiene el endpoint protegido por la regla por defecto y que no se vuelve público.
- [ ] T039 [US3] Crear/actualizar `specs/004-integracion-api-futbol-catalogo/contracts/openapi.yaml` con endpoint, request sin apikey, respuesta por liga, `200`, `401`, `429`/`502`/`503` y `ErrorResponse`.

**Checkpoint**: Un usuario autenticado puede disparar una sincronización y recibe un resultado trazable sin exponer secretos.

## Phase 6: Documentation and cross-cutting validation

**Purpose**: Dejar la operación documentada y comprobar la definición de terminado.

- [ ] T040 [P] Crear/actualizar `specs/004-integracion-api-futbol-catalogo/quickstart.md` con `FOOTBALL_API_KEY`, URL base, header, timeouts, caché, rate limit, arranque sin clave y comando PowerShell/cURL de sincronización.
- [ ] T041 [P] Actualizar README operativo del backend con variables de entorno, advertencia de no commitear claves y diagnóstico de errores externos.
- [ ] T042 [P] Auditar logs y excepciones para confirmar que no aparecen apikey, Authorization completo, JWT, URL sensible ni body completo del proveedor.
- [ ] T043 [P] Auditar que ningún service/controller de catálogo realice HTTP externo y que el adapter sea el único propietario de detalles del proveedor.
- [ ] T044 Ejecutar `mvn test` con tests unitarios y, cuando Docker esté disponible, integración/E2E con Testcontainers; no usar `application-local.yml` como configuración de tests.
- [ ] T045 Ejecutar el arranque local sin apikey, comprobar `GET /api/jugadores`, y ejecutar la sincronización manual con una apikey proporcionada solo por el entorno.
- [ ] T046 Revisar Swagger, logs con `correlationId`, métricas de latencia/error y catálogo local después de éxito, parcial y fallo total.

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 Setup**: Sin dependencias; bloquea el resto por la investigación del proveedor y configuración.
- **Phase 2 Foundational**: Depende de Phase 1; prepara modelo externo, migración, port y caché.
- **Phase 3 US1**: Depende de properties, port y caché; habilita consultas mockeadas al proveedor.
- **Phase 4 US2**: Depende de adapter y persistencia; implementa el upsert y la orquestación.
- **Phase 5 US3**: Depende del service; expone endpoint, seguridad, errores y contrato.
- **Phase 6 Documentation**: Depende de todas las fases y de un flujo funcional probado.

### User Story Dependencies

- **US1**: Puede desarrollarse después de T001-T011 y debe quedar testeable sin red real.
- **US2**: Requiere el puerto/adapter y la migración de identidad externa.
- **US3**: Requiere el service de sincronización y las reglas JWT existentes.

### Parallel Opportunities

- Phase 1: T002-T004 pueden avanzar en paralelo después de T001.
- Phase 2: T007-T008 y T011 pueden avanzar en paralelo; T006/T009/T010 requieren coordinación de esquema.
- US1: T012-T016 pueden escribirse en paralelo; T017-T021 integran el adapter y su infraestructura.
- US2: T022-T025 pueden prepararse en paralelo; T026-T030 deben integrarse sobre el mismo service.
- US3: T031-T034 pueden escribirse en paralelo; T035-T039 integran contrato y controller.
- Phase 6: T040-T043 pueden avanzar en paralelo; T044-T046 son validación final.

## Implementation Strategy

### MVP First

1. Confirmar el contrato real de Football-Data.org v4 y completar configuración segura.
2. Implementar migración/identidad externa, port y adapter mockeado.
3. Implementar sincronización de una liga con upsert y extenderla a las cinco.
4. Implementar estados parcial/fallo y endpoint JWT.
5. Validar catálogo local, tests y documentación antes de operar con una apikey real.

### Definition of Done

La feature está terminada cuando la aplicación arranca sin apikey, el endpoint protegido sincroniza las cinco ligas mediante el adapter, los jugadores se actualizan por proveedor/id externo sin duplicados, la caché y los límites protegen al proveedor, los fallos preservan el catálogo local, logs y errores no exponen secretos, OpenAPI/quickstart están actualizados y pasan tests unitarios, integración y E2E sin tráfico real externo.

## Notes

- `[P]` indica que la tarea puede ejecutarse en paralelo sin depender de trabajo incompleto en el mismo archivo.
- Los nombres de dominio deben conservar español sin acentos ni `ñ` en identificadores.
- `Football-Data.org v4` es la implementación inicial; FutbolOK solo cambia configuración/adaptador, no el contrato del service.
- No se debe modificar ni eliminar ningún test existente sin permiso explícito.
