# Feature Specification: Integración con API externa de fútbol para poblar el catálogo

**Feature Branch**: `004-integracion-api-futbol-catalogo`

**Created**: 2026-10-07

**Status**: Draft

**Input**: User description: "Integrar Football-Data.org / FutbolOK mediante apikey para obtener equipos y jugadores de las cinco ligas del dominio, persistirlos localmente y sincronizarlos manualmente sin romper el catálogo cuando el proveedor falle."

**Related features**: El catálogo local y su paginación se mantienen definidos por `specs/003-paginacion-catalogo-jugadores/`; esta feature agrega la fuente externa y la operación de sincronización.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Configurar el proveedor sin exponer secretos (Priority: P1)

Como operador del backend, quiero configurar la apikey mediante el entorno para que la aplicación pueda usar el proveedor sin guardar secretos en el repositorio ni mostrarlos en logs o respuestas.

**Why this priority**: La integración no puede operar de forma segura si la credencial depende del código fuente o bloquea el arranque cuando falta.

**Independent Test**: Iniciar la aplicación con y sin `FOOTBALL_API_KEY`, inspeccionar la configuración efectiva y ejecutar una sincronización sin encontrar la apikey en logs ni cuerpos HTTP.

**Acceptance Scenarios**:

1. **Given** `FOOTBALL_API_KEY` está configurada, **When** se ejecuta una sincronización, **Then** el adapter la envía al header requerido por el proveedor y nunca la incluye en URLs, logs ni respuestas.
2. **Given** `FOOTBALL_API_KEY` no está configurada, **When** arranca la aplicación, **Then** el arranque termina correctamente y el catálogo local continúa disponible.
3. **Given** la apikey no está configurada, **When** se solicita una sincronización, **Then** la operación informa claramente que la integración no está configurada, no llama al proveedor y no modifica el catálogo local.
4. **Given** una respuesta de error del proveedor, **When** se construye el `ErrorResponse`, **Then** el mensaje no contiene la apikey, headers sensibles ni el cuerpo secreto de autenticación.

---

### User Story 2 - Sincronizar equipos y jugadores manualmente (Priority: P1)

Como usuario autorizado del backend, quiero disparar manualmente una sincronización para poblar o actualizar el catálogo local con los equipos y jugadores de las cinco ligas soportadas.

**Why this priority**: Permite reemplazar el seed como fuente operativa sin introducir todavía un job programado.

**Independent Test**: Mockear respuestas del proveedor para las cinco ligas, invocar el endpoint con JWT y comprobar el upsert local, el resultado por liga y la ausencia de duplicados.

**Acceptance Scenarios**:

1. **Given** una apikey válida y el endpoint protegido, **When** un usuario autenticado ejecuta `POST /api/jugadores/sincronizacion`, **Then** el sistema consulta Football-Data.org v4 para las cinco ligas configuradas, guarda los jugadores normalizados y devuelve un resumen de la operación.
2. **Given** un jugador externo ya persistido, **When** una sincronización vuelve a recibir el mismo id externo, **Then** actualiza sus datos sin insertar una segunda fila.
3. **Given** un jugador externo que cambia de equipo, posición o nombre, **When** se sincroniza nuevamente, **Then** se actualiza la fila identificada por su id externo y el catálogo paginado refleja el valor vigente.
4. **Given** una sincronización exitosa repetida con la misma respuesta, **When** se ejecuta dos veces, **Then** el número de jugadores locales no se duplica y el resumen distingue altas de actualizaciones.
5. **Given** una solicitud sin JWT, con JWT inválido o vencido, **When** intenta iniciar la sincronización, **Then** recibe el rechazo estándar de autenticación y el proveedor no es llamado.
6. **Given** una sincronización en curso, **When** consulta el catálogo local, **Then** el endpoint de catálogo sigue leyendo la base local y no depende de una llamada HTTP al proveedor.
7. **Given** algunas ligas se sincronizan correctamente y otras fallan, **When** termina la operación, **Then** responde `200` con estado `PARCIAL`, persiste las ligas exitosas y conserva los datos previos de las fallidas.

---

### User Story 3 - Mantener el catálogo ante fallas externas (Priority: P1)

Como usuario del catálogo, quiero que los datos locales sigan disponibles aunque Football-Data.org/FutbolOK esté caído, limite las solicitudes o rechace la credencial.

