# Feature Specification: Ranking optimizado para consultas frecuentes

**Feature Branch**: `018-ranking-optimizado`

**Created**: 2026-11-26

**Status**: Draft

**Input**: User description: "Ranking optimizado para consultas frecuentes (Entrega 3, persona C)."

## Scope

Optimizar las lecturas de `GET /api/jugadores/ranking`, cuyo resultado solo cambia cuando se recalculan cotizaciones o se activa otra estrategia. Reutilizar resultados idénticos entre pedidos, invalidar los resultados cuando cambie esa información y comprobar que la consulta paginada aprovecha el índice de cotizaciones existente.

Esta feature no cambia el contrato HTTP, las reglas de cálculo ni el orden del ranking. No modifica otros endpoints ni el proceso de recálculo. La configuración de caché es transversal en `config`; su uso e invalidación pertenecen a `service/cotizaciones`, de acuerdo con la arquitectura de la feature `014`.

## User Scenarios & Testing

### User Story 1 - Repetir una consulta de ranking sin consultar cotizaciones (Priority: P1)

Como usuario autenticado quiero que una consulta frecuente del ranking se responda sin repetir las lecturas de cotizaciones mientras los datos no cambien. La resolución liviana de la estrategia activa se sigue haciendo en cada pedido.

**Why this priority**: El ranking realiza varias consultas para construir una respuesta y sus cotizaciones permanecen estables entre recálculos semanales o activaciones de estrategia.

**Independent Test**: Consultar dos veces el mismo ranking y verificar que ambas respuestas sean iguales, que `EstrategiaRepository.findByActivaTrue` se invoque en ambos pedidos y que `CotizacionRepository` no se invoque en el segundo.

**Acceptance Scenarios**:

1. **Given** una consulta válida de ranking cuya respuesta ya fue calculada, **When** se repiten la misma estrategia resuelta, liga, posición, página y tamaño, **Then** se devuelve el resultado correspondiente sin volver a invocar `CotizacionRepository`; `EstrategiaRepository.findByActivaTrue` sí se consulta en cada pedido.
2. **Given** dos consultas que difieren en estrategia, liga, posición, página o tamaño, **When** se ejecutan, **Then** cada una devuelve el resultado correspondiente a sus propios criterios.
3. **Given** una estrategia sin cotizaciones, **When** se repite la misma consulta vacía antes de invalidar la caché, **Then** se devuelve el resultado vacío cacheado sin invocar `CotizacionRepository` de nuevo.
4. **Given** una entrada guardada en caché, **When** vence su período de expiración o se alcanza el límite de tamaño, **Then** deja de servirse como una entrada vigente.

### User Story 2 - Ver datos vigentes después de un cambio de cotizaciones (Priority: P1)

Como usuario autenticado quiero que el ranking refleje las cotizaciones nuevas luego de un recálculo o de activar otra estrategia.

**Why this priority**: Un resultado en caché no debe ocultar cambios legítimos en los datos ni seguir asociado a la estrategia activa anterior.

**Independent Test**: Calentar el ranking, cambiar las cotizaciones mediante el recálculo o activar otra estrategia y comprobar que el siguiente pedido refleja los datos actualizados.

**Acceptance Scenarios**:

1. **Given** que se consultó y cacheó un ranking, **When** termina un recálculo de cotizaciones, **Then** el siguiente pedido devuelve los datos recalculados y no reutiliza la respuesta previa.
2. **Given** que se consultó y cacheó un ranking, **When** se activa otra estrategia, **Then** el siguiente pedido sin estrategia explícita devuelve los datos de la nueva estrategia activa.
3. **Given** un ranking cacheado para una estrategia, **When** un usuario sin permiso de superusuario pide explícitamente otra estrategia, **Then** la solicitud sigue siendo rechazada con el error de autorización existente, aunque haya un resultado de esa estrategia en caché.

### User Story 3 - Mantener la consulta paginada eficiente y las validaciones intactas (Priority: P2)

Como responsable del servicio quiero confirmar que la consulta de la página del ranking aproveche el índice adecuado sin alterar el comportamiento observable del endpoint.

**Why this priority**: La caché reduce lecturas repetidas, mientras que el plan de ejecución debe seguir siendo eficiente cuando la respuesta aún no está cacheada.

**Independent Test**: Revisar el plan `EXPLAIN` de la consulta paginada y ejecutar pruebas de contrato y validación del endpoint antes y después de llenar la caché.

**Acceptance Scenarios**:

1. **Given** la consulta de página del ranking, **When** se inspecciona su plan con `EXPLAIN`, **Then** se registra en `research.md` el plan obtenido contra PostgreSQL local o de Testcontainers y se determina si el índice definido en `V18__indice_cotizacion_estrategia_fecha.sql` es suficiente.
2. **Given** que el índice de V18 no alcanza para el orden completo (`score` descendente, precio descendente, nombre e id ascendentes), **When** se implementa el soporte de índices, **Then** se agrega una migración nueva con el índice necesario, sin modificar V18 y usando V30 si hace falta una nueva versión.
3. **Given** parámetros inválidos o una estrategia no permitida para el rol, **When** se solicita el ranking aunque haya entradas en caché, **Then** se mantienen las validaciones y errores actuales antes de buscar el resultado cacheado.

## Edge Cases

- Dos pedidos con la misma estrategia solicitada deben compartir resultado solo cuando también coincidan todos los filtros y parámetros de paginación.
- La estrategia que forma parte de la clave es la estrategia ya resuelta para el pedido, incluida la estrategia activa cuando no se envía el parámetro.
- Una solicitud de estrategia alternativa no debe obtener acceso por coincidir con una clave ya cacheada; la autorización se valida en cada pedido.
- Si un recálculo o cambio de estrategia no produce resultados para el ranking, el siguiente pedido debe reflejar el estado actualizado y no la entrada anterior.
- Las respuestas de errores por parámetros, permisos o ausencia de datos conservan el comportamiento actual del endpoint.

