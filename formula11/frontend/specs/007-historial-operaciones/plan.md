# Implementation Plan: Historial de operaciones

**Branch**: `007-historial-operaciones` | **Date**: 2026-10-24 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/007-historial-operaciones/spec.md`

## Summary

Mostrar en `/operaciones` las compras y ventas del `USUARIO` con
`GET /api/usuarios/{id}/transacciones` (páginas de 10, filtro por tipo), con fecha y hora
local, movimiento en el saldo con signo, saldo final y contraparte. La pantalla ya
existe; este plan cubre lo que agregaron la aclaración y la revisión: el superusuario
sin historial (aviso y sin consulta), la marca "(precio inicial)" para operaciones con
`estrategia` en null, y la recuperación de una página fuera de rango. El filtro por
jugador queda fuera de alcance.

## Technical Context

**Language/Version**: JavaScript ES modules, React 19, Vite 8.

**Primary Dependencies**: React Router 7; MSW 2, Vitest 5, React Testing Library y
user-event para pruebas. Sin dependencias nuevas.

**Storage**: N/A. La sesión existente guarda `{ token, usuario }` (incluido `rol`).

**Testing**: Vitest + React Testing Library + MSW con `server.use(...)` por caso;
`npm test`, `npm run lint` y `npm run build`.

**Target Platform**: Navegadores soportados por el frontend Vite; layout responsive.

**Project Type**: Aplicación web frontend (SPA).

**Performance Goals**: Una consulta por cambio de página o filtro; ninguna si el valor
no cambia.

**Constraints**: Orden del backend sin reordenar; solo `tipo` como filtro; ruta con el
`usuario.id` de la sesión; fechas `date-time` en hora local de 24 h.

**Scale/Scope**: Una pantalla con tabla paginada (10 por página) y tres filtros.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Fidelidad visual verificable**: PASS. Reutiliza la tabla del ranking, los
  filtros con `filter-chip` y la paginación del catálogo; corte en 760px. La marca de
  precio inicial queda oculta en celular junto con la columna de precio (limitación
  aceptada en el spec).
- **II. Contratos del backend como autoridad**: PASS. Fuente:
  `backend/specs/005-contratos-entrega2/contracts/openapi.yaml` (constitución v1.1.0).
  No se envía `sort`, que el contrato no define; `jugadorId` existe pero queda sin uso.
- **III. Capas y límites explícitos**: PASS. `HistorialPage` compone, `useHistorial`
  coordina la consulta paginada, `historial.js` normaliza y formatea.
- **IV. Validación, seguridad y trazabilidad**: PASS. Errores en español con
  `correlationId`; `401` limpia la sesión; el JWT no se expone.
- **V. Calidad accesible y comprobable**: PASS. Tabla con `caption` y encabezados;
  filtros con `aria-pressed`; tipo como texto y signo en el movimiento. Los tests
  existentes se conservan.

**Re-check post-design**: PASS.

## Project Structure

### Documentation (this feature)

```text
specs/007-historial-operaciones/
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
├── api/endpoints.js                          # transacciones(usuarioId)
├── components/
│   ├── catalogo/PaginacionCatalogo.jsx       # paginación reutilizada (prop etiqueta)
│   └── historial/
│       ├── historial.js                      # normalizar, formatear, consulta, efecto en saldo
│       └── TablaHistorial.jsx
├── hooks/useHistorial.js                     # consulta paginada, filtro, página fuera de rango
└── pages/HistorialPage.jsx                   # /operaciones, aviso para el superusuario

tests/
├── components/historial.test.js
└── pages/historial.test.jsx
```

**Structure Decision**: Estructura única del frontend; la feature agrega la ruta
`/operaciones` (sin carga diferida: no usa gráficos).

## Complexity Tracking

No aplica: no hay violaciones de la constitución.
