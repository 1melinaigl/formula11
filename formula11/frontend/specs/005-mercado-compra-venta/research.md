# Research: Mercado — compra y venta de tokens

No quedaron puntos `NEEDS CLARIFICATION` en el Technical Context. Estas son las
decisiones de diseño y sus alternativas.

## 1. Fuente del precio y la disponibilidad

- **Decision**: `GET /api/jugadores/{jugadorId}/mercado` (`precioVigente`,
  `tokensDisponibles`, `tokensEmitidos`, `fechaCotizacion`).
- **Rationale**: El contrato lo define para "la pantalla de compra"; ya responde con la
  estrategia activa y con el precio inicial cuando no hubo recálculo.
- **Alternatives considered**: Derivar el precio de las cotizaciones del detalle
  (descartado: duplica la regla de estrategia activa y no da disponibilidad).

## 2. Saldo y tenencia

- **Decision**: `GET /api/usuarios/{usuarioId}/portfolio` con el `usuario.id` de la
  sesión; la tenencia es la posición cuyo `jugador.id` coincide.
- **Rationale**: El contrato no expone otro endpoint de saldo; el portfolio solo es
  visible para su dueño (403 si no coincide).
- **Alternatives considered**: Calcular el saldo en el cliente a partir de las órdenes
  (descartado: el backend es la fuente de verdad).

## 3. Actualización después de operar

- **Decision**: Tras un `201`, volver a pedir portfolio y `/mercado`. Si alguna falla,
  conservar la confirmación, mostrar el error y ofrecer "Reintentar", que solo repite
  esas consultas.
- **Rationale**: Mantiene la confirmación (la orden ya se ejecutó) y evita cualquier
  camino que reenvíe la orden.
- **Alternatives considered**: Ocultar la confirmación (engañoso); reenviar la orden
  para sincronizar (riesgo de compra duplicada).

## 4. Prevención del doble envío

- **Decision**: Mientras `enviando` es verdadero se deshabilitan el botón, el selector
  y el campo de cantidad, y el envío del formulario se ignora. Limpiar el resultado de
  una operación anterior no puede modificar `enviando`.
- **Rationale**: Hoy, cambiar de tipo o de cantidad llama a `limpiarOperacion()`, que
  pone `enviando` en falso y rehabilita el botón con la orden todavía en curso.
- **Alternatives considered**: Solo deshabilitar el botón (insuficiente, es el estado
  actual); un identificador de idempotencia en la orden (el contrato no lo define).

## 5. Rol del superusuario

- **Decision**: El panel se renderiza solo si `usuario.rol` es `USUARIO` (o no viene, como
  en la respuesta de registro). El `SUPERUSUARIO` ve el detalle completo sin el panel.
- **Rationale**: El superusuario es la contraparte de todas las órdenes; el contrato no
  define una respuesta específica si opera, así que la decisión queda del lado de la UX.
- **Alternatives considered**: Pantalla de acceso denegado (rompe la comparación de
  estrategias del 003); dejar el panel y esperar un error del backend (no definido).

## 6. Validación en el cliente

- **Decision**: Orden fijo de validación (vacío, entero ≥ 1, máximo 100, tenencia,
  disponibles, saldo); el mensaje aparece al salir del campo o al enviar.
- **Rationale**: Un solo mensaje claro por vez; el backend sigue validando (`409`, `422`).
- **Alternatives considered**: Mostrar todos los errores juntos (ruido); validar solo en
  el backend (peor experiencia y órdenes innecesarias).
