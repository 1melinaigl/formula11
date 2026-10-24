# Implementation Plan: Portfolio y gráficos del usuario

**Branch**: `006-portfolio-graficos` | **Spec**: [spec.md](./spec.md)

## Technical Context

Apilada sobre 005: reutiliza `endpoints.portfolio`, `normalizarPortfolio` y el `?operacion=venta` del panel de operación; de 003 usa Recharts, los formateadores y el estilo de los gráficos. Sin dependencias nuevas.

## Estructura

```text
src/api/endpoints.js                          # evolucionPortfolio(usuarioId)
src/components/comun/estiloGraficos.js        # ejes, grilla y tooltip compartidos (también los usa 003)
src/components/portfolio/portfolio.js         # enriquecerTenencias, resumenPortfolio, normalizarEvolucion, composicion, gananciasPorJugador, COLORES
src/components/portfolio/ResumenPortfolio.jsx
src/components/portfolio/GraficosPortfolio.jsx # evolución, composición y ganancias
src/components/portfolio/TablaTenencias.jsx
src/hooks/usePortfolio.js                     # portfolio + evolución con allSettled
src/pages/PortfolioPage.jsx                   # ruta /portfolio, carga diferida
src/components/navegacion/*                   # "Mi portfolio" en el menú y breadcrumb
tests/pages/portfolio.test.jsx, tests/components/portfolio.test.js
```

## Decisiones de visualización

- **Forma**: composición como barras horizontales ordenadas y no torta, porque se comparan mejor los tamaños y no hace falta un color por jugador. Ganancias como barras divergentes desde 0.
- **Un solo eje**: valor e invertido están en créditos, así que comparten eje; lo invertido es escalonado y punteado (segunda codificación además del color).
- **Colores** (validados en modo oscuro contra `#06210f`): valor `#e07210`, invertido `#2690b0`, ganancia `#1c9c8c`, pérdida `#e2414f`. La composición usa el color de "valor" porque muestra valor actual.
- **Etiquetas de ganancia** a la derecha del extremo derecho de cada barra (en las negativas, del lado del cero), para que no choquen con los nombres a ningún ancho.
- **`min-width: 0`** en las tarjetas de gráfico: sin eso, `ResponsiveContainer` no se achica dentro del grid y desborda en celular.
- **Alternativa textual**: la tabla de tenencias tiene los mismos datos que los gráficos.
