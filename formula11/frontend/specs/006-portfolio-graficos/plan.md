# Implementation Plan: Portfolio y gráficos del usuario

**Branch**: `006-portfolio-graficos` | **Date**: 2026-10-24 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/006-portfolio-graficos/spec.md`

## Summary

Mostrar en `/portfolio` la posición del `USUARIO`: resumen con los totales del
`PortfolioResponse`, tabla de tenencias y tres gráficos (valor vs. invertido,
composición y ganancia o pérdida por jugador), con la evolución de
`GET /api/usuarios/{id}/portfolio/evolucion`. La pantalla ya existe; este plan cubre lo
que agregaron la aclaración y la revisión del spec: el superusuario sin portfolio (sin
ítem de menú, aviso por URL y sin consultas), el aviso de "poca historia" en lugar del
gráfico de evolución, el orden común por valor (con desempate por nombre) entre tabla
y composición, y una tabla oculta como alternativa textual de la evolución.

## Technical Context

**Language/Version**: JavaScript ES modules, React 19, Vite 8.

**Primary Dependencies**: React Router 7, Recharts 3 (cargado en diferido con la
página); MSW 2, Vitest 5, React Testing Library y user-event para pruebas. Sin
dependencias nuevas.

**Storage**: N/A. La sesión existente guarda `{ token, usuario }` (incluido `rol`).

**Testing**: Vitest + React Testing Library + MSW con `server.use(...)` por caso;
`npm test`, `npm run lint` y `npm run build`.

**Target Platform**: Navegadores soportados por el frontend Vite; layout responsive.

**Project Type**: Aplicación web frontend (SPA).

**Performance Goals**: Mantener Recharts fuera del bundle inicial (la página se carga
con `lazy`).

**Constraints**: Totales del backend sin recalcular (solo saldo + valor de las
tenencias); rutas con el `usuario.id` de la sesión; colores validados para fondo oscuro
y daltonismo; sin desborde a 375px.

**Scale/Scope**: Una página con resumen, tabla y tres gráficos; una temporada tiene
menos de 52 puntos de evolución y hasta 100 tokens por jugador.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Fidelidad visual verificable**: PASS. Reutiliza `player-card`, `quote-tile` y
  la tabla del ranking; corte responsive en 760px. Los gráficos no existen en el
  mockup: se integran con su paleta y tipografías y con colores validados contra el
  fondo de la cancha.
- **II. Contratos del backend como autoridad**: PASS. Fuente:
  `backend/specs/005-contratos-entrega2/contracts/openapi.yaml` (constitución v1.1.0).
  Ocultar el portfolio al `SUPERUSUARIO` es una decisión de UX; no se inventan campos.
- **III. Capas y límites explícitos**: PASS. `PortfolioPage` compone, `usePortfolio`
  coordina las dos consultas, `portfolio.js` normaliza y ordena, y los componentes de
  gráficos solo presentan.
- **IV. Validación, seguridad y trazabilidad**: PASS. Errores en español con
  `correlationId`; `401` limpia la sesión; el JWT no se expone; ruta con el id de la
  sesión.
- **V. Calidad accesible y comprobable**: PASS. Cada gráfico tiene alternativa textual
  (tabla visible o tabla oculta para la evolución) y la polaridad no depende solo del
  color. Los tests existentes se conservan; solo se agregan casos.

**Re-check post-design**: PASS.

## Project Structure

### Documentation (this feature)

```text
specs/006-portfolio-graficos/
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
├── api/endpoints.js                          # portfolio, evolucionPortfolio
├── components/
│   ├── comun/estiloGraficos.js               # ejes, grilla y tooltip compartidos
│   ├── navegacion/MenuPrincipal.jsx          # "Mi portfolio" solo para USUARIO
│   └── portfolio/
│       ├── portfolio.js                      # normalizar, ordenar, composición, ganancias
│       ├── ResumenPortfolio.jsx
│       ├── GraficosPortfolio.jsx             # evolución (+ tabla oculta), composición, ganancias
│       └── TablaTenencias.jsx
├── hooks/usePortfolio.js                     # portfolio + evolución (allSettled)
└── pages/PortfolioPage.jsx                   # /portfolio, aviso para el superusuario

tests/
├── components/portfolio.test.js
└── pages/portfolio.test.jsx
```

**Structure Decision**: Estructura única del frontend; la feature agrega una ruta
(`/portfolio`) cargada en diferido.

## Decisiones de visualización

- **Forma**: composición como barras horizontales ordenadas (no torta) y ganancias
  como barras divergentes desde 0.
- **Un solo eje** para valor e invertido (ambos en créditos); lo invertido es
  escalonado y punteado (segunda codificación además del color).
- **Colores** validados en modo oscuro contra `#06210f`: valor `#e07210`, invertido
  `#2690b0`, ganancia `#1c9c8c`, pérdida `#e2414f`.
- **Etiquetas de ganancia** a la derecha de cada barra para no chocar con los nombres.
- **`min-width: 0`** en las tarjetas para que `ResponsiveContainer` se achique en celular.

## Complexity Tracking

No aplica: no hay violaciones de la constitución.
