# Feature Specification: Compra y venta de tokens de jugadores

**Feature Branch**: `007-mercado-tokens`

**Created**: 2026-10-24

**Status**: Draft

**Input**: User description: "Compra y venta de tokens de jugadores contra el superusuario. Cuenta con saldo en créditos por usuario; tenencia (usuario, jugador, cantidad, precio promedio); transaccion (tipo, cantidad, precio, contraparte, fecha); auditoría append-only inmutable (autor, antes, después, timestamp). Seed del superusuario con 100 tokens por jugador a 1 crédito. Endpoints POST /orders/buy y POST /orders/sell según el contrato 005. Validar saldo suficiente y tokens disponibles. El precio se obtiene por la interfaz CotizacionVigentePort, que devuelve 1 crédito fijo hasta que esté la estrategia de A. La compra y la venta son métodos de service reutilizables fuera de HTTP (para la compra en un click del feature extra). Clock inyectable para la fecha de las operaciones. Migraciones Flyway en el rango V20 a V29."

**Related features**: Reutiliza la autenticación y los usuarios de `specs/002-login-usuarios/`, y el catálogo de jugadores de `specs/003-paginacion-catalogo-jugadores/` y `specs/004-integracion-api-futbol-catalogo/`. La forma de las solicitudes y respuestas HTTP respeta el contrato 005.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Comprar tokens de un jugador (Priority: P1)

Como usuario autenticado, quiero comprar tokens de un jugador al precio vigente para aumentar mi tenencia y que los créditos y tokens se actualicen de forma consistente.

**Why this priority**: La compra es el flujo principal del mercado y habilita la inversión de usuarios en jugadores.

**Independent Test**: Sembrar un jugador, saldo suficiente y tokens disponibles en la cuenta del superusuario; invocar el método de service o `POST /orders/buy` y comprobar saldo, tenencia, stock del superusuario, transacción y auditoría.

**Acceptance Scenarios**:

1. **Given** un usuario autenticado con saldo suficiente y el superusuario con tokens disponibles, **When** solicita comprar una cantidad positiva de tokens de un jugador, **Then** se ejecuta la operación al precio vigente de 1 crédito, se descuentan los créditos, se incrementa su tenencia y se transfieren los tokens desde el superusuario.
2. **Given** un usuario que ya posee tokens del jugador, **When** compra más tokens al mismo precio vigente, **Then** la tenencia se acumula y el precio promedio queda calculado sobre la cantidad total adquirida.
3. **Given** una compra confirmada, **When** se consulta la operación, **Then** existe una transacción de tipo compra con cantidad, precio, contraparte y fecha, junto con un registro de auditoría inmutable del estado anterior y posterior.
4. **Given** una solicitud de compra con JWT válido, **When** se procesa por HTTP, **Then** la respuesta y los códigos de error siguen el contrato 005.

### User Story 2 - Vender tokens al superusuario (Priority: P1)

Como usuario autenticado, quiero vender tokens que poseo al superusuario al precio vigente para recibir créditos y reducir mi tenencia.

**Why this priority**: La venta completa el intercambio contra el superusuario y permite recuperar el saldo de la inversión.

**Independent Test**: Crear una tenencia suficiente para un usuario; invocar el método de service o `POST /orders/sell` y comprobar el aumento del saldo, la reducción de la tenencia, el stock del superusuario, la transacción y la auditoría.

**Acceptance Scenarios**:

1. **Given** un usuario autenticado con cantidad suficiente de tokens de un jugador, **When** solicita vender una cantidad positiva, **Then** se ejecuta al precio vigente de 1 crédito, se acredita el saldo, se reduce su tenencia y los tokens pasan al superusuario.
2. **Given** una solicitud de venta por una cantidad mayor a la tenencia del usuario, **When** se procesa, **Then** se rechaza sin modificar saldo, tenencia, stock, transacciones ni auditorías.
3. **Given** una venta confirmada, **When** se consulta la operación, **Then** existe una transacción de tipo venta con el superusuario como contraparte, la fecha provista por el reloj de la operación y su auditoría correspondiente.
4. **Given** una solicitud de venta con JWT válido, **When** se procesa por HTTP, **Then** la respuesta y los códigos de error siguen el contrato 005.

