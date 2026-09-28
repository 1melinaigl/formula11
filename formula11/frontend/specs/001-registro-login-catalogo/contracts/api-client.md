# Frontend API Contract

El cliente usa rutas relativas `/api` y el proxy de desarrollo las dirige a
`http://localhost:8080`. No se agregan endpoints ni query parameters fuera de los contratos
OpenAPI del backend.

## Request headers

Cada request incluye:

- `X-Correlation-Id`: un UUID nuevo generado con `crypto.randomUUID()`.
- `Authorization: Bearer <token>` cuando existe una sesión autenticada.
- `Content-Type: application/json` en requests con body JSON.

El token nunca forma parte de props, texto visible, errores serializados o logs.

## Endpoints consumidos

| Operación | Request | Respuestas de éxito | Errores |
|---|---|---|---|
| `POST /api/usuarios/registro` | `{ nombre, email, password }` | `201`: `{ id, nombre, email, fechaRegistro, token }` | `400`, `409` |
| `POST /api/usuarios/login` | `{ email, password }` | `200`: `{ id, nombre, email, token }` | `400`, `401` |
| `GET /api/jugadores` | Sin body ni query params | `200`: `Jugador[]`, posiblemente vacío | `401` |

Los campos de registro respetan nombre 1-100, email válido de máximo 254 y password 8-72.
El email de login se normaliza según el contrato del backend.

## Error normalizado

Toda respuesta no exitosa se expone a las capas superiores como:

```js
{
  mensaje: string,
  timestamp: string | null,
  correlationId: string,
  status: number
}
```

`correlationId` se toma del cuerpo cuando existe; si falta, se usa el header de respuesta y,
como último respaldo, el UUID enviado en la request. La UI muestra `mensaje` y
`correlationId`, nunca credenciales.

## Unauthorized policy

- Un `401` de `GET /api/jugadores` limpia `sessionStorage`, reinicia el Context de sesión y
  dirige a la pantalla de acceso denegado.
- Un `401` de `POST /api/usuarios/login` solo devuelve el error de credenciales inválidas;
  no limpia una sesión existente ni ejecuta una redirección global.
- Registro, login y catálogo no se amplían con otros endpoints en esta entrega.
