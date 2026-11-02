# Feature Specification: Portfolio y gráficos del usuario

**Feature Branch**: `006-portfolio-graficos` (apilada sobre `005-mercado-compra-venta`)
**Created**: 2026-10-24
**Status**: Draft
**Input**: Plan de desarrollo Entrega 2, track C: "006-portfolio-graficos (visualización de datos del usuario)". Documento de visión §3.4: cantidad de tokens por jugador, precio promedio, valor actual, ganancia o pérdida. El plan sugiere tres gráficos: evolución del valor contra lo invertido, composición por jugador y ganancia o pérdida por jugador.

## Clarifications

### Session 2026-10-24

- Q: ¿Qué ve el `SUPERUSUARIO` en "Mi portfolio"? → A: No tiene portfolio de inversión: el menú no le muestra "Mi portfolio" y, si entra por la URL, ve un aviso de que es la contraparte del mercado; no se consulta la API.
- Q: ¿Qué rango de fechas muestra el gráfico de evolución? → A: Toda la historia: se pide sin `desde` ni `hasta` y no hay selector de rango.
- Q: ¿Qué se muestra en lugar del gráfico de evolución cuando hay menos de 2 puntos? → A: Un aviso explicativo en el lugar del gráfico.
- Q: ¿En qué orden se listan los jugadores en la tabla "Tus tokens"? → A: De mayor a menor valor actual, igual que el gráfico de composición.

## User Scenarios & Testing

### User Story 1 - Ver mi posición (Priority: P1)

Como usuario autenticado, quiero ver cuánto vale mi portfolio y cuánto gané o perdí.

**Acceptance Scenarios**:

1. **Given** el menú principal, **When** elijo "Mi portfolio", **Then** navego a `/portfolio` y el encabezado muestra "Mi portfolio".
2. **Given** tenencias, **Then** veo valor total (`saldo` + `valorTenencias`), saldo (`saldo`), invertido (`invertido`), valor de los tokens (`valorTenencias`) y ganancia/pérdida (`ganancia`) con su porcentaje sobre lo invertido (`gananciaPorcentaje`), más "Valuado al dd/mm/aaaa" con `fechaValuacion` sobre el título. Si la ganancia es negativa, la tarjeta se titula "Pérdida", el monto lleva signo menos y el porcentaje va en rojo; si lo invertido es 0, el porcentaje se reemplaza por "Sin inversión".
3. **Given** tenencias, **Then** una tabla muestra por jugador, ordenada de mayor a menor valor actual (a igual valor, por nombre), tokens, precio promedio, precio actual, valor, ganancia (con %) y "Vender", que abre el detalle con la venta preseleccionada (005).
4. **Given** tenencias en 0 (vendidas), **Then** no aparecen (el contrato ya devuelve solo posiciones con cantidad mayor a cero; el front lo garantiza igual). Con muchas tenencias, la tabla las lista todas, sin paginar.
5. **Given** ninguna tenencia (también si ya vendió todo y hay historia de evolución), **Then** veo el resumen y una invitación con links a ranking y catálogo; no hay gráficos ni tabla.
6. **Given** un `SUPERUSUARIO`, **When** abre el menú principal, **Then** no aparece "Mi portfolio"; **When** entra a `/portfolio` por la URL, **Then** ve el aviso "El superusuario es la contraparte del mercado y no tiene un portfolio de inversión." y no se consulta el portfolio ni la evolución.

### User Story 2 - Gráficos (Priority: P1)

1. **Evolución**: línea del valor del portfolio y línea escalonada punteada de lo invertido, mismo eje (créditos), leyenda y tooltip, con toda la historia disponible (la consulta no envía `desde` ni `hasta` y no hay selector de rango). Se muestra si hay al menos 2 puntos; con 0 o 1 punto, en su lugar aparece la tarjeta "Valor del portfolio vs. invertido" con el aviso "Todavía no hay suficiente historia para mostrar la evolución (hace falta al menos una semana más de cotizaciones)."
2. **Composición**: barras horizontales con los 7 jugadores de mayor valor actual (a igual valor, por nombre), cada uno con su porcentaje sobre el valor de las tenencias; el resto se agrupa en una barra "Otros (n)" con la suma de su valor.
3. **Ganancia o pérdida**: barras divergentes desde 0, verde azulado si gana y rojo si pierde, con el monto con signo como etiqueta (la polaridad no depende solo del color).
4. Si falla la serie de evolución, se muestran su `mensaje` y `correlationId` en lugar de ese gráfico, y el resumen, la tabla y los otros dos gráficos siguen visibles. Si falla el portfolio, se muestra solo su error (sin resumen, tabla ni gráficos), aunque la evolución haya respondido. No hay reintento dentro de la página: se reintenta recargándola.
5. Cada gráfico tiene una alternativa textual: la tabla "Tus tokens" cubre composición y ganancia; la evolución incluye una tabla oculta visualmente ("Evolución semanal": fecha, valor e invertido) que leen los lectores de pantalla.

