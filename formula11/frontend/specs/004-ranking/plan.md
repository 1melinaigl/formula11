# Implementation Plan: Ranking de jugadores

**Branch**: `003-004-cotizaciones-ranking` | **Spec**: [spec.md](./spec.md)

## Technical Context

Se apila sobre 003: reutiliza la ruta `/jugadores/:id`, `formatearPrecio`, `formatearVariacion`, `formatearFecha`, `nombreEstrategia` y los estilos `.quote-change`. Sin dependencias nuevas; la página no usa Recharts, así que no se carga en diferido.

## Estructura

```text
src/api/endpoints.js                       # ranking
src/components/ranking/ranking.js          # normalizarRanking, filtrarPorPosicion
src/components/ranking/TablaRanking.jsx
src/hooks/useRanking.js
src/pages/RankingPage.jsx                  # ruta /ranking
src/components/navegacion/MenuPrincipal.jsx  # "Ranking" deja de estar bloqueado
src/components/navegacion/Encabezado.jsx     # breadcrumbs de /ranking y /jugadores/:id
src/test/handlers.js                       # handler por defecto, antes de /api/jugadores/:id
tests/pages/ranking.test.jsx
```

## Decisiones

- **Orden del backend**: el frontend no reordena; el puesto es el índice en la lista visible.
- **Filtro local**: una sola consulta; cambiar de posición no vuelve a pedir datos.
- **Tabla semántica** (`<table>` con `th scope="col"`) con link en el nombre, en lugar de filas-botón, para que lectores de pantalla anuncien columnas.
- **MSW**: el handler de `/api/jugadores/ranking` va antes que `/api/jugadores/:id`, porque MSW toma el primero que coincide.
