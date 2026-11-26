# Feature Specification: Actuator y Prometheus

**Feature Branch**: `entrega3-observabilidad`

**Created**: 2026-11-26

**Status**: Draft

**Input**: Entrega 3, integrante B (Observabilidad): "Actuator: exponer prometheus y loggers, completar info con nombre y versión, `/actuator/health` público y el resto solo superusuario en SecurityConfig, un health indicator propio (por ejemplo, si falló la última sincronización de métricas). Prometheus: micrometer-registry-prometheus, métricas de negocio con MeterRegistry (contador de operaciones de mercado por tipo compra/venta/P2P, combinado con A, y timer del recálculo de cotizaciones), docker-compose opcional."

## Scope

La aplicación expone su estado y sus métricas para que el equipo pueda saber si está sana y qué está pasando en el mercado sin mirar la base ni los logs. Hay un chequeo de salud público que además avisa si falló la última carga de métricas de WhoScored. El resto de la información operativa (versión, métricas, niveles de log) queda solo para el superusuario. Se agregan dos métricas de negocio: cuántas operaciones de mercado se hacen por tipo y cuánto tarda el recálculo de cotizaciones.

Quedan fuera: el endpoint `GET /api/metricas/avanzadas` (feature 017), la auditoría de web services (feature 015, ya hecha) y la implementación del mercado P2P, que es de A. Esta feature deja lista la métrica para el tipo P2P y A la incrementa cuando confirme una operación P2P.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Saber si la aplicación está sana (Priority: P1)

Como integrante del equipo que opera el sistema, quiero consultar sin autenticarme si la aplicación está sana, y que ese chequeo avise si falló la última carga de métricas de WhoScored, para detectar a tiempo que las cotizaciones de la semana se van a calcular con datos viejos.

**Why this priority**: Es el chequeo mínimo de operación y el health indicator propio es un ítem explícito de la consigna. Funciona solo, sin las otras historias.

**Independent Test**: Consultar el chequeo de salud sin sesión después de una carga de métricas completada (responde sano) y después de una fallida (responde caído); con sesión de superusuario, ver además el detalle de la última carga.

**Acceptance Scenarios**:

1. **Given** la aplicación levantada y la última carga de métricas completada, **When** alguien sin sesión consulta el chequeo de salud, **Then** recibe estado `UP` y no ve ningún detalle interno.
2. **Given** que la última carga de métricas terminó `FALLIDA`, **When** se consulta el chequeo de salud, **Then** el estado general es `DOWN`.
3. **Given** que la última carga terminó `PARCIAL` (alguna liga falló), **When** se consulta el chequeo de salud, **Then** el estado es `UP` y el detalle indica que la carga fue parcial.
4. **Given** que todavía no hubo ninguna carga de métricas, **When** se consulta el chequeo de salud, **Then** el estado es `UP` y el detalle indica que no hay cargas.
5. **Given** un superusuario autenticado, **When** consulta el chequeo de salud, **Then** ve el detalle de cada componente, y en el de carga de métricas, el estado, la fecha de fin y el identificador de la última carga terminada.

---

### User Story 2 - Información operativa solo para el superusuario (Priority: P1)

Como superusuario, quiero consultar la versión de la aplicación, sus métricas técnicas y de negocio, y ver o cambiar el nivel de log en caliente, sin que un usuario común ni alguien sin sesión pueda hacerlo.

**Why this priority**: Expone la información que pide la consigna y, sobre todo, la protege: sin esta historia, cualquiera podría cambiar el nivel de log o ver datos internos. Va junto con la historia 1.

**Independent Test**: Consultar la información, las métricas, el formato para Prometheus y los niveles de log con tres sesiones: sin sesión (rechazado por falta de autenticación), usuario común (rechazado por falta de permiso) y superusuario (permitido).

**Acceptance Scenarios**:

