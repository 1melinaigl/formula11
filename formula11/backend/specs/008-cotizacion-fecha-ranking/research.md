# Research: 008 - Detalle, historial, cotización a una fecha y ranking

Hallazgos verificados en el código de `dev` (merge de la `009`, commit `9d8b2a0`) y decisiones de diseño.

## Datos existentes

- La tabla `cotizacion` (V12) guarda una fila por jugador, estrategia y fecha. La fecha es el domingo de cierre de la semana en `America/Argentina/Buenos_Aires` (FR-017a de la `006`).
- `cotizacion` no tiene columna de variación: la variación del contrato se calcula al leer.
- La `006` guarda una cotización por semana para cada jugador existente, aunque no haya jugado (conserva el precio anterior). Por eso la cotización "inmediatamente anterior" es la de la semana previa, salvo para jugadores que entraron al catálogo después.
- `CotizacionRepository` ya tiene `findTopByJugadorIdAndEstrategiaAndFechaLessThanEqualOrderByFechaDesc` y `findByJugadorIdAndEstrategiaOrderByFechaAsc`, que alcanzan para el detalle, el historial y la vigente.
- `Jugador.posicion` se guarda como `POR`, `DEF`, `MED` o `DEL`: el seed V2 y la normalización de `FootballDataAdapter` usan esos valores.
- La liga se persiste como enum (`LA_LIGA`) y el contrato la expone con el nombre legible (`La Liga`), igual que en el catálogo.
- El rol llega como authority `ROLE_<ROL>` desde `JwtAuthenticationFilter`.

## Puerto de la 009

- La firma de `CotizacionHistoricaPort` es fija (FR-009 de la `009`): `fechasCotizacion(desde, hasta)` y `preciosA(jugadorIds, fecha)`.
- `PortfolioService` lo recibe por constructor y `PortfolioServiceTest` lo mockea. Por eso cambiar la implementación no afecta los tests de la `009`.
- `CotizacionHistoricaFijaAdapter` es un `@Component` y tiene su propio test. FR-009a de la `009` exige borrarlo cuando entre la `008`.
- El adapter provisorio devuelve los lunes; la implementación real devuelve los domingos de la tabla. `PortfolioService` recorre las fechas que recibe, sin asumir un día de la semana.

## Decisiones

| Tema | Decisión | Alternativa descartada |
|---|---|---|
| Variación | Se calcula al leer, en el modelo puro `VariacionCotizacion` | Columna nueva: obliga a tocar el recálculo de la `006` |
| Historial | Cargar la serie completa del jugador y la estrategia (menos de 52 filas) y filtrar en memoria | Consulta por rango más otra para la anterior: dos consultas para el mismo resultado |
| Ranking | `Specification` paginada con `@EntityGraph` y una consulta extra para la semana anterior | Traer todo y ordenar en memoria: no escala y contradice el filtrado en base |
| `preciosA` | Una consulta con subconsulta correlacionada de `max(fecha)` | Una consulta por jugador (N+1) |
| Controller | Nuevo `CotizacionJugadorController` en `/api/jugadores` | Ampliar `JugadorController`, de la feature 003 |
| 403 | Excepción propia `SoloSuperusuarioException` | `AccessDeniedException` lanzada desde el service: acopla el service a Spring Security |
| Caché | Sin caché nueva | Caché del ranking: necesita invalidación al recalcular y no protege ninguna fuente externa |
| Índice | `V18` con `(estrategia, fecha)` | Sin índice: el ranking y `fechasCotizacion` filtran por estrategia y fecha y el índice existente empieza por `jugador_id` |
