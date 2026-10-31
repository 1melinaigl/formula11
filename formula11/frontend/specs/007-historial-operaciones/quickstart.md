# Quickstart: Historial de operaciones

Formas de datos en [contracts/frontend-api.md](./contracts/frontend-api.md) y reglas en
[data-model.md](./data-model.md).

## Prerrequisitos

- Dependencias instaladas en `formula11/frontend` (`npm install`).
- Para la validación manual: backend de la Entrega 2 en `http://localhost:8080` con un
  `USUARIO` que tenga más de 10 operaciones (algunas al precio inicial) y el
  `SUPERUSUARIO`.

## Validación automática

```bash
npm test
npm run lint
npm run build
```

Escenarios en `tests/pages/historial.test.jsx` y reglas en
`tests/components/historial.test.js`, con MSW.

## Validación manual

1. Como `USUARIO`, entrar desde "Historial" en el portfolio y desde "Ver historial"
   después de una compra.
2. Verificar fecha y hora local, "Compra"/"Venta" en texto, movimiento con signo
   (compra en rojo con "-", venta en verde con "+"), saldo final y contraparte.
3. Las compras hechas antes del primer recálculo muestran "(precio inicial)".
4. Pasar a la página 2, cambiar a "Ventas" (vuelve a la página 1) y repetir "Ventas"
   (no recarga).
5. Como `SUPERUSUARIO`, `/operaciones` muestra el aviso.
6. A 375px no hay desplazamiento horizontal.

Registrar cualquier desviación visual o contractual en `plan.md`.
