# Feature Specification: Detalle de jugador, historial, cotización a una fecha y ranking

**Feature Branch**: `008-cotizacion-fecha-ranking`

**Created**: 2026-10-28

**Status**: Draft

**Input**: User description: "Detalle de jugador con su cotización actual, historial público de cotizaciones, cotización de un jugador a una fecha, ranking por estrategia e implementación real de `CotizacionHistoricaPort` para la evolución del portfolio"

**Related contract**: [Contrato de la Entrega 2](../005-contratos-entrega2/contracts/openapi.yaml). Esta feature implementa únicamente las operaciones que tienen `x-feature: 008-cotizacion-fecha-ranking`.

## Scope

Esta feature expone en modo lectura las cotizaciones semanales que calcula y persiste la feature `006-estrategias-cotizacion`: el detalle de un jugador con su cotización actual, el historial de cotizaciones de un jugador, la cotización vigente a una fecha y el ranking de jugadores por score. Además implementa `CotizacionHistoricaPort`, el puerto acordado con la feature `009-portfolio-historial` para valuar la evolución del portfolio, y elimina `CotizacionHistoricaFijaAdapter`.

No implementa:

- El cálculo, el recálculo ni la persistencia de cotizaciones, que pertenecen a la feature `006`.
- La carga de métricas desde WhoScored, que pertenece a la feature `011`.
- El portfolio, su evolución ni el historial de operaciones, que pertenecen a la feature `009`; esta feature solo provee el puerto que la `009` consume.
- El detector de subvaluados, que pertenece a la feature `010`.
- Frontend.

No se crean tablas nuevas. La única migración es un índice de lectura sobre `cotizacion`.

## Clarifications

### Session 2026-10-28

- Q: ¿Qué fecha usa el ranking como "última cotización"? → A: La fecha máxima de cotización de la estrategia consultada. Todas las cotizaciones del ranking son de esa misma fecha.
- Q: ¿Cómo se desempata el ranking y cómo se numera `puesto`? → A: Score descendente, luego precio descendente, nombre ascendente e id ascendente. `puesto` es la posición dentro del resultado ya filtrado por liga y posición: `page * size + índice + 1`.
- Q: ¿Contra qué se calcula la `variacion` de una cotización? → A: Contra la cotización inmediatamente anterior del mismo jugador y la misma estrategia, aunque quede fuera del rango pedido. Es `(precio / precioAnterior - 1) * 100` con 2 decimales `HALF_UP`, y es `null` si no existe cotización anterior. No se persiste: se calcula al leer.
- Q: ¿Qué es "hoy" para la cotización vigente y el detalle? → A: La fecha de negocio: `Clock` (UTC) convertido con `ClockConfig.ZONA_NEGOCIO`, igual que en la `006`.
- Q: ¿Qué pasa si un `USUARIO` envía `estrategia`? → A: Si es la activa, se responde normalmente. Si es otra, se responde `403` (`ErrorSoloSuperusuario`). El `SUPERUSUARIO` puede consultar cualquiera.
- Q: ¿Qué devuelve el detalle si el jugador no tiene cotizaciones? → A: `cotizacionActual: null`. A diferencia de `CotizacionVigentePort`, no se informa el precio inicial de 1 crédito.
- Q: ¿`desde` y `hasta` del historial tienen valores por defecto? → A: No. Sin `desde`, el historial empieza en la primera cotización; sin `hasta`, termina en la última. `desde` posterior a `hasta` responde `400`.
- Q: ¿Qué fechas devuelve `CotizacionHistoricaPort.fechasCotizacion`? → A: Las fechas reales de cotización de la estrategia activa, es decir, los domingos de cierre de semana. Los lunes eran solo del adapter provisorio. Sin estrategia activa o sin cotizaciones, la lista es vacía.
- Q: ¿Qué hace `preciosA` con un jugador sin cotización a esa fecha? → A: Lo devuelve con `1.0000` crédito, como acordó la `009`. Con una colección nula o vacía devuelve un mapa vacío.
- Q: ¿Se borra `CotizacionHistoricaFijaAdapter`? → A: Sí, como exige FR-009a de la `009`, junto con `CotizacionHistoricaFijaAdapterTest`. Borrar ese test requiere el "sí" explícito del equipo, según la constitución.
- Q: ¿El equipo autoriza borrar `CotizacionHistoricaFijaAdapterTest`? → A: Sí (confirmado por A el 2026-10-29). Se borra junto con el adapter.
- Q: `PortfolioEvolucionE2ETest` de la `009` espera el lunes y el precio 1 del adapter provisorio. ¿Cómo se adapta? → A: Con el "sí" de A (2026-10-29), ese test reemplaza el puerto con `@MockBean CotizacionHistoricaPort` que reproduce el comportamiento provisorio; sus aserciones no cambian. El puerto real se prueba en la integración de la `008`.
- Q: ¿Se agrega una caché para estas lecturas? → A: No. Las lecturas son sobre la base local y no dependen de un proveedor externo. La caché obligatoria de la constitución protege las fuentes externas y ya existe para la cotización vigente del mercado.

