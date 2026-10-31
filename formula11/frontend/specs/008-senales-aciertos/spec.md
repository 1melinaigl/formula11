# Feature Specification: Señales de jugadores subvaluados y panel de aciertos

**Feature Branch**: `008-senales-aciertos` (sale de `front-integracion`, con las features 001 a 007 ya en `dev`)
**Created**: 2026-10-31
**Status**: Draft
**Input**: Feature extra de la Entrega 2. El backend (`010-senales-subvaluados`) genera, después de cada recálculo semanal, señales de jugadores cuyo precio está por debajo de un valor estimado. El front las muestra como alertas, permite comprar en un click y muestra un panel que mide si las señales acertaron.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Recibir alertas de jugadores subvaluados (Priority: P1)

Como usuario autenticado, quiero enterarme sin buscar cuando aparecen jugadores subvaluados, para aprovechar la oportunidad antes de que su precio suba.

**Why this priority**: Es el objetivo de la feature: que el usuario vea la oportunidad sin tener que ir a buscarla.

**Independent Test**: Con una sesión iniciada y señales vigentes simuladas, la alerta aparece en cualquier pantalla. Cuando el backend devuelve una señal nueva, aparece una alerta nueva solo con esa señal.

**Acceptance Scenarios**:

1. **Given** una sesión iniciada, **When** se entra a cualquier pantalla, **Then** el front consulta las señales vigentes y vuelve a consultarlas cada 30 segundos mientras la sesión siga abierta.
2. **Given** señales vigentes que el usuario todavía no vio en esta sesión, **When** llega la respuesta, **Then** aparece una alerta: "N oportunidades nuevas", con las 3 de mayor brecha (jugador, tipo y brecha) y las acciones "Ver señales" y "Cerrar".
3. **Given** una alerta visible, **When** el usuario la cierra (botón "Cerrar" o `Escape`) o pulsa "Ver señales", **Then** esas señales quedan marcadas como vistas y no vuelven a generar alerta en esta sesión.
4. **Given** que una consulta posterior trae señales que ya se vieron, **Then** no aparece ninguna alerta nueva.
5. **Given** que una consulta periódica falla (red, `5xx`), **Then** no se muestra ningún error ni alerta y se reintenta a los 30 segundos. Un `401` limpia la sesión y corta las consultas.
6. **Given** que el usuario cierra la sesión, **Then** se cortan las consultas y se olvidan las señales vistas.

---

### User Story 2 - Ver las señales y comprar en un click (Priority: P1)

Como usuario autenticado, quiero ver la lista de señales con su detalle y comprar un token del jugador sin salir de la lista.

**Why this priority**: Sin la lista y la compra, la alerta no lleva a ninguna acción.

**Independent Test**: Simular señales vigentes e históricas, navegar a `/senales`, alternar entre vigentes e historial y comprar un token desde una fila, verificando la confirmación y los errores `422`, `409` y `403`.

**Acceptance Scenarios**:

1. **Given** el menú principal con sesión, **When** se elige "Señales", **Then** se navega a `/senales`, que muestra por defecto las señales vigentes (las del último recálculo).
2. **Given** señales, **Then** la tabla muestra, en el orden que manda el backend: jugador con link a su detalle, equipo, liga, posición, tipo ("Entre estrategias" o "Precio atrasado"), brecha (por ejemplo "+18,00%"), precio, precio estimado y semana ("al dd/mm/aaaa").
3. **Given** la vista "Historial", **When** se elige, **Then** se muestran todas las señales, de la más nueva a la más vieja, y se vuelve a la página 1. "Vigentes" vuelve a las del último recálculo.
4. **Given** más de una página, **Then** aparece la paginación (20 por página, "Página N de M") y se navega con Anterior/Siguiente conservando la vista elegida. Con una sola página no se muestra.
5. **Given** un usuario con rol `USUARIO`, **When** pulsa "Comprar 1 token" en una fila, **Then** se compra 1 token de ese jugador al precio vigente, el botón queda deshabilitado mientras la compra está en curso y aparece una confirmación con el jugador, el total pagado y el saldo final, más un link "Ver historial".
6. **Given** una compra rechazada, **Then** se muestra el `mensaje` del backend con su `correlationId`: saldo insuficiente (`422`), sin tokens disponibles (`409`) o un error de validación (`400`). La fila sigue disponible para reintentar.
7. **Given** un `SUPERUSUARIO`, **Then** ve las señales pero no el botón de compra, porque el superusuario no opera.

---

### User Story 3 - Ver si las señales acertaron (Priority: P2)