### User Story 3 - Estados (Priority: P1)

- Carga: "Cargando portfolio..." mientras se consultan portfolio y evolución.
- Error del portfolio: `mensaje` y `correlationId`.
- `401` en cualquiera de las dos consultas: se limpia la sesión y se muestra el acceso denegado.
- Sin sesión: acceso denegado, sin consultar la API.

## Requirements

- **FR-001**: Las rutas viven en `src/api/endpoints.js` (`portfolio`, `evolucionPortfolio`).
- **FR-002**: Los totales del resumen (`invertido`, `valorTenencias`, `ganancia`, `gananciaPorcentaje`) y los valores de cada fila (`invertido`, `valorActual`, `ganancia`, `gananciaPorcentaje` de `PosicionPortfolio`) se toman del backend sin recalcular. La única cuenta del front es el valor total = `saldo` + `valorTenencias`. Los porcentajes del contrato (12.5 = 12,5 %) se muestran con un decimal y signo. Montos con dos decimales y coma decimal (`es-AR`); las ganancias llevan signo.
- **FR-003**: Colores de gráficos validados con el validador de paletas contra el fondo `#06210f` (banda de luminosidad, croma, separación para daltonismo y contraste): valor `#e07210`, invertido `#2690b0`, ganancia `#1c9c8c`, pérdida `#e2414f`. La polaridad de la ganancia también se indica con el signo del monto, no solo con el color.
- **FR-004**: Sin desborde horizontal a 375px (el ancho del documento no supera el de la ventana): hasta 760px la tabla oculta precio promedio, precio actual, el porcentaje de la ganancia y la insignia de posición, y los gráficos se ajustan al ancho disponible.
- **FR-005**: La página se carga en diferido porque usa Recharts.
- **FR-006**: El portfolio es solo para el rol `USUARIO` (una sesión sin `rol` cuenta como `USUARIO`); el `SUPERUSUARIO` no ve el ítem del menú ni consulta la API.
- **FR-007**: Las rutas del portfolio y la evolución se arman solo con el `usuario.id` de la sesión; el JWT no se muestra en la UI ni en los errores.
- **FR-008**: La tabla "Tus tokens" y el gráfico de composición usan el mismo orden: mayor valor actual primero y, a igual valor, por nombre.

## Contrato (`backend/specs/005-contratos-entrega2/contracts/openapi.yaml`)

- `GET /api/usuarios/{usuarioId}/portfolio` → `PortfolioResponse { usuarioId, fechaValuacion, saldo, invertido, valorTenencias, ganancia, gananciaPorcentaje, posiciones }`, valuado con la cotización vigente. 403 si el id no es el del JWT.
- `PosicionPortfolio { jugador, cantidad, precioPromedio, precioActual, invertido, valorActual, ganancia, gananciaPorcentaje }`, una por jugador con cantidad mayor a cero. Alcanza para los gráficos de composición y de ganancia por jugador.
- `GET /api/usuarios/{usuarioId}/portfolio/evolucion[?desde=&hasta=]` → `{ usuarioId, puntos: [{ fecha, valor, invertido }] }`, un punto por fecha de cotización.
- Porcentajes como número (12.5 = 12,5 %); el front los convierte a fracción.
- La serie de evolución la implementa el backend en `009-portfolio-historial`; hasta que exista, el front se valida con MSW.

## Dependencias y fuera de alcance

- "Vender" usa el `?operacion=venta` del detalle (005).
- El historial de operaciones es la feature 007; el enlace desde esta pantalla se agrega allí.