### User Story 3 - Mantener saldo, tenencias y stock consistentes (Priority: P1)

Como operador del mercado, quiero que cada operación mantenga invariantes de saldo y tokens para evitar creación o pérdida accidental de valor.

**Why this priority**: La consistencia contable es necesaria para confiar en las operaciones y en el estado del mercado.

**Independent Test**: Ejecutar compras y ventas exitosas y fallidas, incluyendo solicitudes concurrentes, y verificar que cada operación confirma todos sus cambios o no deja ninguno.

**Acceptance Scenarios**:

1. **Given** un usuario con saldo insuficiente para la compra, **When** solicita tokens, **Then** se rechaza antes de modificar cualquier entidad.
2. **Given** el superusuario sin tokens suficientes del jugador, **When** un usuario intenta comprar, **Then** se rechaza antes de modificar cualquier entidad.
3. **Given** una cantidad cero, negativa, fraccionaria o ausente, **When** se envía una orden, **Then** se rechaza como entrada inválida y no se ejecuta la operación.
4. **Given** una operación aceptada, **When** se persisten sus cambios, **Then** saldo, tenencias, stock, transacción y auditoría quedan confirmados juntos; ante un fallo, ninguno de esos cambios queda aplicado.
5. **Given** dos órdenes que compiten por el mismo saldo o stock, **When** se procesan, **Then** el resultado no permite saldo o tokens negativos ni superar el stock disponible.

### User Story 4 - Usar el mercado desde servicios reutilizables (Priority: P2)

Como feature consumidor, quiero invocar compra y venta como métodos de service sin construir una solicitud HTTP para habilitar la compra en un click y otros flujos internos.

**Why this priority**: Evita duplicar reglas de negocio y garantiza que los accesos HTTP e internos tengan el mismo comportamiento.

**Independent Test**: Invocar directamente los métodos de compra y venta con un reloj controlado y comprobar las mismas validaciones, resultados y registros que mediante los endpoints.

**Acceptance Scenarios**:

1. **Given** una llamada interna con datos válidos, **When** invoca el método de compra o venta, **Then** obtiene el resultado de la operación sin depender de Controller, HTTP ni objetos de transporte HTTP.
2. **Given** una llamada interna con saldo, stock o tenencia insuficiente, **When** invoca el service, **Then** recibe el error de dominio/servicio correspondiente y no se persisten cambios parciales.
3. **Given** un reloj inyectado que devuelve una fecha conocida, **When** se completa una operación, **Then** la fecha de la transacción y de la auditoría usa ese valor sin depender directamente de la hora del sistema.

## Edge Cases