1. **Given** un superusuario, **When** consulta la información de la aplicación, **Then** ve el nombre y la versión de la aplicación.
2. **Given** un superusuario, **When** consulta las métricas en formato Prometheus, **Then** recibe el texto con las métricas técnicas y las de negocio.
3. **Given** un superusuario, **When** cambia en caliente el nivel del registro de auditoría a `WARN`, **Then** las llamadas siguientes a la API dejan de escribir líneas de auditoría hasta que lo vuelva a `INFO`, sin reiniciar la aplicación.
4. **Given** alguien sin sesión, **When** consulta cualquier endpoint operativo que no sea el chequeo de salud, **Then** recibe 401 con el mismo formato de error que el resto de la API.
5. **Given** un usuario común autenticado, **When** consulta cualquier endpoint operativo que no sea el chequeo de salud, **Then** recibe 403.

---

### User Story 3 - Métricas de negocio del mercado y del recálculo (Priority: P2)

Como superusuario, quiero ver cuántas operaciones de mercado se hicieron por tipo (compra, venta y P2P) y cuánto tarda el recálculo de cotizaciones, para seguir la actividad del mercado y detectar si el recálculo se vuelve lento.

**Why this priority**: Es el aporte de negocio de Prometheus. Depende de que la historia 2 exponga las métricas, pero se puede probar consultando el registro de métricas directamente.

**Independent Test**: Hacer una compra y una venta, correr un recálculo de cotizaciones y verificar que el contador de compras y el de ventas subieron en uno y que el timer del recálculo registró una ejecución con su duración.

**Acceptance Scenarios**:

1. **Given** la aplicación recién levantada, **When** se consultan las métricas, **Then** el contador de operaciones de mercado ya aparece para los tres tipos (`compra`, `venta` y `p2p`) en cero.
2. **Given** un usuario con saldo, **When** compra tokens con éxito, **Then** el contador de operaciones de tipo `compra` sube en uno.
3. **Given** un usuario con tokens, **When** los vende con éxito, **Then** el contador de tipo `venta` sube en uno.
4. **Given** una compra o venta rechazada (por ejemplo, por saldo o tokens insuficientes), **When** termina, **Then** ningún contador cambia.
5. **Given** un recálculo de cotizaciones, manual o del job semanal, **When** termina bien o con error, **Then** el timer del recálculo registra una ejecución más con su duración y su resultado (`ok` o `error`).
6. **Given** que A confirma una operación P2P, **When** llama al registro de operaciones con el tipo `p2p`, **Then** el contador de tipo `p2p` sube en uno.

---

### User Story 4 - Ver las métricas en un Prometheus local (Priority: P3)

Como integrante del equipo, quiero levantar un Prometheus local con un solo comando que lea las métricas de la aplicación, para mostrar en la demo cómo evolucionan.

**Why this priority**: La consigna lo marca como opcional. Suma a la demo, pero las historias 1 a 3 ya cumplen el ítem.

**Independent Test**: Con la aplicación corriendo, levantar el Prometheus local, cargar el token del superusuario y ver en su interfaz que el contador de operaciones de mercado aparece y sube después de una compra.

**Acceptance Scenarios**:

1. **Given** la aplicación corriendo en la máquina y un token de superusuario guardado donde lo indica la guía, **When** se levanta el Prometheus local, **Then** el objetivo de la aplicación figura como activo y se pueden consultar las métricas de negocio.

---

### Edge Cases