**Why this priority**: El proveedor externo es una dependencia auxiliar; su indisponibilidad no debe convertir el catálogo local en una pantalla de error.

**Independent Test**: Sembrar datos locales, simular cada clase de fallo del proveedor y consultar el catálogo antes y después de la sincronización.

**Acceptance Scenarios**:

1. **Given** un timeout del proveedor, **When** se ejecuta una sincronización, **Then** la operación termina con un mensaje claro y los datos locales previos siguen consultables.
2. **Given** una respuesta `401` o `403`, **When** el proveedor rechaza la apikey, **Then** se informa que la credencial externa es inválida o no está autorizada, sin revelar su valor ni borrar datos locales.
3. **Given** una respuesta `429`, **When** se alcanza el límite del plan, **Then** se informa el límite externo, se respeta `Retry-After` si está disponible y no se realizan reintentos agresivos.
4. **Given** una respuesta `5xx` o cuerpo inválido, **When** falla una liga, **Then** esa liga queda reportada como fallida, las demás ligas exitosas pueden persistirse y el catálogo anterior no se elimina.
5. **Given** una respuesta cacheada vigente, **When** el proveedor no está disponible, **Then** el adapter puede usar la respuesta cacheada para completar o mantener la sincronización sin una request adicional.
6. **Given** una respuesta cacheada vencida o inexistente y el proveedor no está disponible, **When** se solicita sincronización, **Then** se informa el fallo sin inventar jugadores ni alterar los datos locales existentes.

---

### User Story 4 - Operar y auditar la integración (Priority: P1)

Como operador, quiero identificar qué ocurrió en cada sincronización para investigar errores sin registrar secretos ni datos innecesarios.

**Why this priority**: Las llamadas externas deben ser trazables y sus límites observables sin comprometer credenciales.

**Independent Test**: Ejecutar sincronizaciones exitosas, parciales y fallidas con un `X-Correlation-Id` conocido e inspeccionar logs, respuesta resumida y documentación.

**Acceptance Scenarios**:

1. **Given** una llamada al proveedor, **When** se registra su actividad, **Then** el log estructurado incluye `correlationId`, proveedor, liga, operación, estado HTTP sanitizado, duración y resultado, sin apikey ni token completo.
2. **Given** una sincronización de varias ligas, **When** termina, **Then** la respuesta informa estado global, resultado por liga, jugadores creados/actualizados y errores resumidos.
3. **Given** la API configurada, **When** un operador consulta Swagger, **Then** encuentra el endpoint protegido, sus respuestas, los estados de fallo y la configuración de entorno necesaria en la documentación operativa.

## Edge Cases

