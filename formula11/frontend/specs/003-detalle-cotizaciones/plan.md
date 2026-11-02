# Implementation Plan: Detalle de jugador con evolución de cotizaciones

**Branch**: `003-detalle-cotizaciones` | **Date**: 2026-10-24 | **Spec**: [spec.md](./spec.md)

## Summary

Implementar el detalle de un jugador contra el contrato de Entrega 2: ficha, cotización
activa, historial y evolución temporal. El frontend reutiliza el cliente protegido y
sus errores normalizados, mantiene la ficha visible ante fallos parciales y restringe
la comparación de estrategias al `SUPERUSUARIO`. Para ese rol se agrega la consulta de
`GET /api/estrategias` para identificar la estrategia activa.

## Technical Context

**Language/Version**: JavaScript ES modules, React 19, Vite 8.

**Primary Dependencies**: React Router 7, MSW 2 para mocks, Vitest 5, React Testing
Library y Recharts 3 cargado de forma diferida.

**Storage**: N/A para la feature; la sesión existente usa el almacenamiento aprobado
por el contexto de sesión y el JWT no se expone en UI ni logs.

**Testing**: Vitest, React Testing Library, user-event y MSW; además de `npm run lint`
y `npm run build`.

**Target Platform**: Navegadores soportados por el frontend Vite, con layout responsive
en el corte visual de 760px.

**Project Type**: Aplicación web frontend.

**Performance Goals**: Mantener Recharts fuera del bundle inicial mediante code
splitting; el plan existente estima un bundle inicial cercano a 250 kB. No se inventa
un umbral contractual adicional.

**Constraints**: Las llamadas deben pasar por `solicitarProtegida`; los endpoints y
campos deben coincidir con Entrega 2; errores en español deben conservar `mensaje` y
`correlationId`; las fechas `YYYY-MM-DD` se formatean sin conversión de zona horaria.

**Scale/Scope**: Detalle de un jugador, hasta dos series de estrategia y su historial
completo; solo frontend contra mocks de MSW, sin cambios de backend.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Fidelidad visual verificable**: PASS. Se reutilizan los estilos existentes y se
  mantiene el corte responsive; cualquier desviación queda limitada al detalle.
- **Contratos del backend como autoridad**: PASS con dependencia explícita. Se usa el
  contrato de Entrega 2 descrito en `spec.md`; las rutas inglesas de la visión no se
  implementan.
- **Capas y límites explícitos**: PASS. Componentes presentan datos, hooks coordinan
  estado y `solicitarProtegida` centraliza transporte/autorización.
- **Validación, seguridad y trazabilidad**: PASS. Se conserva `401`, `X-Correlation-Id`,
  limpieza de sesión, rol y errores normalizados.
- **Calidad accesible y comprobable**: PASS condicionado a validación. La tabla es
  alternativa textual del gráfico, Escape vuelve al catálogo y se mantienen tests MSW.

No hay violaciones que requieran Complexity Tracking.

## Project Structure

### Documentation (this feature)

```text
specs/003-detalle-cotizaciones/
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

### Source Code

```text
src/
├── api/
│   ├── clienteApi.js
│   └── endpoints.js
├── components/
│   └── cotizaciones/
│       ├── cotizaciones.js
│       ├── GraficoEvolucion.jsx
│       ├── ResumenCotizacion.jsx
│       └── TablaCotizaciones.jsx
├── hooks/
│   └── useDetalleJugador.js
├── pages/
│   └── DetalleJugadorPage.jsx
└── test/
    └── handlers.js

tests/
├── components/
│   └── cotizaciones.test.js
└── pages/
    └── detalleJugador.test.jsx
```

**Structure Decision**: Se mantiene la estructura única del frontend existente. La
feature no introduce una capa de backend ni almacenamiento propio; el hook coordina
consultas, los helpers normalizan contrato y los componentes presentan estados.

## Phase 0: Research

La investigación está consolidada en [research.md](./research.md). Resuelve el contrato
fuente, autorización por rol, reutilización del cliente protegido, fallos parciales,
MSW y code splitting. La única brecha de implementación queda explícita: agregar
`GET /api/estrategias` y su estado de error para completar T018.

## Phase 1: Design

- [data-model.md](./data-model.md) define las entidades de jugador, cotización,
  estrategia y sesión, además de transformaciones de porcentaje y fecha.
- [contracts/frontend-api.md](./contracts/frontend-api.md) fija las formas de las
  respuestas y errores que consume el frontend.
- [quickstart.md](./quickstart.md) documenta la validación reproducible con MSW y los
  comandos de calidad.

## Complexity Tracking

No aplica: la solución conserva las capas y dependencias existentes.
