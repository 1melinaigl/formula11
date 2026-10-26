# Data model: Portfolio e historial de operaciones

## Entidades reutilizadas

No se agregan entidades persistentes.

| Entidad | Uso en la feature | Reutilización |
|---|---|---|
| `Usuario` | propietario autenticado y superusuario | existente |
| `Cuenta` | saldo actual | existente |
| `Tenencia` | cantidad y precio promedio actuales | existente |
| `Transaccion` | fuente inmutable del historial y reconstrucción histórica | existente |
| `Jugador` | detalle de posiciones y filtro | existente |

## Resultados de dominio

Los resultados son records/clases inmutables fuera de `dto`, consumidos por el
controller:

- `PortfolioResultado`: usuario, fecha de valuación, saldo, invertido,
  valor de tenencias, ganancia, porcentaje y posiciones positivas.
- `PosicionPortfolioResultado`: jugador, cantidad, precio promedio, precio actual,
  invertido, valor actual, ganancia y porcentaje.
- `EvolucionPortfolioResultado`: usuario y lista ordenada de
  `PuntoEvolucionResultado`.
- `PuntoEvolucionResultado`: fecha, valor e invertido.
- `HistorialTransaccionesResultado`: contenido de `Transaccion` y metadatos de
  página (`page`, `size`, `totalElements`, `totalPages`).

Los montos se normalizan a escala 4 `HALF_UP`; los porcentajes a escala 2
`HALF_UP`. Ningún resultado de portfolio incluye una posición con cantidad cero.

## Consultas requeridas

`TenenciaRepository`:

- tenencias por usuario, preferentemente ordenadas por jugador/id para resultados
  deterministas;
- consulta de tenencias positivas o filtrado en service sin alterar la fila cero.

`CuentaRepository`:

- cuenta por usuario para lectura de saldo;
- la ausencia de cuenta del usuario propio produce `404` según el flujo existente.

`TransaccionRepository`:

- `Page<Transaccion>` por usuario con `tipo` y `jugadorId` opcionales;
- orden historial `fecha DESC, id DESC`;
- lista cronológica ascendente para reconstrucción de evolución;
- primera fecha del usuario para resolver el default de `desde`.

`AuditoriaMercadoRepository`:

- conservará únicamente `save`, lecturas existentes y una lectura
  `findByOperacionIdIn(Collection<Long> operacionIds)` para recuperar todas las
  auditorías de la página en una sola consulta;
- no expondrá métodos de update ni delete;
- el service leerá `despues.saldoUsuario` desde el JSONB y asociará el valor al id
  de la transacción para completar `OperacionResponse.saldoPosterior`.

Como `saldoPosterior` no es una columna de `transacciones`, la auditoría de la 007
es la fuente de verdad del saldo posterior. La consulta se limita a los ids de la
página solicitada y no requiere recorrer todo el historial ni reconstruir saldos.

Las consultas deben restringir siempre por `usuario.id`; nunca se filtra una lista
global después de cargarla.

## Puerto histórico

```java
public interface CotizacionHistoricaPort {
    List<LocalDate> fechasCotizacion(LocalDate desde, LocalDate hasta);
    Map<Long, BigDecimal> preciosA(Collection<Long> jugadorIds, LocalDate fecha);
}
```

`CotizacionHistoricaFijaAdapter` retorna lunes semanales inclusivos y precio
`1.0000` para cada id solicitado, incluidos jugadores sin una cotización real.
