# Especificación de feature: Portfolio e historial de operaciones

**Feature Branch**: `009-portfolio-historial`

**Created**: 2026-10-24

**Status**: Draft

**Input**: Portfolio actual, evolución histórica e historial de operaciones de un usuario,
según las operaciones marcadas con `x-feature: 009-portfolio-historial` en
`specs/005-contratos-entrega2/contracts/openapi.yaml`.

## Clarifications

### Session 2026-10-25

- Q: ¿Esta feature crea una tabla de cotizaciones históricas? → A: No. La tabla `cotizacion` es de A (feature 006). La evolución lee las cotizaciones solo mediante `CotizacionHistoricaPort`, que A implementa en la feature 008 con la firma acordada.
- Q: ¿Qué pasa con la evolución hasta que A entregue la 008? → A: Se usa `CotizacionHistoricaFijaAdapter`, registrado en todos los perfiles: un punto por semana (los lunes) y precio 1 para todos los jugadores. Se borra cuando se mergea la 008.
- Q: ¿Cómo se calculan las tenencias e invertido a una fecha pasada? → A: Recorriendo las transacciones del usuario con fecha menor o igual a ese día, con la misma regla de precio promedio ponderado del mercado. Los precios son los de la estrategia activa.
- Q: ¿Qué redondeo usan los cálculos? → A: Montos con 4 decimales y porcentajes con 2, siempre `HALF_UP`.
- Q: ¿Qué se responde si el `{usuarioId}` no es el del JWT? → A: `403`, antes de verificar si ese usuario existe, para no revelar qué identificadores existen.
- Q: ¿Qué valores toman `desde` y `hasta` si no se envían? → A: Son opcionales en el contrato. `hasta` es la fecha del `Clock`; `desde` es la fecha de la primera transacción del usuario, o `hasta` si no tiene transacciones.
- Q: ¿Qué zona horaria se usa para convertir `Transaccion.fecha` (`TIMESTAMPTZ`) a fecha calendario en la evolución? → A: UTC.
- Q: ¿Qué ocurre si el filtro `jugadorId` del historial no existe? → A: Se responde `200` con una página vacía; el filtro no valida la existencia del jugador.

## User Scenarios & Testing

### User Story 1 - Consultar el portfolio actual (Priority: P1)

Como usuario autenticado quiero ver mi saldo, el costo de mis tenencias y su
valuación actual para conocer el estado de mi inversión.

**Why this priority**: Es la vista principal que permite saber cuánto dinero está
disponible, cuánto se invirtió y cuánto valen actualmente los tokens.

**Independent Test**: Con un usuario que tenga saldo y al menos una tenencia positiva,
consultar su portfolio y verificar los totales, las posiciones y la valuación con la
cotización vigente.

**Acceptance Scenarios**:

1. **Given** un usuario `USUARIO` autenticado con tenencias de dos jugadores, **When**
   solicita `GET /api/usuarios/{usuarioId}/portfolio`, **Then** recibe `200` con
   `PortfolioResponse`, una posición por cada jugador cuya cantidad sea mayor que cero,
   y `fechaValuacion` correspondiente a la fecha del `Clock`.
2. **Given** una tenencia con cantidad cero, **When** se consulta el portfolio, **Then**
   el jugador no aparece en `posiciones`, aunque la fila de `Tenencia` se conserve para
   el historial del mercado.
3. **Given** un usuario autenticado cuyo JWT no corresponde al `{usuarioId}`, **When**
   solicita el portfolio, **Then** recibe `403` sin revelar datos del usuario indicado.
4. **Given** el JWT del superusuario, **When** solicita cualquier portfolio, **Then**
   recibe `403`, porque el superusuario no tiene `Cuenta` ni portfolio.

### User Story 2 - Consultar la evolución del portfolio (Priority: P2)

Como usuario quiero comparar, por fecha de cotización, el valor de mis tenencias con
el capital invertido para visualizar la evolución del portfolio.

**Why this priority**: Permite explicar la ganancia en el tiempo y alimenta el gráfico
principal de la aplicación.

**Independent Test**: Con operaciones y cotizaciones históricas de varias fechas,
consultar un intervalo y verificar que se devuelve un punto por cada fecha, con
`valor` e `invertido`.

**Acceptance Scenarios**:

