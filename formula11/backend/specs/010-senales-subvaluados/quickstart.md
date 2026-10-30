# Quickstart: verificación manual de la 010

## 1. Generar señales

Con métricas cargadas, recalcular la temporada como superusuario:

```http
POST http://localhost:8080/api/cotizaciones/recalcular
Authorization: Bearer <JWT_SUPERUSUARIO>
Content-Type: application/json

{ "desde": "2026-08-10", "hasta": "2026-11-01" }
```

En el log del backend debe aparecer, por cada semana, cuántas señales de cada tipo se generaron.

## 2. Señales vigentes

```http
GET http://localhost:8080/api/senales
Authorization: Bearer <JWT_USUARIO>
```

Deben venir solo señales de la última semana, ordenadas por brecha, con a lo sumo 20 por tipo. Volver a recalcular la última semana y repetir la consulta: los ids deben ser los mismos.

## 3. Historial de señales

```http
GET http://localhost:8080/api/senales?vigentes=false&page=0&size=50
Authorization: Bearer <JWT_USUARIO>
```

## 4. Compra desde una alerta

Con el `jugador.id` de una señal, comprar con el endpoint existente:

```http
POST http://localhost:8080/api/ordenes/compra
Authorization: Bearer <JWT_USUARIO>
Content-Type: application/json

{ "jugadorId": <id>, "cantidad": 1 }
```

## 5. Panel de aciertos

```http
GET http://localhost:8080/api/senales/aciertos
Authorization: Bearer <JWT_USUARIO>
```

Debe traer una fila por semana con señales y semana siguiente calculada, sin la última semana.
