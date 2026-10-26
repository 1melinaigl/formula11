# Research: Portfolio e historial de operaciones

## Decisión: mantener el monolito y reutilizar el mercado de la 007

El backend ya separa `controller`, `service`, `persistence`, `model`, `dto`,
`exception`, `security` y `adapter`. `Cuenta`, `Tenencia`, `Transaccion`,
`MercadoService`, `UsuarioService` y `OperacionResponse` contienen la lógica y
representaciones necesarias. Crear entidades o tablas paralelas duplicaría el
estado financiero y rompería la trazabilidad del mercado.

**Alternativas descartadas**: un módulo independiente de portfolio, consultas
directas desde el controller o DTOs retornados por el service.

## Decisión: puerto histórico en `com.formula11.service`

La evolución debe quedar desacoplada de la tabla y estrategia de cotizaciones de
la feature 008. Se agregará exactamente:

```java
List<LocalDate> fechasCotizacion(LocalDate desde, LocalDate hasta);
Map<Long, BigDecimal> preciosA(Collection<Long> jugadorIds, LocalDate fecha);
```

El adapter fijo satisface el contrato durante el desarrollo y será reemplazable
por la implementación de A sin cambiar service, controller ni respuestas.

## Decisión: validar autorización antes de resolver el recurso path

El controller resuelve el id autenticado a través de
`UsuarioService.obtenerIdPorEmail`. La comparación con `{usuarioId}` se hace antes
de `findById` o de cualquier consulta de portfolio. Así un id de otro usuario,
existente o no, siempre recibe `403`. El superusuario también recibe `403` porque
no tiene cuenta ni portfolio.

## Decisión: una única reconstrucción cronológica para cada evolución

Las operaciones se ordenan ascendentemente por instante e id. El acumulador por
jugador conserva cantidad y precio promedio; las compras recalculan ponderado y
las ventas solo reducen cantidad. Cada fecha histórica consume el estado acumulado
hasta su fecha UTC. Esto evita recomputar todas las transacciones desde cero para
cada punto y reproduce las reglas de `Tenencia`.

## Decisión: no validar existencia de `jugadorId` en historial

El filtro se aplica directamente en la consulta del usuario autenticado. Un id
inexistente no es un error de validación y devuelve una página vacía con `200`,
según la aclaración de la especificación.

## Decisión: conservar la compra reutilizable con overload de `Clock`

La operación de demo debe usar las mismas validaciones, bloqueos y auditoría que
una compra HTTP. La sobrecarga con `Clock` será una entrada técnica para escenarios
reproducibles y delegará en un núcleo compartido; la versión pública actual seguirá
usando el `Clock` inyectado.

## Decisión: no crear V26 de forma preventiva

La migración V23 ya crea `ix_transacciones_usuario_fecha (usuario_id, fecha DESC)`,
que cubre el filtro principal y el orden del historial. Se verificará con
`EXPLAIN`; solo se agregará V26 si una consulta concreta exige otro índice.
