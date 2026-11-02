# Data Model: Mercado de tokens

## Usuario

Entidad existente `usuarios`, extendida con:

- `rol`: `USUARIO` o `SUPERUSUARIO`, obligatorio, con default/backfill `USUARIO`.
- El email continúa siendo único normalizado.
- Un único usuario con `SUPERUSUARIO` actúa como contraparte. Es una fila normal de `usuarios`: puede loguearse y administrar estrategias.

Relaciones: un `USUARIO` tiene exactamente una `Cuenta`; el `SUPERUSUARIO` no tiene ninguna. Todos pueden tener `Tenencia`; `Transaccion` referencia al usuario que opera y al superusuario como contraparte; `AuditoriaMercado` referencia al autor.

## Cuenta

Tabla `cuentas`, una fila por usuario con rol `USUARIO`:

- `id`: identificador.
- `usuario_id`: FK única a `usuarios`.
- `saldo`: `NUMERIC(19,4)`, no nulo, `CHECK (saldo >= 0)`.

Reglas:

- Registro nuevo: saldo inicial `mercado.saldo-inicial` (por defecto `100.0000`).
- Usuarios existentes: backfill de `100.0000` en V25 (V21 solo crea la tabla).
- El superusuario **no tiene cuenta**: sus créditos no existen ni se registran. Una compra solo descuenta el saldo del usuario; una venta solo lo acredita.

## Tenencia

Tabla `tenencias`, una fila por `(usuario_id, jugador_id)`:

- `id`: identificador.
- `usuario_id`: FK a `usuarios`.
- `jugador_id`: FK a `jugadores`.
- `cantidad`: entero, `CHECK (cantidad >= 0)`.
- `precio_promedio`: `NUMERIC(19,4)`, no negativo.
- Restricción única `(usuario_id, jugador_id)`.

Reglas:

- Cada jugador tiene 100 tokens emitidos.
- **Los tokens del superusuario sí se guardan**: la tenencia del superusuario es el stock disponible. Sin ella no se puede validar la disponibilidad al comprar ni devolver `tokensDisponibles` en `GET /api/jugadores/{jugadorId}/mercado`.
- La tenencia inicial del superusuario (100 tokens, precio promedio 1) la crea `SuperusuarioInitializer` al arrancar, de forma idempotente, para cada jugador sin tenencia del superusuario. No hay migración para esto: Flyway corre antes de que exista el superusuario.
- Un jugador creado después por la sincronización recibe su tenencia de 100 tokens del superusuario en la misma transacción.
- La compra reduce la tenencia del superusuario y aumenta la del usuario; la venta invierte la transferencia.
- Una tenencia vendida por completo permanece con `cantidad = 0`.
- Una compra posterior actualiza el precio promedio mediante promedio ponderado; una venta no lo modifica.
- Invariante: para cada jugador, la suma de `cantidad` de todas las tenencias es 100.

## Transaccion

Tabla `transacciones`:

- `id`: identificador.
- `tipo`: `COMPRA` o `VENTA`.
- `usuario_id`: usuario que ejecutó la orden (siempre rol `USUARIO`).
- `jugador_id`: jugador negociado.
- `cantidad`: entero positivo.
- `precio_unitario`: `NUMERIC(19,4)`.
- `total`: `NUMERIC(19,4)`.
- `contraparte_id`: FK al superusuario.
- `fecha`: `TIMESTAMPTZ`, tomada del `Clock`.
- `estrategia`: nullable.
- `version_cotizacion`: nullable.

Cada fila representa una operación confirmada; no se actualiza ni elimina desde el flujo de mercado.

## AuditoriaMercado

Tabla `auditoria_mercado` append-only:

- `id`: identificador.
- `autor_id`: FK a `usuarios` (el usuario que operó).
- `operacion_id`: FK a `transacciones`.
- `antes`: snapshot JSON (`jsonb`) del estado anterior.
- `despues`: snapshot JSON (`jsonb`) del estado posterior.
- `timestamp`: `TIMESTAMPTZ`, tomado del `Clock`.

Cada snapshot registra los dos lados de la operación:

```json
{
  "saldoUsuario": 100.0000,
  "tokensUsuario": 0,
  "tokensSuperusuario": 100
}
```

