# Frontend API Contract: Ranking de jugadores

El frontend consume el endpoint mediante `solicitarProtegida`. Cada llamada incluye
`Authorization` y `X-Correlation-Id`; un `401` limpia la sesión.

## Ranking

`GET /api/jugadores/ranking?page={page}&size=20[&posicion={codigo}][&liga={nombre}]`

- `page` es base cero; la primera consulta usa `page=0`.
- `size` es siempre `20`.
- `posicion` acepta `POR`, `DEF`, `MED` o `DEL`.
- `liga` acepta un valor del enum `Liga` del contrato y se envía exactamente igual
  a su etiqueta legible, sin traducción ni transformación.
- Los parámetros vacíos se omiten.
- `estrategia` no se envía en esta feature; la vista es siempre de la estrategia
  activa.

## Successful response

```json
{
  "content": [
    {
      "puesto": 1,
      "jugador": {
        "id": 7,
        "nombre": "Nicolò Barella",
        "equipo": "FC Internazionale Milano",
        "liga": "Serie A",
        "posicion": "MED"
      },
      "precio": 1.21,
      "variacion": 10,
      "score": 0.75
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "estrategia": "RENDIMIENTO_POSICION",
  "version": 2,
  "fecha": "2026-10-19"
}
```

El frontend respeta el orden y `puesto` recibidos. La fecha se muestra como
`dd/mm/aaaa`. `variacion` llega como porcentaje (`10` = `10 %`) y `score` puede ser
`null`.

## Empty and no-recalculation responses

Una respuesta con `content: []` es válida. Si `version` y `fecha` son `null`, la UI
muestra `Estrategia activa: <nombre> · sin recálculo todavía`; luego muestra
`Ranking vacío` cuando no hay filas sin filtros, o `Sin jugadores` cuando un filtro
no devuelve resultados.

## Errors

```json
{
  "mensaje": "No se pudo calcular el ranking.",
  "timestamp": "2026-10-24T12:00:00Z",
  "correlationId": "ranking-500"
}
```

| Status | Frontend behavior |
|---|---|
| `400` | Show `mensaje` and `correlationId`; keep the ranking screen. |
| `401` | Clear session and show access/error state. |
| `403` | Aunque esta pantalla siempre consulta la estrategia activa y no envía `estrategia`, el backend puede rechazar la operación por autorización; mostrar `mensaje` y `correlationId`, sin reintentar silenciosamente ni cambiar de estrategia. |
| `5xx` | Show `mensaje` and `correlationId` when supplied. |

`clienteApi` conserva el comportamiento existente cuando la respuesta de error no
completa el esquema: usa el `X-Correlation-Id` generado para la solicitud si el
backend no devuelve `correlationId`, y usa `No se pudo completar la solicitud.`
si falta `mensaje`.