## User Scenarios & Testing

### User Story 1 - Ver el detalle de un jugador con su cotización actual (Priority: P1)

Como usuario autenticado quiero ver los datos de un jugador y su cotización actual para decidir si me interesa operarlo.

**Why this priority**: Es la pantalla de entrada a cada jugador y la usa el frontend desde el catálogo.

**Independent Test**: Sembrar un jugador con dos cotizaciones de la estrategia activa y otra de la no activa, llamar `GET /api/jugadores/{id}` y verificar los datos del jugador y que `cotizacionActual` sea la última de la activa con su variación.

**Acceptance Scenarios**:

1. **Given** un jugador con cotizaciones de la estrategia activa, **When** un usuario autenticado llama `GET /api/jugadores/{id}`, **Then** recibe `200` con `JugadorDetalleResponse` y `cotizacionActual` es la última cotización de la estrategia activa con fecha menor o igual a la fecha de negocio.
2. **Given** un jugador sin cotizaciones, **When** se consulta su detalle, **Then** recibe `200` con `cotizacionActual: null`.
3. **Given** un id que no existe, **When** se consulta, **Then** recibe `404`.
4. **Given** un id no numérico, cero o negativo, **When** se consulta, **Then** recibe `400`.
5. **Given** una solicitud sin JWT, **When** se consulta, **Then** recibe `401`.

---

### User Story 2 - Consultar el historial de cotizaciones de un jugador (Priority: P1)

Como usuario autenticado quiero ver la serie semanal de cotizaciones de un jugador para graficar su evolución.

**Why this priority**: El gráfico de evolución es parte central de la entrega y no tiene otra fuente.

**Independent Test**: Sembrar varias semanas de cotizaciones de las dos estrategias para un jugador, consultar con y sin rango y verificar orden ascendente, filtro por estrategia y variaciones.

**Acceptance Scenarios**:

1. **Given** un jugador con cotizaciones, **When** se llama `GET /api/jugadores/{id}/cotizaciones` sin parámetros, **Then** recibe `200` con `HistorialCotizacionesResponse` de la estrategia activa, ordenado por fecha ascendente.
2. **Given** `desde` y `hasta`, **When** se consulta, **Then** solo se incluyen las cotizaciones con fecha en el rango inclusive, y la primera informa la variación contra la cotización anterior aunque esté fuera del rango.
3. **Given** un jugador sin cotizaciones en el rango, **When** se consulta, **Then** recibe `200` con la lista vacía.
4. **Given** `desde` posterior a `hasta` o una fecha con formato inválido, **When** se consulta, **Then** recibe `400`.
5. **Given** un jugador inexistente, **When** se consulta, **Then** recibe `404`.
6. **Given** un `USUARIO` que pide una estrategia que no es la activa, **When** se consulta, **Then** recibe `403`; el `SUPERUSUARIO` recibe el historial de esa estrategia.

---

### User Story 3 - Consultar la cotización de un jugador a una fecha (Priority: P1)

Como usuario autenticado quiero saber cuánto valía un jugador en una fecha dada.

**Why this priority**: Permite comparar precios pasados y es la misma regla que valúa el portfolio histórico.

**Independent Test**: Sembrar cotizaciones de dos domingos y consultar una fecha intermedia, una anterior a la primera y una sin fecha.

**Acceptance Scenarios**:

