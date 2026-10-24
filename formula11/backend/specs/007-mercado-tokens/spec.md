# Feature Specification: Compra y venta de tokens de jugadores

**Feature Branch**: `007-mercado-tokens`

**Created**: 2026-10-24

**Status**: Draft

**Input**: User description: "Compra y venta de tokens de jugadores contra el superusuario. Cuenta con saldo en créditos por usuario; tenencia (usuario, jugador, cantidad, precio promedio); transaccion (tipo, cantidad, precio, contraparte, fecha); auditoría append-only inmutable (autor, antes, después, timestamp). Seed del superusuario con 100 tokens por jugador a 1 crédito. Endpoints de órdenes según el contrato 005. Validar saldo suficiente y tokens disponibles. El precio se obtiene por la interfaz CotizacionVigentePort, que devuelve 1 crédito fijo hasta que esté la estrategia de A. La compra y la venta son métodos de service reutilizables fuera de HTTP (para la compra en un click del feature extra). Clock inyectable para la fecha de las operaciones. Migraciones Flyway en el rango V20 a V29."

**Related features**: Reutiliza la autenticación y los usuarios de `specs/001-registro-catalogo-jugadores/` y `specs/002-login-usuarios/`, y el catálogo de jugadores de `specs/003-paginacion-catalogo-jugadores/` y `specs/004-integracion-api-futbol-catalogo/`. La forma de las solicitudes y respuestas HTTP la fija el contrato `specs/005-contratos-entrega2/contracts/openapi.yaml` (operaciones con `x-feature: 007-mercado-tokens`). Respeta la constitución v2.1.0 (secciones 9, Clock inyectable, y 10, operaciones de tokens reutilizables).

## Clarifications

### Session 2026-10-24

- Q: ¿Cuál debe ser el saldo inicial de créditos asignado a cada usuario al registrarse y a los usuarios existentes durante la migración? → A: 100 créditos por usuario.
- Q: ¿Qué conjunto de rutas debe considerarse el contrato definitivo de esta feature? → A: Solo las rutas y esquemas del contrato 005 versionado en el repositorio.
- Q: ¿Qué debe ocurrir si las credenciales del superusuario no están configuradas al iniciar la aplicación? → A: En perfiles `local` y `test` se usan credenciales de desarrollo por defecto; fuera de esos perfiles, el arranque falla explícitamente si faltan `SUPERUSUARIO_EMAIL` o `SUPERUSUARIO_PASSWORD`.
- Q: ¿Qué datos del superusuario se persisten? → A: Es una fila de `usuarios` con rol `SUPERUSUARIO` (puede loguearse y administrar estrategias). Sus tokens se guardan en `tenencias` (100 por jugador al inicio) y son el stock disponible. No tiene cuenta: sus créditos no existen ni se registran.
- Q: ¿Qué registra la auditoría de cada operación? → A: Saldo del usuario antes y después, y tokens del jugador del usuario y del superusuario antes y después. Los créditos del superusuario no aparecen porque no existen.
- Q: ¿Qué pasa si el superusuario llama a los endpoints de órdenes? → A: Recibe `403`, porque operaría contra sí mismo. Lo mismo para su portfolio (feature 009). B agrega esos `403` al contrato 005 en la rama `005-contrato-403-superusuario` (PR aprobado por las tres).
- Q: ¿Qué pasa con los jugadores `LEGACY` que la sincronización borra, si ya tienen tenencias? → A: Se borran solo los que no tienen transacciones, junto con la tenencia del superusuario. Los que tienen transacciones se conservan.
- Q: ¿Cómo se reparten las cuentas entre migraciones? → A: V21 solo crea la tabla `cuentas`; V25 crea las cuentas de los usuarios existentes; `UsuarioService` crea la de cada registro nuevo.

## Alcance según el contrato 005

Esta feature implementa (o modifica) estas operaciones del contrato:

| Operación | Cambio |
|-----------|--------|
| `POST /api/ordenes/compra` | Nueva |
| `POST /api/ordenes/venta` | Nueva |
| `GET /api/jugadores/{jugadorId}/mercado` | Nueva |
| `POST /api/usuarios/registro` | Existente: al registrarse se crea la cuenta con el saldo inicial |
| `POST /api/usuarios/login` | Existente: la respuesta y el JWT incluyen el rol (`USUARIO` o `SUPERUSUARIO`) |

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Comprar tokens de un jugador (Priority: P1)