Los créditos del superusuario no aparecen porque no existen.

Reglas: solo inserción, sin endpoint ni repository de update/delete. La fila se crea dentro de la misma transacción que la operación.

## Cotizacion vigente

Value object/record interno del service:

- `precio`: `BigDecimal` escala 4.
- `estrategia`: nullable.
- `version`: nullable.
- `fechaCotizacion`: nullable.

`CotizacionVigentePort` recibe jugador y fecha de operación y devuelve esta cotización. `CotizacionFijaAdapter` devuelve precio `1.0000`, sin estrategia, versión ni fecha.

## Migraciones (V20-V25)

| Versión | Contenido |
|---------|-----------|
| `V20__agregar_rol_usuario.sql` | Columna `rol` con `CHECK`, backfill `USUARIO`, índice único parcial `CREATE UNIQUE INDEX ux_usuarios_superusuario ON usuarios (rol) WHERE rol = 'SUPERUSUARIO'` |
| `V21__crear_cuentas.sql` | Solo la estructura: tabla `cuentas` con FK única y `CHECK (saldo >= 0)`. No inserta filas |
| `V22__crear_tenencias.sql` | Tabla `tenencias` con FKs, única `(usuario_id, jugador_id)`, `CHECK (cantidad >= 0)`, índice por `jugador_id` |
| `V23__crear_transacciones.sql` | Tabla `transacciones` con FKs, `CHECK` de tipo y cantidad, índice `(usuario_id, fecha DESC)` para el historial de la 009 |
| `V24__crear_auditoria_mercado.sql` | Tabla `auditoria_mercado` con FKs |
| `V25__backfill_cuentas_existentes.sql` | Una cuenta de `100.0000` para cada usuario `USUARIO` existente |

Cada migración incluye sus constraints e índices. V26-V29 quedan libres dentro del rango de B.

Responsabilidad única de las cuentas: V21 crea la tabla, V25 crea las cuentas de los usuarios que ya existían y `UsuarioService` crea la de cada registro nuevo.

## Sincronización y jugadores legacy

`SincronizacionService.sincronizar()` hoy ejecuta `deleteAllByOrigen("LEGACY")`. Como el superusuario tiene tenencia de todos los jugadores, ese borrado fallaría por la FK de `tenencias`. Política:

1. Dentro de la misma transacción de la sincronización, buscar los jugadores `LEGACY` sin transacciones.
2. Borrar la tenencia del superusuario de esos jugadores (es la única posible: sin transacciones, ningún usuario tiene tokens) y después los jugadores.
3. Los jugadores `LEGACY` con transacciones se conservan con sus tenencias e historial.
4. Cada jugador nuevo que crea la sincronización recibe su tenencia de 100 tokens del superusuario.

En la práctica la sincronización se corre al preparar la base, antes de que haya operaciones, así que se borran todos los legacy.

## Flujos transaccionales

### Compra

1. Resolver usuario autenticado y jugador (`404` si el jugador no existe).
2. Si el usuario es `SUPERUSUARIO`, rechazar (`403`).
3. Obtener precio mediante `CotizacionVigentePort` con la fecha del `Clock`.
4. Bloquear (`SELECT ... FOR UPDATE`), en este orden: cuenta del usuario, tenencia del superusuario del jugador, tenencia del usuario del jugador.
5. Validar saldo (`422`) y stock del superusuario (`409`).
6. Capturar el snapshot `antes`.
7. Descontar el total del saldo del usuario, transferir tokens del superusuario al usuario y recalcular el precio promedio.
8. Persistir transacción y auditoría (`despues`).
9. Confirmar y construir `OperacionResponse`.

### Venta

1. Resolver usuario autenticado y jugador (`404`).
2. Si el usuario es `SUPERUSUARIO`, rechazar (`403`).
3. Obtener precio vigente.
4. Bloquear en el mismo orden que la compra.
5. Validar la tenencia del vendedor (`422`).
6. Capturar el snapshot `antes`.
7. Acreditar el total al saldo del usuario y devolver los tokens al superusuario, sin cambiar el precio promedio.
8. Persistir transacción y auditoría (`despues`).
9. Confirmar y construir `OperacionResponse`.
