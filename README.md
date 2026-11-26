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