Como usuario autenticado, quiero comprar tokens de un jugador al precio vigente para aumentar mi tenencia y que los créditos y tokens se actualicen de forma consistente.

**Why this priority**: La compra es el flujo principal del mercado y habilita la inversión de usuarios en jugadores.

**Independent Test**: Sembrar un jugador, saldo suficiente y tokens disponibles en la tenencia del superusuario; invocar el método de service o `POST /api/ordenes/compra` y comprobar saldo, tenencia, stock del superusuario, transacción y auditoría.

**Acceptance Scenarios**:

1. **Given** un usuario autenticado con saldo suficiente y el superusuario con tokens disponibles, **When** solicita comprar una cantidad válida de tokens de un jugador, **Then** se ejecuta la operación al precio vigente, se descuenta el total de su saldo, se incrementa su tenencia, se descuentan los tokens de la tenencia del superusuario y se responde `201` con `OperacionResponse`.
2. **Given** un usuario que ya posee tokens del jugador, **When** compra más tokens, **Then** la tenencia se acumula y el precio promedio se recalcula como promedio ponderado de la tenencia anterior y la nueva compra.
3. **Given** una compra confirmada, **When** se consulta la operación, **Then** existe una transacción de tipo `COMPRA` con cantidad, precio unitario, total, contraparte, fecha, estrategia y versión de la cotización usada, junto con un registro de auditoría inmutable del estado anterior y posterior.
4. **Given** un usuario con saldo menor al total, **When** solicita comprar, **Then** se responde `422` sin cambios persistidos.
5. **Given** el superusuario sin tokens suficientes del jugador, **When** un usuario solicita comprar, **Then** se responde `409` sin cambios persistidos.

### User Story 2 - Vender tokens al superusuario (Priority: P1)

Como usuario autenticado, quiero vender tokens que poseo al superusuario al precio vigente para recibir créditos y reducir mi tenencia.

**Why this priority**: La venta completa el intercambio contra el superusuario y permite recuperar el saldo de la inversión.

**Independent Test**: Crear una tenencia suficiente para un usuario; invocar el método de service o `POST /api/ordenes/venta` y comprobar el aumento del saldo, la reducción de la tenencia, el stock del superusuario, la transacción y la auditoría.

**Acceptance Scenarios**:

1. **Given** un usuario autenticado con cantidad suficiente de tokens de un jugador, **When** solicita vender una cantidad válida, **Then** se ejecuta al precio vigente, se acredita el total en su saldo, se reduce su tenencia, los tokens vuelven a la tenencia del superusuario y se responde `201` con `OperacionResponse`.
2. **Given** una solicitud de venta por una cantidad mayor a la tenencia del usuario, **When** se procesa, **Then** se responde `422` sin modificar saldo, tenencia, stock, transacciones ni auditorías.
3. **Given** una venta confirmada, **When** se consulta la operación, **Then** existe una transacción de tipo `VENTA` con el superusuario como contraparte, la fecha provista por el `Clock` y su auditoría correspondiente.
4. **Given** una venta que deja la tenencia del usuario en cero, **When** se confirma, **Then** la tenencia queda con cantidad `0` (no se elimina) y conserva su precio promedio histórico.

### User Story 3 - Consultar los datos de mercado de un jugador (Priority: P1)

Como usuario autenticado, quiero ver cuántos tokens quedan disponibles de un jugador y a qué precio, para decidir una compra.

**Why this priority**: La pantalla de compra del frontend depende de este endpoint.

**Independent Test**: Sembrar un jugador con tokens en la tenencia del superusuario, invocar `GET /api/jugadores/{jugadorId}/mercado` y comprobar la respuesta.

**Acceptance Scenarios**:

1. **Given** un jugador existente, **When** se consulta su mercado, **Then** se responde `200` con `MercadoJugadorResponse`: `tokensEmitidos` (100), `tokensDisponibles` (tokens que todavía tiene el superusuario), `precioVigente` y `fechaCotizacion`.
2. **Given** que todavía no hubo recálculo de cotizaciones, **When** se consulta el mercado, **Then** `precioVigente` es `1` y `fechaCotizacion` es `null`.
3. **Given** un jugador inexistente, **When** se consulta su mercado, **Then** se responde `404`.

### User Story 4 - Cuentas, roles y superusuario (Priority: P1)