Como usuario autenticado, quiero saber si las señales de semanas anteriores acertaron, para decidir cuánto confiar en las nuevas.

**Why this priority**: Le da credibilidad a la feature, pero las alertas y la compra funcionan sin el panel.

**Independent Test**: Simular la respuesta de aciertos con varias semanas, con una sola y vacía, y verificar los indicadores, la tabla y el estado vacío.

**Acceptance Scenarios**:

1. **Given** la pantalla de señales, **Then** incluye el panel "Aciertos" con dos indicadores: tasa de acierto (porcentaje de señales cuyo jugador subió más que el mercado la semana siguiente) y diferencia promedio contra el mercado (en puntos porcentuales, con signo).
2. **Given** semanas medidas, **Then** una tabla muestra por semana, de la más vieja a la más nueva: fecha, cantidad de señales, variación promedio de los señalados, variación promedio del mercado y aciertos ("17 de 23"). Las variaciones positivas se muestran en verde con "+" y las negativas en rojo con "-".
3. **Given** que todavía no hay semanas medidas, **Then** el panel muestra "Todavía no hay semanas para medir: las señales se evalúan una semana después de generarse." y no muestra los indicadores en 0.

---

### Edge Cases

- **Sin señales vigentes**: la lista muestra "No hay señales esta semana" y no aparece ninguna alerta. Es un estado válido, no un error.
- **Página fuera de rango** (la respuesta llega vacía con `totalElements` mayor que 0): se vuelve una sola vez a la última página válida, sin entrar en un bucle.
- **Mismo jugador con dos señales** en la misma semana (una de cada tipo): se muestran como dos filas y cuentan como dos en la alerta.
- **Respuestas desordenadas**: si se cambia de vista o de página con una consulta pendiente, la respuesta anterior no reemplaza a la vigente. Una consulta periódica no pisa la página que el usuario está mirando.
- **Varias pestañas**: cada pestaña tiene su propia sesión y su propio registro de señales vistas.
- **Brecha muy grande** (precio en el piso de 0,10): se muestra tal cual llega, sin redondear a un tope.
- **Tipo desconocido** (un valor que no es `ENTRE_ESTRATEGIAS` ni `PRECIO_ATRASADO`): la señal se descarta sin mostrarse.
- **Sin sesión**: `/senales` muestra el acceso denegado sin consultar la API, y no hay consultas periódicas.
- **Error al cargar la lista o el panel**: se muestra el `mensaje` con su `correlationId`. Un error en el panel no oculta la lista y viceversa.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Las rutas viven en `src/api/endpoints.js` (`senales` y `aciertos`) y se consumen a través del cliente de API y de hooks. Los componentes no hacen `fetch`.
- **FR-002**: La consulta periódica pide `GET /api/senales` con `vigentes=true`, `page=0` y `size=100`, cada 30 segundos desde que hay sesión hasta que se cierra. Solo puede haber una consulta periódica activa a la vez.
- **FR-003**: Las señales vistas se recuerdan por `id` durante la sesión de la pestaña, con la misma estrategia de almacenamiento que la sesión. Se borran al cerrar la sesión o ante un `401`.
- **FR-004**: La alerta es una región anunciada a lectores de pantalla (`role="status"`), visible sobre cualquier pantalla, sin robar el foco. Se cierra con "Cerrar" o con `Escape` cuando tiene el foco.
- **FR-005**: La lista pide `GET /api/senales` con `vigentes` (`true` para Vigentes, `false` para Historial), `page` y `size=20`. El orden lo fija el backend (fecha descendente, brecha descendente) y el front no lo cambia.
- **FR-006**: La compra en un click usa `POST /api/ordenes/compra` con `{ jugadorId, cantidad: 1 }`, igual que el panel de operación de la feature 005. Solo la ofrece a una sesión con rol `USUARIO` (una sesión sin `rol` cuenta como `USUARIO`).
- **FR-007**: El panel pide `GET /api/senales/aciertos` una vez al entrar a `/senales`. Los porcentajes se muestran con dos decimales, coma decimal y el símbolo "%" (`es-AR`); los precios y montos, con dos decimales y la unidad "créditos".
- **FR-008**: El tipo se muestra como texto legible: `ENTRE_ESTRATEGIAS` → "Entre estrategias", con la ayuda "Otra estrategia lo valora más"; `PRECIO_ATRASADO` → "Precio atrasado", con la ayuda "Jugadores con rendimiento parecido valen más".
- **FR-009**: Las fechas (`date`) se muestran como "dd/mm/aaaa" sin correrse de día por la zona horaria.
- **FR-010**: Hasta 1024px se ocultan de la lista el equipo, el precio estimado y la ayuda del tipo, y el botón dice solo "Comprar" (su nombre accesible sigue siendo "Comprar 1 token de {jugador}"). Hasta 760px se ocultan además la liga y la insignia de posición, y la variación del mercado del panel. Si la tabla no entra, scrollea dentro de su contenedor: a 375px el ancho del documento no supera el de la ventana.
- **FR-011**: Las tablas tienen `caption` ("Señales de jugadores subvaluados" y "Aciertos por semana") y encabezados de columna. Vigentes e Historial forman un grupo con nombre ("Ver señales") cuyos botones exponen `aria-pressed`. Todo se puede usar con teclado.
- **FR-012**: El menú principal agrega "Señales" (subtítulo "Jugadores subvaluados") para cualquier sesión, incluida la del `SUPERUSUARIO`. Sin sesión no se muestra.
- **FR-013**: El JWT no se muestra en la UI ni en los errores, y un `401` en cualquiera de las consultas limpia la sesión y muestra el acceso denegado.