1. **Given** cotizaciones del jugador, **When** se llama `GET /api/jugadores/{id}/cotizaciones/vigente?fecha=AAAA-MM-DD`, **Then** recibe `200` con la última cotización de la estrategia con fecha menor o igual a la pedida.
2. **Given** no se envía `fecha`, **When** se consulta, **Then** se usa la fecha de negocio actual.
3. **Given** la fecha es anterior a la primera cotización del jugador, **When** se consulta, **Then** recibe `404`.
4. **Given** un jugador inexistente, **When** se consulta, **Then** recibe `404`.
5. **Given** un `USUARIO` que pide una estrategia que no es la activa, **When** se consulta, **Then** recibe `403`.

---

### User Story 4 - Ver el ranking de jugadores (Priority: P2)

Como usuario autenticado quiero ver los jugadores ordenados por score de la última semana, con filtros por liga y posición.

**Why this priority**: Es una vista de descubrimiento; el mercado funciona sin ella.

**Independent Test**: Sembrar cotizaciones de dos semanas para varios jugadores de distintas ligas y posiciones, consultar el ranking con y sin filtros y verificar orden, puestos, paginación y variaciones.

**Acceptance Scenarios**:

1. **Given** cotizaciones de la estrategia activa, **When** se llama `GET /api/jugadores/ranking`, **Then** recibe `200` con `RankingPageResponse` de la última fecha de esa estrategia, ordenado por score descendente con el desempate definido.
2. **Given** filtros `liga` y `posicion`, **When** se consulta, **Then** solo aparecen jugadores que cumplen ambos, y `puesto` se numera dentro de ese resultado.
3. **Given** `page` y `size`, **When** se consulta la segunda página, **Then** `puesto` continúa la numeración de la primera.
4. **Given** todavía no hubo recálculo, **When** se consulta, **Then** recibe `200` con `content` vacío, `version` y `fecha` en `null` y `totalElements` en `0`.
5. **Given** una liga o posición fuera del contrato, `page` negativo o `size` fuera de `1..100`, **When** se consulta, **Then** recibe `400`.
6. **Given** un `USUARIO` que pide una estrategia que no es la activa, **When** se consulta, **Then** recibe `403`.

---

### User Story 5 - Valuar la evolución del portfolio con cotizaciones reales (Priority: P1)

Como feature `009` necesito las fechas de cotización y los precios históricos reales de la estrategia activa para valuar la evolución del portfolio.

**Why this priority**: Hasta esta feature la evolución usa un adapter provisorio con precio 1 para todos.

**Independent Test**: Con cotizaciones sembradas, invocar el puerto directamente en un test de integración y verificar fechas, precios, jugadores sin cotización y rangos vacíos.

**Acceptance Scenarios**:

1. **Given** cotizaciones de la estrategia activa en varios domingos, **When** se llama `fechasCotizacion(desde, hasta)`, **Then** se devuelven esas fechas dentro del rango, ascendentes y sin repetir.
2. **Given** jugadores con y sin cotización, **When** se llama `preciosA(ids, fecha)`, **Then** el mapa trae una entrada por id: el precio de la última cotización de la activa con fecha menor o igual, o `1.0000` si no tiene.
3. **Given** `desde` posterior a `hasta` o fechas nulas, **When** se llama `fechasCotizacion`, **Then** se devuelve una lista vacía.
4. **Given** se mergea esta feature, **When** arranca la aplicación, **Then** existe una sola implementación de `CotizacionHistoricaPort` y la `009` no cambia controllers, services ni respuestas.

## Edge Cases

- Un jugador con cotizaciones solo de la estrategia no activa tiene `cotizacionActual: null` en el detalle.
- Un jugador incorporado al catálogo después de la última semana calculada no aparece en el ranking; la `006` cotiza a todos los jugadores existentes en cada recálculo.
- Un jugador sin cotización en la semana anterior a la del ranking informa `variacion: null`.
- Si cambia la estrategia activa, las lecturas sin `estrategia` pasan a mostrar la serie de la nueva activa desde la siguiente consulta.
- Si no hay ninguna estrategia activa, las lecturas sin `estrategia` se comportan como si no hubiera cotizaciones y `preciosA` devuelve `1.0000` para todos.
- La ruta literal `/api/jugadores/ranking` no debe ser capturada por `/api/jugadores/{jugadorId}`.
- Una `fecha` futura en la cotización vigente devuelve la última cotización existente.
- Los precios se informan con 4 decimales y las variaciones con 2, como en el resto de la entrega.