Como sistema, necesito que cada usuario tenga una cuenta con saldo y un rol, y que exista un único superusuario dueño inicial de todos los tokens.

**Why this priority**: Sin cuentas, roles y superusuario no hay contraparte ni saldo para operar.

**Independent Test**: Levantar la aplicación sobre una base limpia, registrar un usuario y hacer login con él y con el superusuario; comprobar saldos, tenencias iniciales y el rol en la respuesta y en el JWT.

**Acceptance Scenarios**:

1. **Given** una instalación limpia, **When** se aplican las migraciones y arranca la aplicación, **Then** existe un único usuario con rol `SUPERUSUARIO`, con credenciales tomadas de variables de entorno, que posee 100 tokens de cada jugador en `tenencias` y no tiene cuenta de créditos.
2. **Given** el superusuario creado, **When** hace login, **Then** recibe un JWT con rol `SUPERUSUARIO`.
3. **Given** un nuevo registro, **When** se crea el usuario, **Then** tiene rol `USUARIO` y una cuenta con el saldo inicial configurado.
4. **Given** usuarios registrados antes de esta feature, **When** se aplican las migraciones, **Then** cada uno tiene rol `USUARIO` y una cuenta con el saldo inicial configurado.
5. **Given** un login válido, **When** se emite el JWT, **Then** la respuesta `LoginResponse` incluye `rol` y el JWT incluye el claim del rol.
6. **Given** el superusuario autenticado, **When** llama a `POST /api/ordenes/compra` o `POST /api/ordenes/venta`, **Then** recibe `403` sin cambios persistidos.
7. **Given** un jugador nuevo creado por la sincronización del catálogo, **When** se persiste, **Then** el superusuario recibe 100 tokens de ese jugador.

### User Story 5 - Usar el mercado desde servicios reutilizables (Priority: P2)

Como feature consumidor, quiero invocar compra y venta como métodos de service sin construir una solicitud HTTP para habilitar la compra en un click y otros flujos internos.

**Why this priority**: Lo exige la constitución (sección 10) y evita duplicar reglas de negocio entre HTTP y flujos internos.

**Independent Test**: Invocar directamente los métodos de compra y venta con un `Clock` fijo y comprobar las mismas validaciones, resultados y registros que mediante los endpoints.

**Acceptance Scenarios**:

1. **Given** una llamada interna con datos válidos, **When** invoca el método de compra o venta, **Then** obtiene el resultado de la operación sin depender de Controller, HTTP ni DTOs de transporte.
2. **Given** una llamada interna con saldo, stock o tenencia insuficiente, **When** invoca el service, **Then** recibe la excepción de dominio correspondiente y no se persisten cambios parciales.
3. **Given** un `Clock` inyectado con una fecha fija (por ejemplo, el inicio del campeonato para la demo), **When** se completa una operación, **Then** la fecha de la transacción y de la auditoría es la del `Clock` y el precio es la cotización vigente a esa fecha.

## Edge Cases

- Jugador inexistente: `404` sin cambios. El jugador no tiene estado activo/inactivo; solo se valida su existencia.
- Usuario no autenticado o con JWT inválido: `401`.
- El superusuario no puede comprar ni vender por estos endpoints: se responde `403` ("El superusuario no puede operar en el mercado") sin cambios, porque operaría contra sí mismo.
- La cantidad debe ser un entero entre 1 y 100 (`OrdenRequest`); cero, negativos, fracciones, nulos o valores mayores a 100 se rechazan con `400`. `jugadorId` ausente o inválido también es `400`.
- El comprador o vendedor es siempre el usuario del JWT; el cuerpo de la orden no lleva usuario ni precio.
- El saldo de un usuario y las cantidades de tokens nunca quedan negativos.
- Una compra sin tenencia previa crea la tenencia con el precio de la operación como precio promedio.
- Una venta no modifica el precio promedio de la tenencia restante.
- El precio vigente se consulta en cada operación mediante `CotizacionVigentePort`; nunca se usa un precio enviado por el cliente.
- Si la cotización vigente no está disponible o es inválida (nula, cero o negativa), la operación se rechaza sin cambios.
- No hay idempotencia: cada solicitud válida es una orden independiente.
- Los registros de auditoría no se actualizan ni eliminan; una corrección genera un registro nuevo.

## Requirements *(mandatory)*