### Contrato (`backend/specs/005-contratos-entrega2/contracts/openapi.yaml`)

- `GET /api/senales?vigentes=&page=&size=` → `SenalesPageResponse` (página de `Senal`). `401` sin JWT; `400` para `page` o `size` inválidos.
- `Senal { id, jugador: JugadorResponse, fecha (date), tipo: ENTRE_ESTRATEGIAS | PRECIO_ATRASADO, brecha (porcentaje), precio, precioEstimado }`.
- `GET /api/senales/aciertos` → `AciertosResponse { semanas: [{ fecha, senales, variacionSenalados, variacionMercado, aciertos }], tasaAcierto, diferenciaPromedio }`. Sin semanas medibles devuelve `semanas` vacía y los dos indicadores en 0.
- `POST /api/ordenes/compra` → `201 OperacionResponse`; `400`, `401`, `403` (superusuario), `404`, `409` (sin tokens disponibles), `422` (saldo insuficiente).
- No se agregan endpoints ni campos: todo sale del contrato.

### Key Entities

- **Señal**: oportunidad detectada para un jugador en una semana. Tiene tipo, brecha, precio y precio estimado.
- **Semana medida**: resultado de las señales de una semana comparado con el mercado en la semana siguiente.
- **Señales vistas**: ids de señales que el usuario ya vio en la sesión de la pestaña. Solo existen en el front.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Una señal nueva aparece como alerta en a lo sumo 30 segundos desde que el backend la publica, en cualquier pantalla de la app.
- **SC-002**: La misma señal nunca genera dos alertas en la misma sesión.
- **SC-003**: Desde la alerta, un usuario compra un token de un jugador señalado en como máximo 3 acciones: "Ver señales", "Comprar 1 token" y leer la confirmación.
- **SC-004**: Los valores del panel de aciertos coinciden exactamente con los que devuelve el backend, con el formato de FR-007.
- **SC-005**: Con la base de la demo (hasta 40 señales vigentes, cientos en el historial y varias semanas medidas), la lista y el panel se muestran completos y sin errores.
- **SC-006**: Build, lint y tests pasan. Los tests cubren alerta nueva, alerta repetida, consulta fallida, `401`, lista vacía, historial, compra exitosa, `422`, `409`, superusuario y panel vacío.

## Assumptions

- **Referencia visual**: el mockup `design/formula11-mockup-retro-v3.html` no tiene una pantalla de señales ni alertas. Se reusan los paneles, tablas, botones, estados y colores de ganancia y pérdida que ya existen (portfolio, ranking, historial). La desviación se registra en el plan, como pide la constitución.
- **Cantidad de la compra**: un click compra 1 token. Para comprar más, el link del jugador lleva al detalle, donde está el panel de operación de la 005.
- **Alertas al iniciar sesión**: la primera consulta de la sesión trata todas las señales vigentes como nuevas, así que se ven en una sola alerta agrupada y no una por señal.
- **Consultas periódicas**: siguen activas aunque la pestaña quede en segundo plano. Como son cada 30 segundos y la respuesta es chica, no se optimiza.
- **Saldo en otras pantallas**: después de una compra desde la lista, el detalle y el portfolio muestran el saldo nuevo la próxima vez que se abren, como ya pasa con el resto de la app.
- **Backend**: la `010` y el recálculo de la temporada ya están en `dev`, y la base de la demo tiene señales y semanas medidas.
