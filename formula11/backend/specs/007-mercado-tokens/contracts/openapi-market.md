# Contrato HTTP del mercado

La fuente normativa y completa es [`specs/005-contratos-entrega2/contracts/openapi.yaml`](../../005-contratos-entrega2/contracts/openapi.yaml). Esta referencia evita duplicar el contrato y fija el subconjunto implementado por la feature.

## Operaciones

| Método | Ruta | Respuesta exitosa | Errores funcionales |
|---|---|---|---|
| GET | `/api/jugadores/{jugadorId}/mercado` | `200 MercadoJugadorResponse` | `401`, `404` |
| POST | `/api/ordenes/compra` | `201 OperacionResponse` | `400`, `401`, `403`, `404`, `409`, `422` |
| POST | `/api/ordenes/venta` | `201 OperacionResponse` | `400`, `401`, `403`, `404`, `422` |

## OrdenRequest

```json
{
  "jugadorId": 1,
  "cantidad": 2
}
```

`jugadorId` es obligatorio; `cantidad` es un entero obligatorio entre 1 y 100. El usuario y el precio se obtienen del JWT y de `CotizacionVigentePort`, respectivamente.

## OperacionResponse

Debe incluir `id`, `tipo`, `jugador`, `cantidad`, `precioUnitario`, `total`, `saldoPosterior`, `contraparte`, `fecha`, `estrategia` y `version`. `estrategia` y `version` son nulos mientras opere la cotización fija inicial.

## Convenciones

- Todas las rutas requieren JWT.
- Los errores usan el esquema `Error`.
- Los montos se expresan en créditos con hasta cuatro decimales.
- El comprador/vendedor es el usuario autenticado.
- No se agregan aliases `/orders/buy` ni `/orders/sell`.
- El superusuario recibe `403` en compra y venta (operaría contra sí mismo). Ese `403` todavía no está en el contrato 005: B lo agrega en el PR de la rama `005-contrato-403-superusuario`, junto con el `403` del superusuario en `GET /api/usuarios/{usuarioId}/portfolio`.