## Requirements

### Functional Requirements

- **FR-001**: El sistema MUST exponer `GET /api/jugadores/{jugadorId}` y devolver `JugadorDetalleResponse`: los datos de `JugadorResponse` y `cotizacionActual`, que es la última cotización de la estrategia activa con fecha menor o igual a la fecha de negocio, o `null` si no existe.
- **FR-002**: El sistema MUST exponer `GET /api/jugadores/{jugadorId}/cotizaciones` con `estrategia`, `desde` y `hasta` opcionales y devolver `HistorialCotizacionesResponse` ordenado por fecha ascendente, sin paginar.
- **FR-003**: El sistema MUST exponer `GET /api/jugadores/{jugadorId}/cotizaciones/vigente` con `estrategia` y `fecha` opcionales y devolver el esquema `Cotizacion` de la última cotización con fecha menor o igual a la pedida, o a la fecha de negocio si no se envía. Si no hay ninguna, MUST responder `404`.
- **FR-004**: El sistema MUST exponer `GET /api/jugadores/ranking` con `estrategia`, `liga`, `posicion`, `page` y `size`, y devolver `RankingPageResponse` con las cotizaciones de la última fecha de la estrategia, ordenadas por score descendente, precio descendente, nombre ascendente e id ascendente.
- **FR-004a**: El ranking MUST filtrar y paginar en la base de datos. `puesto` MUST ser `page * size + índice + 1` dentro del resultado filtrado.
- **FR-004b**: Sin cotizaciones de la estrategia, el ranking MUST responder `200` con `content` vacío, `version` y `fecha` en `null`, y `totalElements` y `totalPages` en `0`.
- **FR-005**: Sin el parámetro `estrategia`, todas las lecturas MUST usar la estrategia activa. Si un usuario sin rol `SUPERUSUARIO` pide una estrategia distinta de la activa, MUST responder `403`. El `SUPERUSUARIO` MUST poder consultar cualquier estrategia.
- **FR-006**: Cada `Cotizacion` devuelta MUST incluir `jugadorId`, `fecha`, `precio` con 4 decimales, `score` con 4 decimales `HALF_UP`, `variacion`, `estrategia` y `version`. `variacion` MUST calcularse contra la cotización inmediatamente anterior del mismo jugador y estrategia, como `(precio / precioAnterior - 1) * 100` redondeado a 2 decimales `HALF_UP`, y MUST ser `null` si no existe anterior. El cálculo MUST vivir en el modelo.
- **FR-007**: `jugadorId` MUST validarse como positivo; un valor no numérico, cero o negativo MUST responder `400`. Un jugador inexistente MUST responder `404` en detalle, historial y vigente.
- **FR-008**: `estrategia` MUST aceptar solo `RENDIMIENTO_POSICION` y `CONSISTENCIA`; `liga` solo los nombres legibles del contrato; `posicion` solo `POR`, `DEF`, `MED` y `DEL`; `page >= 0` y `1 <= size <= 100`. Cualquier otro valor MUST responder `400`. `desde` posterior a `hasta` MUST responder `400`.
- **FR-009**: La fecha de negocio MUST obtenerse del `Clock` inyectado convertido con `ClockConfig.ZONA_NEGOCIO`; ningún Service MUST llamar a `LocalDate.now()` sin `Clock`.
- **FR-010**: El sistema MUST implementar `CotizacionHistoricaPort` sin modificar su firma: `fechasCotizacion(desde, hasta)` MUST devolver las fechas de cotización de la estrategia activa dentro de `[desde, hasta]`, ascendentes y sin repetir, o una lista vacía si alguna fecha es nula, `desde` es posterior a `hasta` o no hay estrategia activa; `preciosA(jugadorIds, fecha)` MUST devolver una entrada por cada id pedido con el precio de la última cotización de la activa con fecha menor o igual, o `1.0000` si no existe, y un mapa vacío si la colección es nula o vacía.
- **FR-010a**: `preciosA` MUST resolver todos los ids con una cantidad de consultas que no dependa de la cantidad de jugadores.
- **FR-011**: Al incorporar la implementación real, `CotizacionHistoricaFijaAdapter` MUST eliminarse para que exista un solo bean de `CotizacionHistoricaPort`. `CotizacionHistoricaFijaAdapterTest` MUST eliminarse solo con el "sí" explícito del equipo.
- **FR-012**: Todos los endpoints MUST requerir JWT, usar el esquema `Error` en las respuestas de error e incluir el `correlationId`.
- **FR-013**: La migración de esta feature MUST usar `V18`, dentro del rango reservado de A (`V18` y `V19` libres tras la `011`), y MUST limitarse a crear un índice sobre `cotizacion (estrategia, fecha)` para el ranking y las fechas del puerto.
- **FR-014**: Swagger y la colección de Postman MUST documentar los cuatro endpoints.