- El jugador inexistente, inactivo o no disponible para el mercado debe rechazarse sin modificar el estado.
- El usuario no autenticado o inexistente no puede comprar ni vender.
- El superusuario no puede operar como usuario comprador o vendedor mediante estos endpoints.
- La cantidad debe ser un entero positivo; no se aceptan cero, negativos, fracciones, nulos ni desbordes numéricos.
- El saldo y las cantidades de tokens no pueden quedar negativos.
- Una compra sin tenencia previa crea una tenencia con el precio de la operación como precio promedio.
- Una venta que deja la cantidad en cero elimina la tenencia o la deja explícitamente en cero, según el modelo persistente definido para la implementación, sin conservar saldo de tokens utilizable.
- El precio vigente se consulta para cada operación; una operación no reutiliza un precio recibido por el cliente.
- Si la cotización vigente no está disponible o devuelve un valor inválido, la operación se rechaza sin cambios.
- Los identificadores de actor, jugador y contraparte deben corresponder a entidades existentes y no pueden sustituirse por valores enviados para alterar la contabilidad.
- Reintentos de una solicitud no deben registrar una operación adicional si el contrato 005 proporciona un identificador de idempotencia; si dicho identificador no forma parte del contrato, cada solicitud válida se considera una orden independiente.
- Los registros de auditoría no se actualizan ni eliminan, incluso cuando una operación posterior cambia el estado auditado.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST mantener un saldo de créditos para cada usuario habilitado para operar.
- **FR-002**: El sistema MUST mantener una tenencia identificada por usuario y jugador, con cantidad y precio promedio de adquisición.
- **FR-003**: El sistema MUST mantener transacciones con tipo (compra o venta), cantidad, precio, contraparte y fecha.
- **FR-004**: El sistema MUST crear y mantener un superusuario como contraparte del mercado.
- **FR-005**: El seed inicial MUST asignar al superusuario 100 tokens de cada jugador disponible, a un precio de referencia de 1 crédito por token.
- **FR-006**: El sistema MUST exponer `POST /orders/buy` y `POST /orders/sell` protegidos por autenticación, respetando el contrato 005 para request, response, estados HTTP y errores.
- **FR-007**: El endpoint de compra MUST identificar el jugador y la cantidad solicitada según el contrato 005, sin aceptar un precio definido por el cliente.
- **FR-008**: El endpoint de venta MUST identificar el jugador y la cantidad solicitada según el contrato 005, sin aceptar un precio definido por el cliente.
- **FR-009**: El sistema MUST obtener el precio de cada operación mediante `CotizacionVigentePort`.
- **FR-010**: Mientras no esté disponible la estrategia A, `CotizacionVigentePort` MUST devolver un precio vigente fijo de 1 crédito por token.
- **FR-011**: El sistema MUST rechazar una compra cuando el saldo de créditos del usuario sea menor al costo total.
- **FR-012**: El sistema MUST rechazar una compra cuando el superusuario no tenga tokens suficientes del jugador.
- **FR-013**: El sistema MUST rechazar una venta cuando el usuario no tenga tokens suficientes del jugador.
- **FR-014**: El sistema MUST aplicar el precio vigente a la cantidad solicitada y calcular el costo o crédito total sin usar valores monetarios provistos por el cliente.
- **FR-015**: Una compra confirmada MUST transferir créditos del usuario al superusuario y tokens del superusuario al usuario.
- **FR-016**: Una venta confirmada MUST transferir tokens del usuario al superusuario y créditos del superusuario al usuario.
- **FR-017**: El service MUST exponer operaciones reutilizables de compra y venta que no dependan de HTTP, y los endpoints MUST delegar en esos métodos.
- **FR-018**: El service MUST validar existencia de usuario, jugador y contraparte, y el modelo MUST preservar sus invariantes de saldo, tenencia y stock.
- **FR-019**: Cada compra o venta exitosa MUST registrar una transacción con su contraparte, precio aplicado y fecha de operación.
- **FR-020**: El sistema MUST registrar auditoría append-only e inmutable para los cambios de mercado, con autor, estado antes, estado después y timestamp.
- **FR-021**: La auditoría MUST impedir actualizaciones y eliminaciones posteriores; cualquier corrección debe generar un nuevo registro, no alterar el anterior.
- **FR-022**: El timestamp de las transacciones y auditorías MUST obtenerse mediante un `Clock` inyectable para permitir operaciones reproducibles y testeables.
- **FR-023**: Una operación MUST ser atómica: saldo, tenencias, stock, transacción y auditoría se confirman juntos o se revierten juntos.
- **FR-024**: El sistema MUST impedir estados negativos y condiciones de carrera que permitan gastar dos veces el saldo o vender dos veces los mismos tokens.
- **FR-025**: Las migraciones de persistencia de esta feature MUST ubicarse dentro del rango Flyway `V20` a `V29`, sin reutilizar versiones ya aplicadas.
- **FR-026**: Los mensajes funcionales y de error expuestos por la feature MUST estar en español y no deben revelar datos de otros usuarios.
- **FR-027**: La documentación OpenAPI y la colección de Postman MUST describir y permitir ejecutar ambos endpoints de acuerdo con el contrato 005.

