# Especificación de feature: Portfolio e historial de operaciones

**Feature Branch**: `009-portfolio-historial`

**Created**: 2026-10-24

**Status**: Draft

**Input**: Portfolio actual, evolución histórica e historial de operaciones de un usuario,
según las operaciones marcadas con `x-feature: 009-portfolio-historial` en
`specs/005-contratos-entrega2/contracts/openapi.yaml`.

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
- Cotización vigente faltante o inválida para una posición: la operación no debe
  inventar un valor ni devolver una respuesta parcial; se informa el error mediante el
  mecanismo estándar de la aplicación.
- Usuario inexistente: `404` en los tres endpoints cuando corresponde; JWT ausente o
  inválido: `401`.
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
  MUST ser cero.
- **FR-007**: El sistema MUST exponer
  `GET /api/usuarios/{usuarioId}/portfolio/evolucion` con `desde` y `hasta`, y devolver
  `EvolucionPortfolioResponse` con un punto por fecha de cotización.
- **FR-008**: Cada punto de evolución MUST incluir `fecha`, `valor` e `invertido`. El
  valor MUST calcularse con las tenencias que el usuario tenía a esa fecha y la
  cotización histórica de cada jugador; el invertido MUST representar el costo de esas
  tenencias a esa fecha.
- **FR-009**: La evolución MUST consultar un `CotizacionHistoricaPort`, independiente
  de la persistencia concreta de cotizaciones. Hasta que A entregue la tabla de la
  feature 006, se MUST proveer una implementación provisoria explícita, reemplazable y
  limitada al perfil local/pruebas.
- **FR-010**: El sistema MUST exponer
  `GET /api/usuarios/{usuarioId}/transacciones` con `page`, `size`, `tipo` y
  `jugadorId`, devolviendo `TransaccionesPageResponse` con `OperacionResponse`.
- **FR-011**: Las transacciones MUST ordenarse por fecha descendente y, ante empate,
  por identificador descendente para que la paginación sea determinista.
- **FR-012**: Los filtros MUST combinarse (AND) y aplicarse en la consulta del usuario
  autenticado, no sobre resultados de otro usuario.
- **FR-013**: Solo el propio usuario MUST acceder a su portfolio, evolución e historial.
  Un identificador diferente al del JWT MUST producir `403`; el superusuario MUST
  producir `403` para portfolio y evolución y no MUST tener `Cuenta`.
- **FR-014**: El sistema MUST reutilizar `Cuenta`, `Tenencia` y `Transaccion` de la
  feature 007 sin duplicar entidades ni alterar las reglas del mercado.
- **FR-015**: `Clock` MUST ser inyectable para obtener fechas reproducibles en pruebas y
  para cargar el escenario 3 de la demo.
- **FR-016**: En el perfil `local`, al levantar la aplicación, el sistema MUST cargar
  de forma idempotente datos de prueba para cuatro usuarios que compren cinco jugadores
  al inicio del campeonato, usando una fecha fija del `Clock`. No MUST cargar esos
  datos en perfiles distintos de `local`.
- **FR-017**: Las migraciones Flyway de esta feature MUST estar contenidas en el rango
  V26 a V29 y ser compatibles con las migraciones V20 a V25 de la feature 007.

### Autorización y respuestas HTTP

Los endpoints MUST respetar las respuestas del contrato: `200` para consultas
exitosas, `400` para validación, `401` para autenticación ausente o inválida, `403`
para propietario incorrecto o superusuario, y `404` para usuario o jugador no
encontrado cuando corresponda. Una respuesta de error MUST usar el esquema `Error`
existente y no filtrar datos.

### Migraciones Flyway

El contenido esperado del rango es:

| Versión | Contenido |
|---|---|
| `V26__crear_cotizaciones_historicas.sql` | Estructura para fecha, jugador, precio y metadatos/versionado de cotizaciones históricas, con FK a `jugadores`, unicidad por `(jugador_id, fecha)` e índices para consultar por fecha. |
| `V27__indices_portfolio_historial.sql` | Índices de lectura para tenencias por usuario/cantidad y transacciones por usuario, fecha e identificadores usados por los filtros, sin cambiar el modelo de la feature 007. |
| `V28__datos_demo_portfolio_local.sql` | No debe contener datos productivos: si se incluye, debe ser seguro/idempotente y quedar restringido al perfil local mediante el mecanismo de inicialización de la aplicación. |
| `V29__ajustes_cotizaciones_historicas.sql` | Constraints, índices o ajustes finales necesarios para que `CotizacionHistoricaPort` soporte fechas de evolución; debe ser compatible con la futura tabla entregada por A. |

La carga de los cuatro usuarios, sus cinco jugadores y las compras de demostración
debe ejecutarse preferentemente como inicializador de perfil local después de que
existan usuarios, jugadores, cuentas y tenencias. No debe depender de que Flyway
conozca credenciales ni insertar duplicados en cada arranque.

### Key Entities

- **PortfolioResponse**: resumen valuado del usuario y sus posiciones positivas.
- **PosicionPortfolio**: costo, valuación y resultado de una tenencia individual.
- **EvolucionPortfolioResponse**: serie de puntos diarios/semanales, uno por fecha
  disponible de cotización dentro del intervalo.
- **CotizacionHistoricaPort**: puerto que obtiene cotizaciones por jugador y fecha;
  abstrae la tabla definitiva de la feature 006.
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
- **SC-006**: Las pruebas con `Clock.fixed` producen fechas y resultados repetibles,
  y las migraciones V26–V29 se aplican sin error sobre el esquema de 007.

## Assumptions

- La autenticación, `Usuario`, `Jugador`, `Cuenta`, `Tenencia`, `Transaccion` y
  `CotizacionVigentePort` ya existen o son provistas por las features precedentes.
- Los importes usan la representación contractual `Creditos` y la precisión monetaria
  vigente del mercado; no se agregan redondeos específicos a esta feature.
- La fecha de evolución se maneja como fecha de calendario, mientras que las
  transacciones conservan su instante original.
- La tabla definitiva de cotizaciones y su estrategia pertenecen a la feature 006 de
  A; el puerto y el adaptador provisorio de esta feature deben poder reemplazarse sin
  cambiar los controladores ni las respuestas OpenAPI.
- La demo local usa usuarios y datos sintéticos, no modifica el comportamiento de
  producción y puede reinicializarse de manera idempotente.