1. **Given** un usuario propio y un intervalo `desde`/`hasta` válido, **When** solicita
   `GET /api/usuarios/{usuarioId}/portfolio/evolucion`, **Then** recibe `200` con
   `EvolucionPortfolioResponse` y un punto por cada fecha de cotización del intervalo.
2. **Given** un intervalo sin cotizaciones, **When** se solicita la evolución, **Then**
   recibe `200` con `puntos` vacío.
3. **Given** `desde` posterior a `hasta` o una fecha inválida, **When** se solicita la
   evolución, **Then** recibe `400`.
4. **Given** un usuario autenticado que no es el propietario, **When** solicita la
   evolución, **Then** recibe `403`; el superusuario también recibe `403`.

### User Story 3 - Consultar el historial de operaciones (Priority: P1)

Como usuario quiero consultar mis compras y ventas paginadas, ordenadas desde la más
nueva a la más vieja, para auditar mis movimientos.

**Why this priority**: El historial es necesario para revisar cada operación y es la
fuente de trazabilidad de las posiciones.

**Independent Test**: Con varias transacciones de compra y venta, consultar páginas
sucesivas y aplicar los filtros `tipo` y `jugadorId`, verificando que solo se muestran
operaciones del usuario autenticado.

**Acceptance Scenarios**:

1. **Given** transacciones propias, **When** se solicita
   `GET /api/usuarios/{usuarioId}/transacciones`, **Then** recibe `200` con
   `TransaccionesPageResponse` y contenido ordenado por `fecha` descendente.
2. **Given** el filtro `tipo=COMPRA` o `tipo=VENTA`, **When** se consulta el historial,
   **Then** todas las operaciones devueltas tienen ese tipo.
3. **Given** `jugadorId`, `page` y `size` válidos, **When** se consulta el historial,
   **Then** se aplica el filtro del jugador y se devuelve la página solicitada,
   posiblemente vacía.
4. **Given** un `{usuarioId}` distinto al del JWT, **When** se consulta el historial,
   **Then** recibe `403` y no se devuelven transacciones.

## Edge Cases

- Portfolio sin tenencias positivas: `200`, `posiciones` vacío, `valorTenencias` e
  `invertido` iguales a cero y `ganancia` igual a cero.
- Portfolio con `invertido` igual a cero: `gananciaPorcentaje` se informa como cero,
  evitando división por cero.
- Ganancia negativa: se conserva el signo negativo tanto en `ganancia` como en el
  porcentaje.
- Jugador sin cotización: `CotizacionVigentePort` y `CotizacionHistoricaPort` devuelven
  precio 1, igual que en el mercado; no hay valuaciones faltantes.
- `{usuarioId}` distinto al del JWT: `403` sin verificar si ese usuario existe. `404`
  solo aplica si el usuario del propio JWT ya no existe. JWT ausente o inválido: `401`.
- `desde` y `hasta` ausentes: `hasta` es la fecha del `Clock` y `desde` la de la primera
  transacción del usuario (o `hasta` si no tiene transacciones).
- Ventas en el pasado: al reconstruir una fecha, una venta reduce la cantidad sin
  cambiar el precio promedio, igual que en el mercado.
- Parámetros `page`, `size`, `tipo`, `jugadorId`, `desde` y `hasta` inválidos: `400`.
- No se exponen transacciones de otro usuario aun cuando el solicitante conozca su
  identificador.
- La respuesta nunca incluye una posición con cantidad negativa o cero.

## Requirements

### Functional Requirements

- **FR-001**: El sistema MUST exponer `GET /api/usuarios/{usuarioId}/portfolio` conforme
  al contrato OpenAPI y autenticar la solicitud con el JWT existente.
- **FR-002**: El sistema MUST devolver `PortfolioResponse` con `usuarioId`,
  `fechaValuacion`, `saldo`, `invertido`, `valorTenencias`, `ganancia`,
  `gananciaPorcentaje` y `posiciones`.
- **FR-003**: Cada `PosicionPortfolio` MUST incluir jugador, cantidad, precio promedio,
  precio actual, invertido, valor actual, ganancia y ganancia porcentual.
- **FR-004**: `saldo` MUST provenir de `Cuenta`; `invertido` MUST ser la suma del costo
  de las tenencias positivas (`cantidad * precioPromedio`); y `valorTenencias` MUST
  ser la suma de `cantidad * precioActual`.
