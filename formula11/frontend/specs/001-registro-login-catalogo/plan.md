# Implementation Plan: Registro, login y catálogo de jugadores

**Branch**: `001-registro-login-catalogo` | **Date**: 2026-09-23 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/001-registro-login-catalogo/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command; its definition describes the execution workflow.

## Summary

La entrega reemplaza la demo de Vite por un frontend React para registro, login y catálogo
protegido. La solución usará React Router para `/`, `/ingresar`, `/registro`,
`/cuenta-creada` y `/catalogo`, Context y `useSesion` para la sesión, un cliente API único
para los tres endpoints OpenAPI y filtrado local del catálogo.
El look and feel se portará del mockup a CSS global dividido, con fuentes locales
`@fontsource`, breakpoint de 760px y sin cuentas demo ni JWT visible. Vitest, React Testing
Library y MSW cubrirán los estados felices y de borde.

## Technical Context

<!--
  ACTION REQUIRED: Replace the content in this section with the technical details
  for the project. The structure here is presented in advisory capacity to guide
  the iteration process.
-->

**Language/Version**: JavaScript ES modules, React 19, Vite 8

**Primary Dependencies**: `react-router-dom`, `@fontsource/bebas-neue`,
`@fontsource/barlow`; dev: Vitest, `@testing-library/react`,
`@testing-library/user-event`, `@testing-library/jest-dom`, MSW, jsdom

**Storage**: `sessionStorage` para token y datos mínimos de sesión; catálogo solo en memoria

**Testing**: Vitest + React Testing Library + user-event + MSW en jsdom

**Target Platform**: Navegador moderno; responsive con corte obligatorio en 760px

**Project Type**: Aplicación web frontend

**Performance Goals**: Una sola consulta de catálogo por carga de sesión; búsqueda y filtros
locales sin requests adicionales; interacción de menú y filtros perceptiblemente inmediata

**Constraints**: No Tailwind, librería UI ni CDN; no cambios al backend; ningún componente
ejecuta fetch; ningún token en DOM o logs; proxy `/api` hacia `http://localhost:8080`;
el cliente solo consume `POST /api/usuarios/registro`, `POST /api/usuarios/login` y
`GET /api/jugadores`

**Scale/Scope**: 5 rutas principales, 3 endpoints existentes, catálogo completo manejado en
memoria y una primera entrega sin ranking, mercado, cotizaciones, transacciones ni recovery

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

* **I. Fidelidad visual verificable**: PASS. El plan porta el mockup a CSS global, conserva
  paleta, Bebas Neue/Barlow, paneles, botones inclinados, scanlines, cancha y 760px. Las
  diferencias obligatorias (sin demo, sin JWT visible, sin toggle de catálogo vacío) quedan
  documentadas en research y quickstart.
* **II. Contratos del backend como autoridad**: PASS. Solo se consumen los tres endpoints
  definidos en `backend/specs/001-registro-catalogo-jugadores` y
  `backend/specs/002-login-usuarios`; el filtrado es local porque el contrato no ofrece
  parámetros de búsqueda.
* **III. Capas y límites explícitos**: PASS. Páginas, componentes, `useSesion`/estado y
  `src/api` tienen responsabilidades separadas; ningún componente hace fetch.
* **IV. Validación, seguridad y trazabilidad**: PASS. Se validan límites del DTO, cada
  request genera `X-Correlation-Id`, el token queda en `sessionStorage` sin renderizar ni
  loguear, y un `401` protegido limpia la sesión.
* **V. Calidad accesible y comprobable**: PASS. MSW cubre `400`, `401`, `409` y catálogo
  vacío; se incluyen navegación por teclado, labels, foco visible, build, lint y tests.
* **Additional Constraints / Workflow**: PASS. Se mantienen JavaScript, React/Vite, CSS sin
  librerías visuales, español en UI y plan con las dependencias ausentes explicitadas.

## Project Structure

### Documentation (this feature)

```text
specs/001-registro-login-catalogo/
├── plan.md              # This file (/speckit-plan command output)
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── api-client.md
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)
```text
src/
├── api/
│   ├── clienteApi.js
│   ├── erroresApi.js
│   └── endpoints.js
├── components/
│   ├── accesibilidad/
│   ├── catalogo/
│   ├── comun/
│   ├── formularios/
│   └── navegacion/
├── context/
│   └── SesionContext.jsx
├── hooks/
│   ├── useSesion.js
│   └── useCatalogo.js
├── pages/
│   ├── InicioPage.jsx
│   ├── AccesoPage.jsx
│   ├── CuentaCreadaPage.jsx
│   ├── CatalogoPage.jsx
│   └── AccesoDenegadoPage.jsx
├── router/
│   └── AppRouter.jsx
├── styles/
│   ├── base.css
│   ├── layout.css
│   ├── components.css
│   └── screens.css
├── test/
│   ├── handlers.js
│   ├── server.js
│   └── setup.js
├── App.jsx
├── index.css
└── main.jsx

tests/
├── api/
├── components/
├── pages/
└── hooks/
```

**Structure Decision**: Aplicación web single-project dentro del frontend existente. Las
páginas orquestan navegación y estado de vista; componentes presentan UI; hooks y Context
concentran estado; `src/api` es el único límite HTTP; estilos globales portan el mockup; los
tests mantienen handlers MSW independientes del backend real. Se reemplazan los archivos de
demo `App.jsx`, `App.css`, `index.css` y assets no usados.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| N/A | No hay violaciones constitucionales | N/A |
