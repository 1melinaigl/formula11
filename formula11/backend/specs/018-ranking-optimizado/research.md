# Research: Ranking optimizado (018)

## Resolución de estrategia y alcance de la caché

- `CotizacionConsultaService.ranking` resuelve la estrategia mediante `resolverEstrategia`, que consulta `EstrategiaRepository.findByActivaTrue`; esa consulta debe continuar en cada pedido, incluso ante un hit de caché.
- Para construir un resultado nuevo, el servicio consulta `CotizacionRepository` por fecha máxima, página, precios de la fecha anterior y versión. Esas consultas deben omitirse cuando hay un resultado cacheado.
- La estrategia activa no se cachea. `EstrategiaService.activar` invalida durante la transacción y antes del commit; cachear el resultado de `findByActivaTrue` podría mantener y volver a guardar la estrategia anterior durante ese intervalo.
- También se cachean los resultados vacíos. La invalidación explícita después del recálculo y la activación mantiene vigente tanto una respuesta con contenido como una vacía.
- `RankingResultado.Item` contiene una entidad `Jugador`. El mapeo actual hacia `RankingItemResponse` lee los campos simples de `Jugador`; `Jugador` no declara relaciones JPA. La implementación debe preservar este comportamiento: no recorrer relaciones lazy ni modificar las entidades cacheadas.

## Revisión inicial de EXPLAIN

Se ejecutó `EXPLAIN (COSTS, VERBOSE)` contra PostgreSQL local (`localhost:5432/formula11`) para la consulta equivalente a la selección de la página del ranking, con página 0, tamaño 20, sin filtros de liga ni posición. La base local no tenía cotizaciones para la estrategia activa; se usó `RENDIMIENTO_POSICION` y una fecha fija (`2026-01-01`) para obtener el plan. Por eso este resultado es una observación inicial y no una evaluación con distribución representativa.

Consulta revisada:

```sql
SELECT c.id, c.fecha, c.precio, c.score, c.estrategia, c.version, c.pesos, c.jugador_id
FROM cotizacion c
JOIN jugadores j ON j.id = c.jugador_id
WHERE c.estrategia = 'RENDIMIENTO_POSICION'
  AND c.fecha = DATE '2026-01-01'
ORDER BY c.score DESC, c.precio DESC, j.nombre ASC, j.id ASC
LIMIT 20 OFFSET 0;
```

Plan obtenido:

```text
Limit  (cost=18.50..18.50 rows=1 width=296)
  Output: c.id, c.fecha, c.precio, c.score, c.estrategia, c.version, c.pesos, c.jugador_id, j.nombre, j.id
  ->  Sort  (cost=18.50..18.50 rows=1 width=296)
        Output: c.id, c.fecha, c.precio, c.score, c.estrategia, c.version, c.pesos, c.jugador_id, j.nombre, j.id
        Sort Key: c.score DESC, c.precio DESC, j.nombre, c.jugador_id
        ->  Nested Loop  (cost=0.43..18.49 rows=1 width=296)
              Inner Unique: true
              ->  Index Scan using ix_cotizacion_jugador_fecha on public.cotizacion c
                    Index Cond: (c.fecha = '2026-01-01'::date)
                    Filter: ((c.estrategia)::text = 'RENDIMIENTO_POSICION'::text)
              ->  Index Scan using jugadores_pkey on public.jugadores j
                    Index Cond: (j.id = c.jugador_id)
```

En este plan no se eligió `ix_cotizacion_estrategia_fecha` de V18: se usó `ix_cotizacion_jugador_fecha` para la fecha y luego se filtró la estrategia. El ordenamiento completo requirió un nodo `Sort`, incluido el nombre que proviene de la tabla `jugadores`. Dado que la tabla y la distribución de la base local no representan una carga real del ranking, este plan no basta por sí solo para decidir el índice adicional. Durante la implementación se debe repetir y documentar `EXPLAIN` con datos representativos en PostgreSQL de Testcontainers o local; solo entonces decidir si se agrega una migración nueva (V30 si hace falta).

## EXPLAIN con datos representativos

La base local no tenía aplicadas las migraciones V13 a V19 (incluida V18), por eso la revisión inicial no encontró `ix_cotizacion_estrategia_fecha`. Se repitió la revisión en PostgreSQL 17 local dentro de una transacción con `ROLLBACK` (no quedan datos ni índices). Se creó el índice de V18 y se generaron cotizaciones sintéticas: 2620 jugadores × 2 estrategias × 52 semanas (≈ 272 000 filas), con `ANALYZE`.

Consulta (página 0, tamaño 20, sin filtros, una fecha con 2620 cotizaciones):

```sql
SELECT c.*, j.nombre FROM cotizacion c JOIN jugadores j ON j.id = c.jugador_id
WHERE c.estrategia = 'RENDIMIENTO_POSICION' AND c.fecha = DATE '2025-12-28'
ORDER BY c.score DESC, c.precio DESC, j.nombre, j.id LIMIT 20;
```

| Índice | Plan | Filas de cotizacion leídas | Tiempo de ejecución |
|---|---|---|---|
| V18 `(estrategia, fecha)` | Bitmap Index Scan + Hash Join + `Sort` top-N | 2620 | 3,2 ms |
| Candidato `(estrategia, fecha, score DESC, precio DESC)` | Index Scan + Nested Loop + `Incremental Sort` (presorted: score, precio) | 21 | 0,13 ms |

Con filtro de posición (`DEL`, offset 100) y solo V18, el plan también lee las 2620 cotizaciones de la fecha y ordena (1,5 ms).

**Decisión**: agregar `V30__indice_ranking_cotizacion.sql` con `CREATE INDEX IF NOT EXISTS ix_cotizacion_ranking ON cotizacion (estrategia, fecha, score DESC, precio DESC)`. Ningún índice sobre `cotizacion` puede evitar por completo el ordenamiento, porque el desempate usa `jugadores.nombre`; el índice deja solo un `Incremental Sort` sobre las filas empatadas. V18 no se modifica ni se elimina: es prefijo del índice nuevo y ya forma parte de una migración aplicada.

## Criterios de validación

- Un hit de caché no invoca `CotizacionRepository`, pero sí resuelve la estrategia activa y repite validaciones y autorización.
- Los resultados vacíos se cachean y se limpian con las mismas invalidaciones que los resultados con contenido.
- Las pruebas de carga antes y después son opcionales y quedan fuera de las tareas obligatorias.