- **FR-005**: La valuación actual MUST obtener `precioActual` mediante
  `CotizacionVigentePort` para cada jugador y usar una única fecha de valuación tomada
  del `Clock` inyectable.
- **FR-006**: `ganancia` MUST ser `valorTenencias - invertido`; cuando `invertido` sea
  positivo, `gananciaPorcentaje` MUST ser `ganancia / invertido * 100`, y en otro caso
  MUST ser cero. Lo mismo aplica a cada posición.
- **FR-006a**: Los montos (`saldo`, `invertido`, `valorTenencias`, `ganancia`, precios y
  valores) MUST usar `BigDecimal` con 4 decimales y los porcentajes 2 decimales, siempre
  con redondeo `HALF_UP`.
- **FR-007**: El sistema MUST exponer
  `GET /api/usuarios/{usuarioId}/portfolio/evolucion` con `desde` y `hasta`, y devolver
  `EvolucionPortfolioResponse` con un punto por fecha de cotización.
- **FR-008**: Cada punto de evolución MUST incluir `fecha`, `valor` e `invertido`. Las
  tenencias del usuario a esa fecha MUST reconstruirse recorriendo sus transacciones con
  fecha menor o igual a ese día (una compra suma cantidad y recalcula el precio promedio
  ponderado; una venta resta cantidad sin cambiarlo). `invertido` es la suma de
  `cantidad * precioPromedio` y `valor` la suma de `cantidad * precio` con el precio de
  `CotizacionHistoricaPort` a esa fecha, de la estrategia activa.
- **FR-008a**: `desde` y `hasta` son opcionales: `hasta` por defecto es la fecha del
  `Clock` y `desde` la de la primera transacción del usuario (o `hasta` si no tiene).
- **FR-009**: La evolución MUST obtener fechas y precios solo mediante
  `CotizacionHistoricaPort`, en el paquete `com.formula11.service`, con esta firma,
  acordada con A, que lo implementa en la feature 008:

  ```java
  public interface CotizacionHistoricaPort {
      // Fechas de cotización de la estrategia activa dentro de [desde, hasta], ascendentes y sin repetir.
      List<LocalDate> fechasCotizacion(LocalDate desde, LocalDate hasta);

      // Precio de cada jugador vigente a esa fecha: último registro de la estrategia activa con fecha <= fecha.
      // Un jugador sin cotización a esa fecha vale 1 crédito. El mapa trae una entrada por cada id pedido.
      Map<Long, BigDecimal> preciosA(Collection<Long> jugadorIds, LocalDate fecha);
  }
  ```

  Esta feature NO crea tablas de cotizaciones ni lee la tabla `cotizacion` de A.
- **FR-009a**: Hasta que se mergee la 008, MUST existir `CotizacionHistoricaFijaAdapter`,
  registrado en todos los perfiles (si faltara, la aplicación no arrancaría): devuelve un
  lunes por semana dentro del intervalo y precio 1 para cada jugador. Quien mergee
  segunda entre la 008 y la 009 MUST borrarlo, para que exista una sola implementación.
- **FR-010**: El sistema MUST exponer
  `GET /api/usuarios/{usuarioId}/transacciones` con `page`, `size`, `tipo` y
  `jugadorId`, devolviendo `TransaccionesPageResponse` con `OperacionResponse`.
- **FR-011**: Las transacciones MUST ordenarse por fecha descendente y, ante empate,
  por identificador descendente para que la paginación sea determinista.
- **FR-012**: Los filtros MUST combinarse (AND) y aplicarse en la consulta del usuario
  autenticado, no sobre resultados de otro usuario.
- **FR-013**: Solo el propio usuario MUST acceder a su portfolio, evolución e historial.
  Un identificador diferente al del JWT MUST producir `403` antes de verificar si ese
  usuario existe. El superusuario MUST recibir `403` en los tres endpoints, porque no
  tiene `Cuenta`, portfolio ni operaciones propias.
- **FR-014**: El sistema MUST reutilizar `Cuenta`, `Tenencia` y `Transaccion` de la
  feature 007 sin duplicar entidades ni alterar las reglas del mercado.
- **FR-015**: `Clock` MUST ser inyectable para obtener fechas reproducibles en pruebas y
  para cargar el escenario 3 de la demo.
