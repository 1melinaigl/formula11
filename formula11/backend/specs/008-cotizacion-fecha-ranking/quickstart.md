# Quickstart: verificación manual de la 008

## 1. Preparar datos

1. Levantar PostgreSQL y el backend con el perfil local.
2. Tener métricas cargadas (`011` o datos de prueba) y cotizaciones calculadas:

```http
POST http://localhost:8080/api/cotizaciones/recalcular
Authorization: Bearer <JWT_SUPERUSUARIO>
Content-Type: application/json

{ "desde": "2026-08-02", "hasta": "2026-10-25" }
```

3. Obtener un JWT de `USUARIO` y otro de `SUPERUSUARIO` con `POST /api/usuarios/login`.

## 2. Detalle

```http
GET http://localhost:8080/api/jugadores/{jugadorId}
Authorization: Bearer <JWT_USUARIO>
```

Verificar `cotizacionActual` con la fecha del último domingo calculado, el precio, el score y la variación.

## 3. Historial

```http
GET http://localhost:8080/api/jugadores/{jugadorId}/cotizaciones?desde=2026-09-01&hasta=2026-10-25
Authorization: Bearer <JWT_USUARIO>
```

Verificar el orden ascendente y que la primera cotización tenga variación aunque la semana anterior quede fuera del rango.

## 4. Cotización a una fecha

```http
GET http://localhost:8080/api/jugadores/{jugadorId}/cotizaciones/vigente?fecha=2026-09-17
Authorization: Bearer <JWT_USUARIO>
```

Debe devolver la cotización del domingo 2026-09-13. Con `fecha=2026-07-01` debe responder `404`.

## 5. Ranking

```http
GET http://localhost:8080/api/jugadores/ranking?liga=La%20Liga&posicion=DEL&page=0&size=10
Authorization: Bearer <JWT_USUARIO>
```

Verificar el orden por score y los puestos 1 a 10. Con `page=1`, los puestos deben empezar en 11.

## 6. Permisos por estrategia

- Con `JWT_USUARIO` y `estrategia=<la que no es la activa>`: `403`.
- Con `JWT_SUPERUSUARIO` y la misma estrategia: `200`.

## 7. Evolución del portfolio (009)

Con un usuario que tenga operaciones de demo:

```http
GET http://localhost:8080/api/usuarios/{usuarioId}/portfolio/evolucion
Authorization: Bearer <JWT_DE_ESE_USUARIO>
```

Los puntos deben caer en domingos y los valores deben reflejar precios distintos de 1.