### Functional Requirements

**Cuentas, roles y superusuario**

- **FR-001**: El sistema MUST mantener una cuenta con saldo de créditos para cada usuario con rol `USUARIO`.
- **FR-002**: El saldo inicial MUST asignarse al crear la cuenta en el registro y ser configurable (`mercado.saldo-inicial`), con valor por defecto de 100 créditos.
- **FR-003**: Una migración MUST crear la cuenta con el saldo inicial para los usuarios existentes antes de esta feature.
- **FR-004**: Cada usuario MUST tener un rol `USUARIO` o `SUPERUSUARIO`; los usuarios registrados son siempre `USUARIO`.
- **FR-005**: El login MUST devolver el rol en `LoginResponse` y el JWT MUST incluir el rol como claim, según el contrato 005.
- **FR-006**: MUST existir un único superusuario: una fila de `usuarios` con rol `SUPERUSUARIO`, creada de forma idempotente al arrancar la aplicación. Puede loguearse y es la contraparte de todas las órdenes. En los perfiles `local` y `test` puede usar credenciales de desarrollo definidas en `application-local.yml` y `application-test.yml`; fuera de esos perfiles, `SUPERUSUARIO_EMAIL` y `SUPERUSUARIO_PASSWORD` son obligatorias y la aplicación MUST fallar al arrancar con un error explícito si falta cualquiera.
- **FR-007**: El superusuario NO tiene cuenta ni saldo de créditos: sus créditos no existen ni se registran. Las compras y ventas solo modifican el saldo del usuario que opera.
- **FR-007a**: El superusuario MUST recibir `403` si llama a `POST /api/ordenes/compra` o `POST /api/ordenes/venta`, porque operaría contra sí mismo. El service rechaza la operación con una excepción de dominio, así también se cubre la invocación interna.

**Tokens y tenencias**

- **FR-008**: Cada jugador MUST tener 100 tokens emitidos, guardados en `tenencias` a nombre del superusuario. Al arrancar, después de crear el superusuario, el sistema asigna de forma idempotente 100 tokens (precio promedio de referencia 1 crédito) a cada jugador que todavía no tenga tenencia del superusuario. No se hace por migración, porque las migraciones corren antes de que exista el superusuario.
- **FR-009**: Cada jugador creado después (por ejemplo, por la sincronización del catálogo) MUST recibir 100 tokens en la tenencia del superusuario en la misma transacción en que se crea.
- **FR-009a**: La sincronización del catálogo, que hoy borra los jugadores `LEGACY`, MUST respetar las FK del mercado: un jugador legacy sin transacciones se borra junto con su tenencia del superusuario (que es la única que puede tener), en la misma transacción; un jugador legacy con transacciones se conserva, porque sus operaciones y tenencias de usuarios forman parte del historial.
- **FR-009b**: La base MUST garantizar como máximo un usuario con rol `SUPERUSUARIO` mediante un índice único parcial.
- **FR-009c**: `UsuarioService` MUST tomar la fecha de registro del `Clock` inyectado (constitución, sección 9). La emisión y expiración del JWT y el timestamp de los errores quedan fuera de esta feature: no son services y dependen del reloj real.
- **FR-010**: El sistema MUST mantener una tenencia por usuario y jugador, con cantidad entera y precio promedio de adquisición. El stock disponible de un jugador es la tenencia del superusuario (no hay tabla de stock aparte).
- **FR-011**: Una tenencia vendida por completo MUST quedar con cantidad `0`, sin eliminarse.

**Órdenes**