### Key Entities

- **Usuario**: Cuenta autenticada que posee saldo de créditos y puede mantener tenencias y operar contra el superusuario.
- **Superusuario**: Cuenta especial propietaria inicial de los tokens y contraparte de las compras y ventas del mercado.
- **Jugador**: Entidad del catálogo para la que existe una cantidad limitada de tokens negociables.
- **Saldo de créditos**: Cantidad de créditos disponible para un usuario o para el superusuario.
- **Tenencia**: Relación entre usuario y jugador que registra la cantidad de tokens poseída y su precio promedio.
- **Transacción**: Registro de una compra o venta confirmada, con tipo, cantidad, precio, contraparte y fecha.
- **Auditoría de mercado**: Registro append-only que conserva autor, estado anterior, estado posterior y timestamp de cada cambio relevante.
- **Cotización vigente**: Precio aplicable a una operación, obtenido exclusivamente mediante `CotizacionVigentePort`.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de las compras válidas con saldo y stock suficientes actualiza saldo, tenencia, stock, transacción y auditoría sin inconsistencias.
- **SC-002**: El 100% de las compras con saldo insuficiente o stock insuficiente es rechazado sin cambios persistidos.
- **SC-003**: El 100% de las ventas válidas con tenencia suficiente acredita créditos, reduce la tenencia, actualiza el stock y registra transacción y auditoría.
- **SC-004**: El 100% de las ventas con tenencia insuficiente es rechazado sin cambios persistidos.
- **SC-005**: El 100% de las operaciones utiliza el precio devuelto por `CotizacionVigentePort`; hasta la estrategia A, ese precio es 1 crédito por token.
- **SC-006**: El 100% de las transacciones confirmadas tiene exactamente una contraparte, una cantidad positiva, un precio aplicado y una fecha provista por el reloj de la operación.
- **SC-007**: Ningún registro de auditoría previamente creado puede ser modificado o eliminado, y cada cambio exitoso de mercado deja autor, antes, después y timestamp.
- **SC-008**: Las reglas ejecutadas por `POST /orders/buy` y `POST /orders/sell` son las mismas que las ejecutadas por los métodos reutilizables del service.
- **SC-009**: Las migraciones necesarias para la feature se encuentran dentro de `V20`-`V29` y una instalación limpia deja disponible el seed inicial del superusuario.
- **SC-010**: Las órdenes concurrentes nunca producen saldos, tenencias o stocks negativos ni permiten transferir más tokens que los disponibles.

## Assumptions

- El usuario opera con el JWT existente; no se agrega una credencial M2M ni un mecanismo de autorización separado.
- “Según el contrato 005” significa que no se modifica el contrato externo de request, response, autenticación y códigos HTTP; cualquier campo adicional interno no se expone por los endpoints.
- El superusuario es una identidad técnica/administrativa sembrada y no se ofrece como actor seleccionable por el cliente.
- El precio es expresado en créditos por token y la cantidad negociada es entera.
- El precio promedio de una tenencia se calcula sobre las compras acumuladas; una venta reduce cantidad, pero no altera retroactivamente el precio de las compras ya registradas.
- El alcance de esta feature no incluye la estrategia A ni el cálculo dinámico de cotizaciones; solo define el puerto y su valor fijo temporal.
- La compra en un click pertenece a otro feature consumidor; esta entrega solo garantiza que puede reutilizar los métodos de service.
- El rango `V20`-`V29` se reserva para las migraciones de esta feature y se asignan versiones concretas durante la planificación, sin crear migraciones fuera del rango.
- La auditoría de mercado conserva los estados relevantes de las entidades afectadas en formato consultable; no se permite editar ni borrar eventos históricos.
