# Quickstart: Mercado — compra y venta de tokens

Guía para validar la feature de punta a punta. Formas de datos en
[contracts/frontend-api.md](./contracts/frontend-api.md) y reglas en
[data-model.md](./data-model.md).

## Prerrequisitos

- Node y dependencias instaladas en `formula11/frontend` (`npm install`).
- Para la validación manual: backend de la Entrega 2 corriendo en `http://localhost:8080`
  (el proxy `/api` de Vite apunta ahí) con un usuario `USUARIO` y el `SUPERUSUARIO`.

## Validación automática

```bash
npm test
npm run lint
npm run build
```

Los escenarios del mercado están en `tests/pages/mercado.test.jsx` y las reglas en
`tests/components/mercado.test.js`, con MSW.

## Validación manual

1. Ingresar como `USUARIO`, abrir el detalle de un jugador y verificar el panel "Operar":
   saldo, tokens propios, "N de 100" disponibles, precio vigente con su fecha (o
   "precio inicial") y total estimado.
2. Comprar 2 tokens: la confirmación muestra precio unitario, total y saldo; el saldo,
   la tenencia y los disponibles se actualizan.
3. Mientras la orden está en curso, intentar cambiar de tipo, editar la cantidad o
   volver a enviar: nada responde hasta que termina.
4. Vender 1 token y abrir `/jugadores/{id}?operacion=venta`: arranca en "Vender".
5. Intentar comprar más que el saldo, más de 100 o más que los disponibles: aparece el
   mensaje correspondiente y no se envía la orden.
6. Con el backend devolviendo `409` o `422`, verificar `mensaje` y `correlationId`.
7. Ingresar como `SUPERUSUARIO` y abrir el mismo detalle: se ve la ficha y las
   cotizaciones, pero no el panel "Operar".
8. Con el navegador angosto (≤ 820px), el panel queda debajo de la ficha sin desborde.

Registrar cualquier desviación visual o contractual en `plan.md`.
