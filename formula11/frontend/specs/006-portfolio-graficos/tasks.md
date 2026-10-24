---

description: "Task list for Formula11 user portfolio and charts"
---

# Tasks: Portfolio y gráficos del usuario

**Input**: `spec.md` y `plan.md` de `/specs/006-portfolio-graficos/`.

**Tests**: Obligatorios; Vitest, React Testing Library, user-event y MSW.

**Scope**: Solo frontend, contra mocks de MSW. Depende de 003 y 005.

## Phase 1: Fundamentos

- [X] T001 Agregar `evolucionPortfolio(usuarioId)` a `src/api/endpoints.js` y su handler por defecto en `src/test/handlers.js`.
- [X] T002 [P] Crear helpers en `src/components/portfolio/portfolio.js`.
- [X] T003 [P] Extraer el estilo compartido de gráficos a `src/components/comun/estiloGraficos.js` y usarlo en el gráfico del 003.
- [X] T004 [P] Validar la paleta de los gráficos contra el fondo oscuro con el validador.
- [X] T005 [P] Tests unitarios de los helpers en `tests/components/portfolio.test.js`.

## Phase 2: User Story 1 - Ver mi posición

- [X] T006 Crear `src/hooks/usePortfolio.js`.
- [X] T007 Crear `ResumenPortfolio.jsx` y `TablaTenencias.jsx` (con "Vender" hacia `?operacion=venta`).
- [X] T008 Crear `src/pages/PortfolioPage.jsx`, ruta `/portfolio` en diferido, ítem "Mi portfolio" en el menú y breadcrumb.
- [X] T009 Tests de resumen, tabla, tenencias en 0, estado sin tokens y navegación desde el menú.

## Phase 3: User Story 2 - Gráficos

- [X] T010 Gráfico de evolución (valor vs. invertido).
- [X] T011 Gráfico de composición con agrupación en "Otros".
- [X] T012 Gráfico de ganancia o pérdida divergente con etiquetas con signo.
- [X] T013 Test de que falla la evolución sin ocultar el resto.

## Phase 4: User Story 3 - Estados

- [X] T014 Carga, error con `correlationId` y sin sesión, con tests.

## Phase 5: Cierre

- [X] T015 Revisión visual en el navegador (API simulada) a 1100px y 375px: sin desborde y sin choques de etiquetas.
- [X] T016 `npm test`, `npm run lint` y `npm run build` en verde.
- [X] T017 Alinear con el contrato `005-contratos-entrega2`: totales y `posiciones` del `PortfolioResponse`, `fechaValuacion` y `puntos` de la evolución.