- Una carga de métricas en curso no cuenta como "última carga": el chequeo de salud mira la última carga terminada (completada, parcial o fallida).
- Si después de una carga fallida hay una completada, el chequeo vuelve a `UP` sin reiniciar nada.
- Si no se puede consultar el estado de las cargas (por ejemplo, la base no responde), el componente de carga de métricas informa `DOWN` con el motivo, sin romper el chequeo de salud.
- El chequeo de salud público nunca muestra detalles (versiones, rutas, errores, identificadores): solo el estado.
- Un token vencido o inválido en un endpoint operativo se trata igual que sin sesión (401).
- Los endpoints operativos no pasan por la auditoría de web services (feature 015): no son endpoints de la API propia.
- La asignación de tokens al superusuario cuando se sincroniza el catálogo no es una compra ni una venta y no se cuenta.
- Si falla el registro de una métrica, la operación de mercado o el recálculo terminan igual: las métricas nunca rompen una operación.
- El contador `p2p` queda en cero hasta que A integre su feature; no es un error.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST ofrecer un chequeo de salud público, sin autenticación, que responda solo el estado general (`UP` o `DOWN`).
- **FR-002**: El chequeo de salud MUST incluir un componente propio de carga de métricas: `DOWN` si la última carga terminada fue `FALLIDA`; `UP` si fue `COMPLETADA` o `PARCIAL`, o si no hubo ninguna; el detalle MUST indicar el estado, la fecha de fin y el identificador de esa carga.
- **FR-003**: El detalle de los componentes del chequeo de salud MUST mostrarse solo al superusuario.
- **FR-004**: El sistema MUST exponer, solo para el superusuario, la información de la aplicación (nombre y versión), las métricas, las métricas en formato Prometheus y la consulta y el cambio de niveles de log.
- **FR-005**: Los endpoints operativos distintos del chequeo de salud MUST responder 401 sin sesión, con el formato de error de la API, y 403 a un usuario que no es superusuario.
- **FR-006**: El sistema MUST contar las operaciones de mercado confirmadas por tipo (`compra`, `venta`, `p2p`) y los tres tipos MUST existir en cero desde el arranque.
- **FR-007**: El sistema MUST ofrecer una sola forma de registrar una operación de mercado por tipo, para que la feature P2P de A la use sin duplicar la métrica.
- **FR-008**: El sistema MUST medir la duración de cada recálculo de cotizaciones (manual o del job semanal) con su resultado (`ok` o `error`).
- **FR-009**: Una falla al registrar una métrica MUST NOT cambiar el resultado de la operación de mercado ni del recálculo.
- **FR-010**: El repositorio MUST incluir una forma opcional de levantar un Prometheus local que lea las métricas de la aplicación, con una guía de uso.

### Key Entities

- **Carga de métricas** (ya existe): cada ejecución de la carga de WhoScored, con su estado (`EN_CURSO`, `COMPLETADA`, `PARCIAL`, `FALLIDA`), inicio y fin. El chequeo de salud lee la última terminada; esta feature no la cambia.
- **Contador de operaciones de mercado**: cantidad acumulada de operaciones confirmadas desde que arrancó la aplicación, separada por tipo. No se guarda en la base.
- **Timer del recálculo**: cantidad de recálculos, duración total y máxima, separado por resultado. No se guarda en la base.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Después de una carga de métricas fallida, el chequeo de salud informa `DOWN` en la consulta siguiente, sin reiniciar la aplicación.
- **SC-002**: El 100 % de los endpoints operativos distintos del chequeo de salud rechazan a quien no tiene sesión (401) y a los usuarios comunes (403), verificado con un test por endpoint.
- **SC-003**: Después de N compras y M ventas confirmadas, los contadores muestran exactamente N y M; las operaciones rechazadas no suman.
- **SC-004**: Cada recálculo de cotizaciones suma exactamente una ejecución al timer, termine bien o con error.
- **SC-005**: Ningún test existente cambia su resultado: los endpoints de la API responden igual que antes.

## Assumptions

- "Sincronización de métricas" es la carga de métricas de WhoScored (manual o del job semanal), que ya guarda cada ejecución con su estado.
- Una carga `PARCIAL` deja la aplicación usable (las ligas que cargaron sirven para cotizar), por eso no la marca caída; solo `FALLIDA` lo hace.
- Que el chequeo de salud responda `DOWN` cuando falló la carga es intencional: es la señal que pide la consigna. El chequeo de la base de datos y los demás que ya trae la plataforma se mantienen.
- La versión que se muestra es la del proyecto en el build; el nombre es el de la aplicación.
- Los contadores y el timer viven en memoria y se reinician con la aplicación; Prometheus es quien guarda la historia.
- Solo se cuentan compras y ventas de usuarios contra el superusuario (las operaciones de mercado actuales) y, cuando exista, las P2P de A.
- El Prometheus local usa un token de superusuario que se guarda en un archivo fuera del control de versiones; como el token vence, la guía explica cómo renovarlo.
- `application.yml` y la seguridad de `/actuator/**` las cambia solo B, en un bloque aparte con comentario (acuerdo del grupo para la entrega 3). En `pom.xml`, B agrega solo su dependencia.