- **FR-012**: El sistema MUST exponer únicamente `POST /api/ordenes/compra` y `POST /api/ordenes/venta`, protegidos por JWT, con cuerpo `OrdenRequest` (`jugadorId`, `cantidad` entera entre 1 y 100) y respuesta `201` con `OperacionResponse`, según el contrato 005; no debe agregar aliases `/orders/buy` ni `/orders/sell`.
- **FR-013**: El comprador o vendedor MUST ser el usuario del JWT; el cuerpo no acepta usuario ni precio.
- **FR-014**: El precio de cada operación MUST obtenerse mediante `CotizacionVigentePort`, que devuelve para un jugador y una fecha el precio vigente, la estrategia y la versión usadas y la fecha de cotización.
- **FR-015**: Mientras no esté la implementación de A, `CotizacionVigentePort` MUST devolver precio `1`, estrategia `null`, versión `null` y fecha de cotización `null`.
- **FR-016**: El total de la operación MUST calcularse como precio vigente por cantidad, sin usar montos enviados por el cliente.
- **FR-017**: Una compra MUST rechazarse con `422` si el saldo del usuario es menor al total, y con `409` si el superusuario no tiene tokens suficientes del jugador.
- **FR-018**: Una venta MUST rechazarse con `422` si el usuario no tiene la cantidad de tokens a vender.
- **FR-019**: Una orden con jugador inexistente MUST responder `404`; una orden con datos inválidos MUST responder `400`; sin JWT válido, `401`; del superusuario, `403`. Todos los errores usan el esquema `Error` del contrato.
- **FR-020**: Una compra confirmada MUST descontar el total del saldo del usuario y transferir los tokens de la tenencia del superusuario a la del usuario; una venta confirmada MUST acreditar el total al usuario y devolver los tokens a la tenencia del superusuario.
- **FR-021**: `OperacionResponse` MUST incluir `id`, `tipo` (`COMPRA`/`VENTA`), `jugador`, `cantidad`, `precioUnitario`, `total`, `saldoPosterior` del usuario, `contraparte`, `fecha`, `estrategia` y `version` (estos dos nulos si se operó al precio inicial).

**Datos de mercado**

- **FR-022**: El sistema MUST exponer `GET /api/jugadores/{jugadorId}/mercado` con `MercadoJugadorResponse`: `tokensEmitidos`, `tokensDisponibles` (tenencia del superusuario), `precioVigente` y `fechaCotizacion` (nula si no hubo recálculo), obtenidos por `CotizacionVigentePort`.

**Service reutilizable, tiempo y consistencia**

- **FR-023**: El service MUST exponer métodos de compra y venta que no dependan de HTTP; los controllers solo traducen HTTP y delegan en ellos (constitución, sección 10).
- **FR-024**: La fecha de las operaciones, transacciones y auditorías MUST obtenerse de un `java.time.Clock` inyectado; ningún service llama a `Instant.now()` ni `LocalDate.now()` (constitución, sección 9).
- **FR-025**: Cada operación MUST ser atómica: saldo, tenencias, transacción y auditoría se confirman o se revierten juntos.
- **FR-026**: El sistema MUST impedir condiciones de carrera que permitan gastar dos veces el saldo, vender dos veces los mismos tokens o comprar más tokens de los disponibles.
- **FR-027**: Precios, saldos y totales MUST manejarse como `BigDecimal` con 4 decimales (contrato 005) y redondeo `HALF_UP`; las cantidades de tokens son enteras.

**Transacciones y auditoría**

- **FR-028**: Cada compra o venta exitosa MUST registrar una transacción con tipo, usuario, jugador, cantidad, precio unitario, total, contraparte, fecha, estrategia y versión de la cotización.
- **FR-029**: El sistema MUST registrar una auditoría append-only por cada operación de mercado, con autor, operación y timestamp, y como estado anterior y posterior: el saldo del usuario, la cantidad de tokens del jugador del usuario y la cantidad de tokens del jugador del superusuario. Los créditos del superusuario no se auditan porque no existen.
- **FR-030**: La auditoría MUST ser de solo inserción: no se exponen ni se implementan operaciones de actualización o borrado sobre ella.

**Persistencia, mensajes y documentación**

- **FR-031**: Las migraciones de esta feature MUST ubicarse en el rango Flyway `V20` a `V29`.
- **FR-032**: Los mensajes de error MUST estar en español y no revelar datos de otros usuarios.
- **FR-033**: La documentación OpenAPI (Swagger) y la colección de Postman MUST incluir las operaciones de esta feature tal como están en el contrato 005.
- **FR-034**: B agrega en un PR al contrato 005 (rama `005-contrato-403-superusuario`) una respuesta específica `ErrorSuperusuarioNoOpera` (`403`, "El superusuario no puede operar en el mercado") a `POST /api/ordenes/compra`, `POST /api/ordenes/venta` y `GET /api/usuarios/{usuarioId}/portfolio`, aprobado por las tres integrantes. Se usa una respuesta propia en lugar de `ErrorProhibido`, que describe el acceso a recursos de otro usuario. En el portfolio, que ya tenía un `403` por recurso de otro usuario, el mismo `403` cubre ambos casos. El PR MUST estar mergeado antes de cerrar la feature.