## Requirements

### Functional Requirements

- **FR-001**: El sistema MUST almacenar en caché el `RankingResultado` del ranking mediante Caffeine, con `expireAfterWrite` de 15 minutos y `maximumSize` de 1000, siguiendo el patrón de `CotizacionCacheConfig`.
- **FR-002**: La clave de caché MUST incluir la estrategia resuelta, liga, posición, `page` y `size`; pedidos que difieran en cualquiera de esos valores MUST ocupar entradas distintas.
- **FR-003**: El sistema MUST validar parámetros, invocar `EstrategiaRepository.findByActivaTrue` para resolver la estrategia activa en cada pedido y comprobar la autorización antes de consultar la caché. La estrategia activa MUST NOT cachearse. La caché MUST NOT permitir que un usuario no superusuario obtenga el ranking de una estrategia que no sea la activa.
- **FR-004**: Una repetición válida con la misma clave MUST devolver el resultado cacheado sin volver a invocar `CotizacionRepository` para la fecha máxima, la página, los precios anteriores o la versión. La resolución de la estrategia activa puede seguir invocando `EstrategiaRepository.findByActivaTrue` en cada pedido.
- **FR-005**: La configuración del caché Caffeine MUST residir en `config` y seguir el patrón de `CotizacionCacheConfig`; la lógica de consulta y la invalidación MUST residir en `service/cotizaciones` y respetar `ArquitecturaTest` de la feature `014`.
- **FR-006**: El sistema MUST cachear también los resultados vacíos, incluida la ausencia de estrategia activa, y MUST invalidar el caché del ranking al recalcular cotizaciones y al activar otra estrategia, incorporando esa limpieza a `CotizacionCacheInvalidator`, que ya es invocado por `RecalculoService` y `EstrategiaService`. La invalidación explícita es el mecanismo principal para mantener los resultados al día; al activar una estrategia, se ejecuta dentro de la transacción y antes del commit.
- **FR-007**: El endpoint MUST conservar los mismos parámetros, respuesta, validaciones y errores HTTP. La optimización MUST NOT cambiar el contrato ni el orden del ranking: score descendente, precio descendente, nombre ascendente e id ascendente.
- **FR-008**: La consulta paginada MUST revisarse con `EXPLAIN` contra PostgreSQL local o de Testcontainers y el plan obtenido MUST documentarse en `research.md`. Si el análisis con datos representativos confirma que V18 no alcanza para filtrar y ordenar la página, MUST agregarse una migración nueva con el índice adecuado, sin modificar migraciones existentes; para una versión disponible a partir de V26, usar V30.
- **FR-009**: Las pruebas MUST verificar que un segundo pedido idéntico no vuelve a invocar `CotizacionRepository` aunque vuelva a resolver la estrategia activa, que un resultado vacío también se cachea, que un recálculo y la activación de otra estrategia invalidan la caché y permiten obtener los datos nuevos, y que las validaciones y permisos se aplican incluso cuando existiría una respuesta cacheada.

### Key Entities

- **RankingResultado**: resultado existente del servicio de ranking que se reutiliza para pedidos con la misma clave mientras no cambien las cotizaciones.
- **Estrategia resuelta**: estrategia efectiva para el pedido, explícita o activa; identifica los datos a los que corresponde el resultado cacheado.
- **Clave de ranking**: combinación de estrategia resuelta, liga, posición, página y tamaño que distingue resultados.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Para dos pedidos válidos e idénticos mientras la entrada siga vigente, el segundo pedido no invoca `CotizacionRepository` y devuelve el mismo resultado; `EstrategiaRepository.findByActivaTrue` se invoca en ambos pedidos.
- **SC-002**: Después de un recálculo o de activar otra estrategia, el siguiente pedido refleja los datos nuevos en vez de los datos almacenados previamente.
- **SC-003**: El 100 % de las solicitudes continúa aplicando validaciones y permisos antes de acceder al caché, con las respuestas HTTP existentes.
- **SC-004**: `research.md` contiene el plan `EXPLAIN` obtenido y documenta si V18 basta para la consulta de página; si no basta con datos representativos, una migración nueva permite usar un índice adecuado sin alterar el contrato ni la ordenación.
- **SC-005**: La prueba de arquitectura de la feature `014` pasa con la configuración en `config` y la lógica e invalidación en `service/cotizaciones`.

## Assumptions

- Las cotizaciones de una estrategia no cambian excepto durante un recálculo; la estrategia efectiva también puede cambiar al activar otra.
- `CotizacionCacheConfig` es el patrón de configuración vigente para cachés Caffeine, y `CotizacionCacheInvalidator` ya recibe llamadas de los dos flujos que deben invalidar el ranking.
- `RankingResultado` contiene entidades `Jugador`; se acepta cachearlas porque el mapeo de respuesta solo lee campos simples y no modifica las entidades. El mapeo no debe acceder a relaciones lazy.
- Las reglas actuales de validación de `page`, `size`, liga, posición y permiso de superusuario son la fuente de verdad y no se modifican en esta feature.
- Las migraciones desde V26 en adelante están reservadas por otras integrantes del equipo; V30 puede usarse si se requiere agregar un índice.
- La prueba de carga es opcional y queda fuera de las tareas obligatorias; las mediciones antes y después pueden agregarse a las release notes.