- La apikey vacía o compuesta solo por espacios se considera no configurada.
- El header de autenticación del proveedor (`X-Auth-Token` por defecto para Football-Data.org) se configura junto con la URL base para permitir compatibilidad con el proveedor seleccionado; los services no conocen ese detalle.
- La URL base, timeouts, TTL y límites de caché son configurables y tienen valores seguros para desarrollo; la apikey no tiene valor por defecto real.
- La lista de ligas es exactamente: Premier League, Bundesliga, La Liga, Serie A y Ligue 1; el adapter traduce sus nombres a los identificadores del proveedor.
- Una liga sin equipos o sin jugadores válidos no debe borrar los jugadores locales de esa liga.
- Un jugador sin id externo, nombre, equipo, liga o posición utilizable se rechaza y se reporta en el resumen sin abortar necesariamente las demás ligas.
- Dos respuestas del proveedor con el mismo id externo no crean duplicados aunque difieran en nombre, equipo o posición.
- Un id externo se considera dentro del namespace del proveedor; el modelo debe impedir colisiones entre proveedores si en el futuro se agrega otro adapter.
- Las respuestas cacheadas se invalidan o reemplazan solo después de una respuesta válida; los errores no deben sobrescribir la caché sana.
- Las llamadas deben respetar el límite configurado del plan: no se permiten reintentos ilimitados ni paralelismo sin límite.
- El endpoint de sincronización es idempotente respecto del id externo y no crea una segunda ejecución programada.
- Una excepción del proveedor no debe filtrarse como stack trace, URL completa, apikey, token o body sensible.
- Los registros creados exclusivamente por `V2__seed_jugadores.sql` se consideran legacy y deben eliminarse o migrarse antes de la primera sincronización externa para que no coexistan duplicados con los jugadores importados.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE implementar un adapter en una capa `Adapters` separada de controllers, services y repositories para encapsular toda llamada HTTP al proveedor externo.
- **FR-002**: Los services DEBEN depender de una interfaz/puerto del adapter y NO DEBEN conocer URLs, headers, códigos HTTP ni DTOs propietarios del proveedor.
- **FR-003**: La apikey DEBE leerse desde configuración externa, con `FOOTBALL_API_KEY` como variable documentada, y NUNCA DEBE hardcodearse, commitearse, loguearse ni devolverse en errores.
- **FR-004**: El header, URL base y credencial del proveedor DEBEN configurarse fuera de la lógica de negocio; el adapter DEBE enviar la apikey en el header requerido por el proveedor.
- **FR-005**: La ausencia de la apikey NO DEBE impedir el arranque de la aplicación; la sincronización DEBE informar que la integración no está configurada y el catálogo local DEBE seguir funcionando.
- **FR-006**: El sistema DEBE exponer una operación manual protegida por JWT para sincronizar equipos y jugadores de las cinco ligas del dominio.
- **FR-007**: La operación manual DEBE ser idempotente y DEBE usar el id externo del proveedor, junto con el namespace del proveedor, como clave de correspondencia para altas y actualizaciones.
- **FR-008**: La persistencia DEBE almacenar el identificador externo necesario para realizar upsert y DEBE impedir duplicados para el mismo proveedor e id externo.
- **FR-009**: La sincronización DEBE normalizar la respuesta externa a un modelo interno antes de persistirla; ningún DTO propietario del proveedor DEBE filtrarse al controller o al catálogo.
- **FR-010**: La sincronización DEBE cubrir exactamente Premier League, Bundesliga, La Liga, Serie A y Ligue 1 y DEBE informar el resultado individual de cada liga.
- **FR-011**: Los datos locales existentes NO DEBEN eliminarse ni dejar de estar disponibles por un fallo parcial o total del proveedor.
- **FR-012**: El adapter DEBE configurar timeouts de conexión, lectura y respuesta, y DEBE traducir `401`, `403`, `429`, `5xx`, respuestas inválidas y timeout a errores internos claros en español.
- **FR-013**: Ante `429`, el sistema DEBE respetar `Retry-After` cuando exista y DEBE limitar los reintentos según una política configurable; NO DEBE generar reintentos agresivos o ilimitados.
- **FR-014**: El sistema DEBE cachear las respuestas externas por proveedor, liga y parámetros relevantes, con TTL y capacidad configurables, sin cachear apikeys ni datos de autenticación.
- **FR-015**: La caché DEBE conservar la última respuesta válida y NO DEBE reemplazarla con errores; si el proveedor falla, el adapter DEBE poder usar una entrada cacheada vigente.
- **FR-016**: Cada llamada externa DEBE producir un log estructurado con `correlationId`, proveedor, liga, operación, estado sanitizado, duración y resultado, sin secretos ni tokens completos.
- **FR-017**: La respuesta de sincronización DEBE incluir estado global (`COMPLETADA`, `PARCIAL` o `NO_EJECUTADA`), resultados por liga, cantidades creadas/actualizadas y errores resumidos sin secretos.
- **FR-018**: Si al menos una liga se sincroniza correctamente y otra falla, el endpoint DEBE responder `200` con estado `PARCIAL`; si ninguna liga puede sincronizarse por falta de configuración o fallo total del proveedor, DEBE responder un error claro (`503` para configuración/indisponibilidad o el estado HTTP externo normalizado) sin modificar los datos locales.
- **FR-019**: La documentación Swagger/OpenAPI DEBE describir el endpoint protegido, su resumen de sincronización, estados de éxito/error y la ausencia de apikey en el contrato HTTP del cliente.
- **FR-020**: El README o quickstart DEBE documentar `FOOTBALL_API_KEY`, URL base, header configurable, timeouts, caché, comando de arranque y cómo ejecutar la sincronización manual sin incluir una clave real.
- **FR-021**: DEBEN existir tests unitarios del adapter con el proveedor HTTP mockeado, tests unitarios del service con el adapter mockeado y tests de integración de persistencia contra PostgreSQL/Testcontainers para upsert y ausencia de duplicados.
- **FR-022**: Ningún test DEBE realizar llamadas a Football-Data.org/FutbolOK real ni requerir una apikey real.
- **FR-023**: El job programado, scraping de WhoScored, cotizaciones, cambios en frontend y mercado de tokens DEBEN permanecer fuera de esta feature.

