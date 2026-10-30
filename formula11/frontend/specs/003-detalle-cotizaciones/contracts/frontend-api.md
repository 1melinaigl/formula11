# Frontend API Contract: Detalle de jugador con evolución de cotizaciones

El frontend consume estas formas mediante `solicitarProtegida`. Todas las respuestas de
error usan `mensaje`, `timestamp` y `correlationId`.

## Player detail

`GET /api/jugadores/{jugadorId}`

Returns `JugadorDetalle` with `cotizacionActual`, which is `null` when no recalculation
exists. Requires authentication.

## Quote history

`GET /api/jugadores/{jugadorId}/cotizaciones`

Returns:

```json
{
  "jugadorId": 7,
  "estrategia": "RENDIMIENTO_POSICION",
  "cotizaciones": [
    {
      "jugadorId": 7,
      "fecha": "2026-10-20",
      "precio": 125.5,
      "score": 82.25,
      "variacion": 12.5,
      "estrategia": "RENDIMIENTO_POSICION",
      "version": 3
    }
  ]
}
```

The canonical response is not paginated and is ordered by ascending `fecha`.

## Strategy-specific history

`GET /api/jugadores/{jugadorId}/cotizaciones?estrategia={codigo}`

- `SUPERUSUARIO`: may request either `RENDIMIENTO_POSICION` or `CONSISTENCIA`.
- `USUARIO`: sees only the active strategy; requesting another strategy is forbidden
  and returns `403`.

## Active strategy

`GET /api/estrategias`

Returns the configured strategies with `codigo`, `nombre` and `activa`. The detail
marks the strategy whose `activa` is `true`. The UI must surface an explicit error if
the response cannot identify a valid active strategy; it must not silently choose one.

## Error contract

Protected calls include `Authorization: Bearer ...` and `X-Correlation-Id`.

| Status | Meaning for this feature |
|---|---|
| `401` | Session is invalid; clear the session and show access/error state. |
| `403` | Authenticated `USUARIO` requested a non-active strategy. |
| `404` | Player does not exist; show player error and no quote panel. |
| `5xx` | Preserve the player when possible and show the affected panel error. |
