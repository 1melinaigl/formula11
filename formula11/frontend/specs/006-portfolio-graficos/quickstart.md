# Quickstart: Portfolio y gráficos del usuario

Formas de datos en [contracts/frontend-api.md](./contracts/frontend-api.md) y reglas en
[data-model.md](./data-model.md).

## Prerrequisitos

- Dependencias instaladas en `formula11/frontend` (`npm install`).
- Para la validación manual: backend de la Entrega 2 en `http://localhost:8080` con un
  `USUARIO` que tenga tenencias y varias semanas de cotizaciones (backfill), y el
  `SUPERUSUARIO`.

## Validación automática

```bash
npm test
npm run lint
npm run build
```

Escenarios en `tests/pages/portfolio.test.jsx` y reglas en
`tests/components/portfolio.test.js`, con MSW.

## Validación manual

1. Como `USUARIO`, abrir "Mi portfolio" desde el menú: "Valuado al ...", las cinco
   tarjetas con los totales del backend y el valor total = saldo + valor de los tokens.
2. Verificar que la tabla y el gráfico de composición tienen el mismo orden (mayor valor
   primero) y que "Vender" abre el detalle con la venta elegida.
3. Con una ganancia negativa, la tarjeta dice "Pérdida" y la barra es roja con signo menos.
4. Con una sola semana de cotizaciones, en lugar del gráfico de evolución aparece el aviso
   de poca historia.
5. Con el backend devolviendo error en la evolución, se ve el error en ese lugar y el
   resto de la página sigue visible.
6. Con un lector de pantalla, la tabla "Evolución semanal" lee fecha, valor e invertido.
7. Como `SUPERUSUARIO`, el menú no muestra "Mi portfolio" y `/portfolio` muestra el aviso.
8. A 375px no hay desplazamiento horizontal y la tabla oculta las columnas opcionales.

Registrar cualquier desviación visual o contractual en `plan.md`.