### Key Entities

- **Jugador**: Entidad local existente, ampliada con `externalId` y `externalProvider` para identificar de forma única el jugador importado y conservar nombre, equipo, liga y posición locales.
- **ExternalPlayer**: Modelo normalizado interno del adapter con id externo, nombre, equipo, liga y posición; no conoce DTOs HTTP del proveedor.
- **ExternalTeam**: Modelo normalizado de equipo con id externo, nombre y liga, usado para obtener o asociar jugadores.
- **LigaSyncResult**: Resultado por liga con estado, cantidades creadas/actualizadas, uso de caché y error sanitizado.
- **SyncResponse**: Resumen de la operación manual con estado global, resultados de las cinco ligas, totales y `correlationId` de trazabilidad.
- **ExternalApiConfiguration**: Configuración opcional de URL base, variable de apikey, header, timeouts, TTL, capacidad y límites de requests; nunca contiene un valor secreto serializable en respuestas.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: La aplicación arranca correctamente en el 100% de los entornos de prueba sin `FOOTBALL_API_KEY` y el catálogo local sigue respondiendo.
- **SC-002**: El 100% de las sincronizaciones con una respuesta externa válida para las cinco ligas persiste jugadores y equipos normalizados sin duplicar ids externos.
- **SC-003**: Dos sincronizaciones consecutivas con la misma respuesta dejan la misma cantidad de filas y registran la segunda operación como actualización o sin cambios, nunca como duplicación.
- **SC-004**: El 100% de los errores mockeados (`401`, `403`, `429`, `5xx`, timeout y cuerpo inválido) produce un resultado claro y conserva los datos locales previos; los fallos parciales responden `200` con estado `PARCIAL`.
- **SC-005**: El 100% de las llamadas externas usa timeout, header configurado, correlation ID en logs y una política de requests compatible con el límite configurado.
- **SC-006**: Una entrada cacheada válida permite completar o mantener la sincronización sin llamar nuevamente al proveedor cuando este no está disponible.
- **SC-007**: Ningún test de la feature realiza tráfico de red hacia el proveedor real ni requiere una credencial real.
- **SC-008**: Swagger/OpenAPI y README/quickstart documentan el endpoint manual y todas las variables de configuración antes de dar la feature por terminada.
- **SC-009**: Ninguna respuesta, log, excepción o reporte de sincronización contiene `FOOTBALL_API_KEY`, su valor, headers sensibles o tokens completos.

## Assumptions

- `Football-Data.org` v4 es el proveedor inicial y su header por defecto es `X-Auth-Token`; si el despliegue usa la variante denominada FutbolOK, el nombre del header y la URL base se cambian solo mediante configuración.
- Los identificadores de competencias, endpoints de equipos/jugadores y límites de requests de Football-Data.org v4 se encapsulan en el adapter y no forman parte del contrato del service.
- El proveedor puede no ofrecer el mismo nivel de detalle para jugadores en todas sus rutas; el adapter debe resolver la fuente configurada y rechazar registros incompletos sin corromper la base local.
- La caché inicial será in-memory con TTL configurable, salvo que el plan técnico determine que el entorno ya dispone de Redis; la abstracción debe permitir cambiar la implementación sin tocar el service.
- La migración de persistencia agregará la identidad externa y eliminará o migrará los registros heredados del seed V2 antes de la primera sincronización externa, evitando que las filas legacy coexistan con sus equivalentes importadas.
- Solo usuarios autenticados pueden disparar la sincronización; no se añade un rol administrativo en esta feature porque no fue solicitado.
- La sincronización puede terminar en estado `PARCIAL` si algunas ligas se procesan y otras fallan; nunca debe borrar el catálogo local para representar una falla externa.

## Out of Scope

- Job programado, scheduler, refresh automático o sincronización nocturna.
- Scraping de WhoScored u otra fuente no contratada.
- Cotizaciones, métricas deportivas para valuación, tokens, compras y ventas.
- Cambios en frontend o en el contrato de paginación del catálogo.
- Exposición de un endpoint que reenvíe directamente respuestas o credenciales del proveedor.
