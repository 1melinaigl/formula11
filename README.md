# Formula11

Mercado de tokens de jugadores de fútbol de las cinco ligas principales: catálogo, cotización semanal según estrategia, compra y venta de tokens, y portfolio de cada usuario.

- `formula11/backend`: API REST en Spring Boot (Java 21, PostgreSQL, Flyway).
- `formula11/frontend`: aplicación web en React + Vite.

## Arquitectura

El backend es un **monolito en capas**. Las capas son los paquetes de primer nivel de `com.formula11`, y dentro de cada una las clases se agrupan por **área funcional**:

```
com.formula11
├── controller/    traduce HTTP: recibe DTOs, valida y delega en el service
├── service/       reglas de negocio y transacciones
├── persistence/   repositorios de Spring Data
├── model/         entidades y lógica de dominio
├── dto/           requests y responses de la API
├── adapter/       integraciones externas (football-data, WhoScored)
├── config/        configuración (transversal)
├── exception/     errores y su traducción a HTTP (transversal)
├── observabilidad/ auditoría de web services y métricas (transversal)
└── security/      autenticación con JWT (transversal)
```

Las áreas funcionales se repiten en cada capa:

| Área | Qué abarca |
|---|---|
| `usuarios` | registro, login, roles y superusuario |
| `jugadores` | catálogo y sincronización con football-data |
| `metricas` | métricas por partido (WhoScored) y su carga |
| `cotizaciones` | estrategias, recálculo semanal, historial y ranking |
| `mercado` | compra y venta de tokens, cuentas, tenencias y auditoría |
| `portfolio` | portfolio, evolución e historial de operaciones del usuario |
| `senales` | detección de jugadores subvaluados |

Por ejemplo, la compra de tokens recorre `controller/mercado` → `service/mercado` → `persistence/mercado`, sobre las entidades de `model/mercado`. Entre las capas funcionales, las dependencias van hacia abajo: los controllers no usan repositorios, y el modelo no depende de controller, service, persistence ni DTOs. El test `ArquitecturaTest` hace cumplir estas reglas automáticamente.

Los tests siguen la misma estructura: `unit/<capa>/<área>`, `integration/<área>` y `e2e/<área>`.

## Observabilidad

### Auditoría

Cada llamada a la API deja una línea en `logs/auditoria.log` (dentro de `formula11/backend`), con fecha, correlation ID, usuario (o `anónimo`), operación, parámetros, duración en ms y resultado. Las contraseñas y los tokens se enmascaran con `****`. El archivo rota por día y guarda 30 días; la carpeta se cambia con `APP_AUDITORIA_DIRECTORIO`.

### Actuator

| Endpoint | Quién lo ve |
|---|---|
| `/actuator/health` | cualquiera; sin superusuario muestra solo `UP` o `DOWN` |
| `/actuator/info` (nombre y versión), `/actuator/metrics`, `/actuator/prometheus`, `/actuator/loggers` | solo el superusuario (sin sesión 401, usuario común 403) |

El health tiene un componente propio, `cargaMetricas`. Da `DOWN` (y el health responde 503) si la última carga de métricas de WhoScored terminó fallida, porque el recálculo de la semana usaría datos viejos. Una carga parcial no lo baja. Con `POST /actuator/loggers/AUDITORIA` y `{"configuredLevel":"WARN"}` se apaga la auditoría en caliente; se vuelve con `"INFO"`.

### Métricas de negocio

- `formula11_mercado_operaciones_total{tipo="compra|venta|p2p"}`: operaciones de mercado confirmadas. Los tres tipos están desde el arranque; el P2P lo registra su feature con `MetricasNegocio.registrarOperacionMercado(TipoOperacionMercado.P2P)`.
- `formula11_cotizaciones_recalculo_seconds{resultado="ok|error"}`: duración de cada recálculo de cotizaciones, manual o del job semanal.

Prometheus local (opcional), con la app corriendo en el puerto 8080:

```powershell
Set-Content -NoNewline -Encoding ascii formula11\backend\docker\prometheus\token "<token del superusuario>"
```

```powershell
docker compose -f formula11\backend\docker\prometheus\docker-compose.yml up -d
```

Se abre en `http://localhost:9090`. El token vence en una hora: se reemplaza el archivo y Prometheus lo toma en la lectura siguiente. El archivo `token` no se versiona.