### Key Entities

- **Usuario**: Cuenta autenticada con rol `USUARIO` o `SUPERUSUARIO`.
- **Superusuario**: Único usuario con rol `SUPERUSUARIO` (fila en `usuarios`); dueño inicial de todos los tokens (en `tenencias`), contraparte de todas las órdenes. No tiene cuenta ni créditos y no puede comprar ni vender.
- **Cuenta**: Saldo de créditos de un usuario con rol `USUARIO`.
- **Jugador**: Entidad del catálogo con 100 tokens emitidos.
- **Tenencia**: Usuario, jugador, cantidad entera y precio promedio. La tenencia del superusuario es el stock disponible.
- **Transacción**: Compra o venta confirmada con tipo, cantidad, precio unitario, total, contraparte, fecha, estrategia y versión.
- **Auditoría de mercado**: Registro append-only con autor, operación, timestamp y estado anterior y posterior (saldo del usuario, tokens del usuario y tokens del superusuario).
- **Cotización vigente**: Precio, estrategia, versión y fecha de cotización de un jugador a una fecha, obtenidos solo mediante `CotizacionVigentePort`.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las compras válidas actualiza saldo, tenencia del usuario, tenencia del superusuario, transacción y auditoría sin inconsistencias.
- **SC-002**: El 100% de las compras con saldo insuficiente (`422`) o stock insuficiente (`409`) se rechaza sin cambios persistidos.
- **SC-003**: El 100% de las ventas válidas acredita créditos, reduce la tenencia, devuelve los tokens al superusuario y registra transacción y auditoría.
- **SC-004**: El 100% de las ventas con tenencia insuficiente (`422`) se rechaza sin cambios persistidos.
- **SC-005**: El 100% de las operaciones usa el precio devuelto por `CotizacionVigentePort`; hasta la implementación de A, ese precio es 1 crédito.
- **SC-006**: Para cada jugador, la suma de tokens de todas las tenencias (incluido el superusuario) es siempre 100.
- **SC-007**: Ningún registro de auditoría puede modificarse o eliminarse, y cada operación exitosa deja autor, antes, después y timestamp.
- **SC-008**: Las respuestas de los cinco endpoints de esta feature coinciden con los esquemas y códigos del contrato 005 (incluido el `403` del superusuario, una vez mergeado el PR de FR-034).
- **SC-009**: Una instalación limpia deja el superusuario con 100 tokens de cada jugador y sin cuenta, y todos los usuarios con rol `USUARIO` con cuenta.
- **SC-010**: Las órdenes concurrentes nunca producen saldos o tenencias negativos ni transfieren más tokens que los disponibles, verificado con un test de integración de transacciones simultáneas sobre PostgreSQL.
- **SC-011**: Con un `Clock` fijo, las operaciones son reproducibles: misma fecha y mismo precio para la misma entrada.
- **SC-012**: El 100% de las órdenes del superusuario se rechaza con `403` sin cambios persistidos.
- **SC-013**: La sincronización del catálogo sigue funcionando después de esta feature: borra los jugadores legacy sin transacciones y conserva los que tienen.

## Assumptions

- El saldo inicial de cada usuario es de 100 créditos; cualquier parametrización operativa debe conservar ese valor como comportamiento por defecto y no cambia el contrato funcional.
- El usuario opera con el JWT existente; no se agrega otro mecanismo de autenticación.
- Las credenciales de desarrollo del superusuario solo existen en los perfiles `local` y `test`; ningún perfil fuera de ellos tiene valores por defecto para `SUPERUSUARIO_EMAIL` o `SUPERUSUARIO_PASSWORD`.
- La verificación de rol para las operaciones de administración (`403` de `ErrorSoloSuperusuario`) la usan las features de A; esta feature solo agrega el rol al usuario, al login y al JWT.
- El precio promedio de una tenencia es el promedio ponderado de las compras; una venta reduce la cantidad pero no altera el precio promedio.
- El cálculo dinámico de cotizaciones pertenece a la feature 006 (A); esta feature define el puerto y su implementación fija temporal.
- La compra en un click pertenece al feature extra (010); esta feature solo garantiza que puede reutilizar los métodos de service.
- El portfolio y el historial de transacciones (`/api/usuarios/{usuarioId}/portfolio` y `/transacciones`) pertenecen a la feature 009.