### Contract Surface

| Operación | Método y ruta | Autorización | Respuestas |
|---|---|---|---|
| Detalle | `GET /api/jugadores/{jugadorId}` | Usuario autenticado | `200 JugadorDetalleResponse`, `400`, `401`, `404` |
| Ranking | `GET /api/jugadores/ranking` | Usuario autenticado; otra estrategia solo `SUPERUSUARIO` | `200 RankingPageResponse`, `400`, `401`, `403` |
| Historial | `GET /api/jugadores/{jugadorId}/cotizaciones` | Usuario autenticado; otra estrategia solo `SUPERUSUARIO` | `200 HistorialCotizacionesResponse`, `400`, `401`, `403`, `404` |
| Vigente | `GET /api/jugadores/{jugadorId}/cotizaciones/vigente` | Usuario autenticado; otra estrategia solo `SUPERUSUARIO` | `200 Cotizacion`, `400`, `401`, `403`, `404` |

No se modifica el contrato.

### Key Entities

- **Cotizacion**: entidad existente de la `006`; esta feature la lee y agrega en el modelo el cálculo de la variación contra la anterior.
- **Jugador**: entidad existente; aporta nombre, equipo, liga y posición al detalle y al ranking.
- **Estrategia**: entidad existente; determina la estrategia activa por defecto.
- **CotizacionHistoricaPort**: puerto existente de la `009`; esta feature aporta su única implementación.

## Non-Functional Requirements

- **NFR-001**: El cálculo de la variación y del puesto MUST tener tests unitarios sin Spring.
- **NFR-002**: Las consultas de repositorio y el puerto MUST tener tests de integración contra PostgreSQL con Testcontainers.
- **NFR-003**: Los endpoints MUST tener tests E2E con MockMvc en el paquete `e2e`, con `@WithMockUser(roles = "USUARIO")` y `@WithMockUser(roles = "SUPERUSUARIO")`.
- **NFR-004**: Los tests MUST limpiar solo los datos que crean y usar un `Clock` fijo cuando dependan de la fecha.
- **NFR-005**: El código nuevo MUST cumplir el Quality Gate de SonarCloud (cobertura del código nuevo de al menos 80 %).

## Success Criteria

### Measurable Outcomes

- **SC-001**: Los cuatro endpoints devuelven exactamente los campos obligatorios del contrato en el 100 % de las respuestas exitosas.
- **SC-002**: Un `USUARIO` nunca obtiene datos de una estrategia que no es la activa.
- **SC-003**: Para el mismo estado de la base, dos consultas iguales del ranking devuelven el mismo orden y los mismos puestos.
- **SC-004**: La evolución del portfolio de la `009` muestra precios distintos de 1 para jugadores con cotizaciones, sin cambiar código de la `009`.
- **SC-005**: El ranking de una semana completa responde en menos de 1 segundo en la compu de la demo.

## Assumptions

- La `006` guarda una cotización por jugador, estrategia y semana, con fecha en el domingo de cierre, y no deja huecos para los jugadores existentes al recalcular.
- `Jugador.posicion` ya se guarda como `POR`, `DEF`, `MED` o `DEL`.
- La `011` está mergeada en `dev` antes de crear la rama de esta feature, por eso `V13`–`V17` están ocupadas.
- El rol del usuario se obtiene del JWT, como en las features `006`, `007` y `009`.
- La `009` ya consume `CotizacionHistoricaPort` y su `PortfolioServiceTest` usa un mock del puerto.