- **FR-016**: En el perfil `local`, al levantar la aplicación, un inicializador
  (`ApplicationRunner` activo solo en `local`, que corre después de
  `SuperusuarioInitializer`) MUST cargar de forma idempotente cuatro usuarios de demo
  que compran cinco jugadores al inicio del campeonato. Las compras MUST hacerse con
  las reglas de `MercadoService` (validaciones, transacción y auditoría) y con un
  `Clock` fijo en la fecha de inicio del campeonato, configurable en
  `application-local.yml`. Si los usuarios de demo ya existen, no se carga nada. No MUST
  cargarse en otros perfiles ni mediante Flyway.
- **FR-017**: Esta feature no necesita migraciones nuevas. Si el plan detecta que hace
  falta un índice de lectura, MUST usar la `V26`; `V27` a `V29` quedan libres dentro
  del rango de B.

### Autorización y respuestas HTTP

Los endpoints MUST respetar las respuestas del contrato: `200` para consultas
exitosas, `400` para validación, `401` para autenticación ausente o inválida, `403`
para propietario incorrecto o superusuario, y `404` para usuario no encontrado
cuando corresponda. Un `jugadorId` inexistente usado como filtro del historial
produce `200` con una página vacía. Una respuesta de error MUST usar el esquema
`Error` existente y no filtrar datos.

### Migraciones Flyway

Esta feature no crea tablas: reutiliza `cuentas`, `tenencias` y `transacciones` de la
007, y las cotizaciones son de A. La 007 ya creó el índice
`ix_transacciones_usuario_fecha (usuario_id, fecha DESC)`, que cubre el historial. Solo
si el plan detecta otra consulta lenta se agrega un índice en `V26`.

### Key Entities

- **PortfolioResponse**: resumen valuado del usuario y sus posiciones positivas.
- **PosicionPortfolio**: costo, valuación y resultado de una tenencia individual.
- **EvolucionPortfolioResponse**: serie de puntos diarios/semanales, uno por fecha
  disponible de cotización dentro del intervalo.
- **CotizacionHistoricaPort**: puerto que devuelve las fechas de cotización y los
  precios de varios jugadores a una fecha. Lo implementa A en la 008; mientras tanto,
  `CotizacionHistoricaFijaAdapter`.
- **Cuenta**: saldo disponible del usuario, reutilizada desde 007.
- **Tenencia**: cantidad y precio promedio por usuario y jugador, reutilizada desde 007.
- **Transaccion**: compra o venta confirmada, reutilizada desde 007 y fuente del
  historial.
- **OperacionResponse**: representación contractual de una `Transaccion`.
- **Clock**: dependencia temporal inyectable para valuación, operaciones y demo.

## Success Criteria

### Measurable Outcomes

- **SC-001**: El endpoint de portfolio devuelve todos los campos obligatorios del
  contrato y exactamente una posición por jugador con cantidad positiva.
- **SC-002**: El historial devuelve páginas deterministas, ordenadas de más nueva a
  más vieja, y los filtros no incluyen operaciones que no coincidan.
- **SC-003**: Una consulta de evolución devuelve exactamente un punto por cada fecha
  de cotización disponible dentro del intervalo, incluyendo correctamente el caso
  de serie vacía.
- **SC-004**: El 100% de las solicitudes con JWT de otro usuario o superusuario son
  rechazadas con `403` y sin datos en el cuerpo de negocio.
- **SC-005**: Levantar dos veces con perfil `local` deja los mismos cuatro usuarios,
  cinco jugadores y operaciones de demo, sin duplicados ni cambios acumulativos.
- **SC-006**: Las pruebas con `Clock.fixed` producen fechas y resultados repetibles.
- **SC-007**: Reemplazar `CotizacionHistoricaFijaAdapter` por la implementación de A no
  requiere cambiar controllers, services ni respuestas.

## Assumptions

- La autenticación, `Usuario`, `Jugador`, `Cuenta`, `Tenencia`, `Transaccion` y
  `CotizacionVigentePort` ya existen o son provistas por las features precedentes.
- Los importes usan la representación contractual `Creditos` con 4 decimales, como el
  mercado; los porcentajes, 2 decimales (FR-006a).
- La fecha de evolución se maneja como fecha de calendario UTC, mientras que las
  transacciones conservan su instante original.
- La tabla de cotizaciones y sus estrategias pertenecen a A (006); la implementación
  real de `CotizacionHistoricaPort` llega con la 008.
- La demo local usa usuarios y datos sintéticos, no modifica el comportamiento de
  producción y puede reinicializarse de manera idempotente.
