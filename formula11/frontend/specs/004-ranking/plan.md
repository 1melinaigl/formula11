# Implementation Plan: Ranking de jugadores

**Branch**: `003-004-cotizaciones-ranking` | **Date**: 2026-10-24 | **Spec**: [spec.md](./spec.md)

## Summary

La feature expone `/ranking` para usuarios autenticados y consume
`GET /api/jugadores/ranking` mediante el cliente protegido existente. El frontend
presenta el ranking ya ordenado por el backend, delega filtros y paginación al
servidor, muestra los metadatos de la estrategia activa y conserva los errores
contractuales con `mensaje` y `correlationId`. El alcance no incluye seleccionar otra
estrategia: aunque el contrato general permite que un `SUPERUSUARIO` la solicite,
esta pantalla siempre consulta la estrategia activa sin enviar `estrategia`.

## Technical Context

**Language/Version**: JavaScript ES modules, React 19, Vite 8.

**Primary Dependencies**: React Router 7, MSW 2, Vitest 5, React Testing Library y
user-event. No se agregan dependencias nuevas.

**Storage**: N/A. La sesión existente conserva el JWT mediante el mecanismo aprobado
por `SesionContext`; el ranking no introduce estado persistente.

**Testing**: Vitest, React Testing Library, user-event y MSW. La validación de cierre
incluye `npm test`, `npm run lint` y `npm run build`.

**Target Platform**: Navegadores soportados por Vite, con responsive en el corte de
760px definido por el mockup.

**Project Type**: Aplicación web frontend.

**Performance Goals**: Una consulta protegida por cambio efectivo de filtros o página;
no se filtra ni reordena localmente. La página no incorpora Recharts ni modifica el
code splitting existente.

**Constraints**:

- El contrato de Entrega 2 es la autoridad para ruta, nombres, campos, paginación,
  filtros y errores.
- Las llamadas pasan por `solicitarProtegida`, incluyendo `Authorization` y
  `X-Correlation-Id`.
- La API usa `page=0` en la primera página y `size=20`; la UI muestra páginas desde 1.
- `variacion` llega como porcentaje contractual (`12.5` = `12,5 %`) y se transforma
  solo para reutilizar los formateadores existentes.
- La UI, documentación y errores permanecen en español; no se expone el JWT.

**Scale/Scope**: Una página paginada de hasta 20 filas visibles, con filtros de
posición y liga enviados al backend. No hay cambios de backend ni almacenamiento.

## Constitution Check

*GATE: Must pass before Phase 0 research and be rechecked after Phase 1 design.*

- **Fidelidad visual verificable**: PASS. Se reutilizan paneles, botones, tipografías,
  colores y corte responsive existentes; la tabla agrega solo la superficie prevista
  por el mockup y los estilos actuales.
- **Contratos del backend como autoridad**: PASS con dependencia documentada. El
  contrato de Entrega 2 define el endpoint de ranking y sus formas. Conforme a la
  constitución v1.1.0, la fuente autorizada para esta feature es
  `backend/specs/005-contratos-entrega2/contracts/openapi.yaml`; no se inventan
  endpoints, campos ni datos.
- **Capas y límites explícitos**: PASS. `useRanking` coordina estado y llamadas,
  `ranking.js` normaliza y construye consultas, `TablaRanking` presenta y el cliente
  protegido centraliza transporte.
- **Validación, seguridad y trazabilidad**: PASS. Se conserva la limpieza ante `401`,
  los mensajes en español, `correlationId` y la prohibición de mostrar tokens. Aunque
  esta feature no envía `estrategia`, un `403` del backend se interpreta como rechazo
  de autorización de la consulta de la estrategia activa: se muestra el error
  contractual, sin reintentar silenciosamente ni cambiar de estrategia.
- **Calidad accesible y comprobable**: PASS condicionado a cierre. La tabla usa
  encabezados semánticos, el nombre es un enlace, filtros usan `aria-pressed` y los
  tests MSW cubren éxito, vacío, errores, filtros y ausencia de sesión.

No hay violaciones que requieran Complexity Tracking. La feature aplica directamente
la autoridad contractual definida por la constitución vigente.

## Project Structure

### Documentation

```text
specs/004-ranking/
├── plan.md
├── research.md
├── data-model.md
├── contracts/
│   └── frontend-api.md
├── quickstart.md
└── tasks.md
```

### Source Code

```text
src/
├── api/
│   └── endpoints.js
├── components/
│   └── ranking/
│       ├── ranking.js
│       └── TablaRanking.jsx
├── hooks/
│   └── useRanking.js
├── pages/
│   └── RankingPage.jsx
└── test/
    └── handlers.js

tests/
└── pages/
    └── ranking.test.jsx
```

**Structure Decision**: Se mantiene la arquitectura existente. La página no ejecuta
fetch, el hook utiliza `solicitarProtegida`, los helpers aíslan transformaciones del
contrato y MSW provee respuestas contractuales para pruebas.

## Phase 0: Research

La investigación está consolidada en [research.md](./research.md). Resuelve el uso
del endpoint de Entrega 2, la semántica de paginación y filtros, la decisión de no
enviar `estrategia`, la normalización de porcentajes y el tratamiento de errores.

## Phase 1: Design

- [data-model.md](./data-model.md) define la página, las filas anidadas, el jugador
  resumido, los filtros y la sesión.
- [contracts/frontend-api.md](./contracts/frontend-api.md) fija la consulta,
  respuesta, headers, errores y estados visibles.
- [quickstart.md](./quickstart.md) documenta comandos y escenarios MSW/manuales para
  validar la feature contra el contrato.

## Complexity Tracking

No aplica: se reutilizan cliente, helpers de cotización, paginación, estilos y
componentes de sesión existentes.
