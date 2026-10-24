# Feature Specification: Portfolio y gráficos del usuario

**Feature Branch**: `006-portfolio-graficos` (apilada sobre `005-mercado-compra-venta`)
**Created**: 2026-10-24
**Status**: Draft
**Input**: Plan de desarrollo Entrega 2, track C: "006-portfolio-graficos (visualización de datos del usuario)". Documento de visión §3.4: cantidad de tokens por jugador, precio promedio, valor actual, ganancia o pérdida. El plan sugiere tres gráficos: evolución del valor contra lo invertido, composición por jugador y ganancia o pérdida por jugador.

## User Scenarios & Testing

### User Story 1 - Ver mi posición (Priority: P1)

Como usuario autenticado, quiero ver cuánto vale mi portfolio y cuánto gané o perdí.

**Acceptance Scenarios**:

1. **Given** el menú principal, **When** elijo "Mi portfolio", **Then** navego a `/portfolio` y el encabezado muestra "Mi portfolio".
2. **Given** tenencias, **Then** veo valor total (saldo + tokens), saldo, invertido, valor de los tokens y ganancia/pérdida con su porcentaje sobre lo invertido.
3. **Given** tenencias, **Then** una tabla muestra por jugador: tokens, precio promedio, precio actual, valor, ganancia (con %) y "Vender", que abre el detalle con la venta preseleccionada (005).
4. **Given** tenencias en 0 (vendidas), **Then** no aparecen.
5. **Given** ninguna tenencia, **Then** veo el saldo y una invitación con links a ranking y catálogo; no hay gráficos.

### User Story 2 - Gráficos (Priority: P1)

1. **Evolución**: línea del valor del portfolio y línea escalonada punteada de lo invertido, mismo eje (créditos), leyenda y tooltip. Se muestra si hay al menos 2 puntos.
2. **Composición**: barras horizontales ordenadas por valor actual con el porcentaje; a partir del 8.º jugador se agrupan en "Otros (n)".
3. **Ganancia o pérdida**: barras divergentes desde 0, verde azulado si gana y rojo si pierde, con el monto con signo como etiqueta (la polaridad no depende solo del color).
4. Si falla la serie de evolución, se muestra su error y el resto de la página sigue visible.

### User Story 3 - Estados (Priority: P1)

Carga, error del portfolio con `mensaje` y `correlationId`, y sin sesión (acceso denegado, sin consultar la API).

## Requirements

- **FR-001**: Las rutas viven en `src/api/endpoints.js` (`portfolio`, `evolucionPortfolio`).
- **FR-002**: Los totales y los valores de cada posición vienen calculados por el backend; el front solo suma saldo + valor de las tenencias para el valor total y muestra la fecha de valuación.
- **FR-003**: Colores de gráficos validados con el validador de paletas contra el fondo `#06210f` (banda de luminosidad, croma, separación para daltonismo y contraste).
- **FR-004**: Sin desborde horizontal a 375px: tabla con columnas opcionales ocultas y gráficos que se achican.
- **FR-005**: La página se carga en diferido porque usa Recharts.

## Contrato (`backend/specs/005-contratos-entrega2/contracts/openapi.yaml`)

- `GET /api/usuarios/{usuarioId}/portfolio` → `PortfolioResponse { usuarioId, fechaValuacion, saldo, invertido, valorTenencias, ganancia, gananciaPorcentaje, posiciones }`, valuado con la cotización vigente. 403 si el id no es el del JWT.
- `PosicionPortfolio { jugador, cantidad, precioPromedio, precioActual, invertido, valorActual, ganancia, gananciaPorcentaje }`, una por jugador con cantidad mayor a cero. Alcanza para los gráficos de composición y de ganancia por jugador.
- `GET /api/usuarios/{usuarioId}/portfolio/evolucion[?desde=&hasta=]` → `{ usuarioId, puntos: [{ fecha, valor, invertido }] }`, un punto por fecha de cotización.
- Porcentajes como número (12.5 = 12,5 %); el front los convierte a fracción.
