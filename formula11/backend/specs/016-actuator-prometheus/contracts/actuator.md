# Contrato: endpoints de Actuator

Base: `/actuator`. Autenticación: JWT del login (`Authorization: Bearer <token>`).

| Endpoint | Método | Sin sesión | Usuario | Superusuario |
|---|---|---|---|---|
| `/actuator/health` (y `/actuator/health/**`) | GET | 200 o 503, solo `status` | 200 o 503, solo `status` | 200 o 503, con `components` y `details` |
| `/actuator/info` | GET | 401 | 403 | 200 |
| `/actuator/metrics`, `/actuator/metrics/{nombre}` | GET | 401 | 403 | 200 |
| `/actuator/prometheus` | GET | 401 | 403 | 200, `text/plain` (formato de exposición de Prometheus) |
| `/actuator/loggers`, `/actuator/loggers/{nombre}` | GET | 401 | 403 | 200 |
| `/actuator/loggers/{nombre}` | POST | 401 | 403 | 204 |

Los endpoints que no están en la lista (`env`, `beans`, `configprops`, `heapdump`, etc.) no se
exponen y responden 404 al superusuario. Sin sesión, responden 401, como cualquier ruta que
no es pública.

## Errores

401 y 403 usan el mismo cuerpo que el resto de la API:

```json
{ "mensaje": "La autenticación es requerida", "timestamp": "2026-11-26T19:00:00Z", "correlationId": "..." }
```

```json
{ "mensaje": "No tiene permisos para realizar esta operación", "timestamp": "...", "correlationId": "..." }
```

Los nombres de los campos son los de `ErrorResponse`; no cambian.

## Health

Sin superusuario:

```json
{ "status": "DOWN" }
```

Superusuario (recorte):

```json
{
  "status": "DOWN",
  "components": {
    "cargaMetricas": {
      "status": "DOWN",
      "details": {
        "estado": "FALLIDA",
        "fin": "2026-11-23T06:12:40Z",
        "ejecucionId": 42,
        "error": "No se pudo descargar la liga"
      }
    },
    "db": { "status": "UP", "details": { "database": "PostgreSQL" } }
  }
}
```

Sin cargas: `"cargaMetricas": { "status": "UP", "details": { "detalle": "Sin cargas de métricas" } }`.
`DOWN` hace que el endpoint responda 503, que es el comportamiento estándar del health.

## Info

```json
{ "app": { "nombre": "Formula11", "version": "0.0.1-SNAPSHOT" } }
```

## Loggers

Cambiar el nivel del registro de auditoría:

```http
POST /actuator/loggers/AUDITORIA
Content-Type: application/json

{ "configuredLevel": "WARN" }
```

Con `"configuredLevel": null` se vuelve al nivel de `logback-spring.xml` (`INFO`).
