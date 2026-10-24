# Implementation Plan: Mercado — compra y venta de tokens

**Branch**: `005-mercado-compra-venta` | **Date**: 2026-10-24 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/005-mercado-compra-venta/spec.md`

## Summary

Permitir que un `USUARIO` compre y venda tokens de un jugador desde su detalle, con el
precio y la disponibilidad de `GET /api/jugadores/{id}/mercado`, el saldo y la tenencia
del portfolio de la sesión, y las órdenes `POST /api/ordenes/compra|venta` del contrato
de Entrega 2. La base ya existe (panel "Operar", hook `useMercado`, helpers y tests);
este plan cubre lo que la revisión del spec agregó: ocultar el panel al `SUPERUSUARIO`,
bloquear por completo el formulario mientras hay una orden en curso (hoy cambiar de
tipo o de cantidad rehabilita el envío), el mensaje de jugador agotado y el botón
"Reintentar" que vuelve a consultar los datos sin reenviar la orden.

## Technical Context

**Language/Version**: JavaScript ES modules, React 19, Vite 8.

**Primary Dependencies**: React Router 7; MSW 2, Vitest 5, React Testing Library y
user-event para pruebas. Sin dependencias nuevas.

**Storage**: N/A. La sesión existente guarda `{ token, usuario }` (incluido `rol`) en
`sessionStorage`; el JWT no se muestra en la UI.

**Testing**: Vitest + React Testing Library + MSW con `server.use(...)` por caso;
`npm test`, `npm run lint` y `npm run build`.

**Target Platform**: Navegadores soportados por el frontend Vite; layout responsive.

**Project Type**: Aplicación web frontend (SPA).

**Performance Goals**: Sin metas nuevas; el detalle sigue cargando Recharts en un
chunk diferido y el panel no agrega dependencias.

**Constraints**: Toda llamada pasa por `solicitarProtegida`; el portfolio se pide solo
con el `usuario.id` de la sesión; el precio mostrado es referencial y la confirmación
usa `OperacionResponse`; cantidad entre 1 y 100 por orden; no se reenvía ni reintenta
una orden automáticamente.

**Scale/Scope**: Un panel en el detalle del jugador, dos órdenes (compra y venta) y dos
consultas de datos (portfolio y `/mercado`).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Fidelidad visual verificable**: PASS con desviación justificada. El panel
  reutiliza `player-card`, `filter-chip` y `btn`. El detalle del jugador (003) pasa a
  una columna a los **820px** en lugar del corte general de 760px, porque la ficha, el
  panel y el gráfico en dos columnas no entran entre 761 y 820px sin desbordar; el panel
  hereda ese corte. Por debajo de 760px se mantienen las reglas del mockup.
- **II. Contratos del backend como autoridad**: PASS. Fuente:
  `backend/specs/005-contratos-entrega2/contracts/openapi.yaml`, autorizada por la
  constitución v1.1.0 para las features 003 en adelante. Ocultar el panel al
  `SUPERUSUARIO` es una decisión de UX; no se inventa un código de error que el
  contrato no define para las órdenes.
- **III. Capas y límites explícitos**: PASS. `PanelOperacion` presenta, `useMercado`
  coordina estado y llamadas, `mercado.js` normaliza y valida, y `clienteApi` transporta.
- **IV. Validación, seguridad y trazabilidad**: PASS. Validaciones de cantidad del
  lado del cliente sin reemplazar las del backend; errores en español con
  `correlationId`; `401` limpia la sesión; el JWT no se expone.
- **V. Calidad accesible y comprobable**: PASS. Selector con `aria-pressed`, campo con
  label, `aria-invalid` y `aria-describedby`, confirmación con `role="status"`. Los
  tests existentes del mercado se conservan; solo se agregan casos nuevos.

**Re-check post-design**: PASS. El diseño de Phase 1 no agrega capas, dependencias ni
endpoints fuera del contrato.

## Project Structure

### Documentation (this feature)

```text
specs/005-mercado-compra-venta/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── frontend-api.md
├── checklists/
│   └── api.md
└── tasks.md
```

### Source Code (repository root)

```text
src/
├── api/
│   ├── clienteApi.js            # transporte, X-Correlation-Id, 401
│   └── endpoints.js             # mercadoJugador, comprar, vender, portfolio
├── components/
│   └── mercado/
│       ├── mercado.js           # normalizar, validar y describir operaciones
│       └── PanelOperacion.jsx   # formulario Comprar/Vender
├── hooks/
│   └── useMercado.js            # portfolio + /mercado + operar + reintento
├── pages/
│   └── DetalleJugadorPage.jsx   # panel solo para USUARIO; lee ?operacion=
└── test/
    └── handlers.js              # respuestas por defecto del contrato

tests/
├── components/
│   └── mercado.test.js
└── pages/
    └── mercado.test.jsx
```

**Structure Decision**: Se mantiene la estructura única del frontend. La feature vive
dentro del detalle del jugador (003) y no agrega rutas.

## Complexity Tracking

No aplica: no hay violaciones de la constitución.
