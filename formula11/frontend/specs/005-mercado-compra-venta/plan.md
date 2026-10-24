# Implementation Plan: Mercado — compra y venta de tokens

**Branch**: `005-mercado-compra-venta` | **Spec**: [spec.md](./spec.md)

## Technical Context

Apilada sobre 003-004: el panel vive en el detalle del jugador (003); desde el ranking (004) se llega al detalle. El precio y la disponibilidad salen de `/jugadores/{id}/mercado`, no de las cotizaciones. Sin dependencias nuevas.

## Estructura

```text
src/api/endpoints.js                       # mercadoJugador(id), comprar, vender, portfolio(usuarioId)
src/components/mercado/mercado.js          # normalizarPortfolio, normalizarMercado, tenenciaDe, validarOperacion, describirResultado
src/components/mercado/PanelOperacion.jsx  # formulario comprar/vender
src/hooks/useMercado.js                    # portfolio + mercado + operar() + recarga tras operar
src/pages/DetalleJugadorPage.jsx           # columna lateral: ficha + panel; lee ?operacion=
src/test/handlers.js                       # portfolio y órdenes por defecto
tests/pages/mercado.test.jsx
tests/components/mercado.test.js
```

## Decisiones

- **Recarga del portfolio y de `/mercado`** tras cada operación exitosa (contador `version` en el efecto), en lugar de calcular el saldo en el cliente: el backend es la fuente de verdad.
- **`useMercado` antes de los returns tempranos** del detalle, para respetar las reglas de hooks.
- **Validación al tocar o enviar**: el error se muestra tras salir del campo o al enviar, no mientras se escribe la primera vez.
- **`?operacion=`** preselecciona el tipo; sirve para la compra en un click (008) sin crear otra pantalla.
- **Portfolio y ordenes en endpoints separados** de `useDetalleJugador`: un error del portfolio no oculta la ficha ni el gráfico.
