# Implementation Plan: Detalle de jugador con evolución de cotizaciones

**Branch**: `003-detalle-cotizaciones` | **Spec**: [spec.md](./spec.md)

## Technical Context

React 19 + React Router 7 + Vite; tests con Vitest, Testing Library y MSW. Se suma **Recharts 3** para el gráfico (sugerido en el plan de la Entrega 2; también sirve para `006-portfolio-graficos`). Sin cambios en backend: todo se mockea con MSW hasta que el contrato y los endpoints estén en `main`.

## Estructura

```text
src/api/endpoints.js                         # jugador(id), cotizacionesJugador(id)
src/hooks/useDetalleJugador.js               # consulta ficha + cotizaciones en paralelo (allSettled)
src/components/cotizaciones/cotizaciones.js  # normalizar, series por fecha, resumen y formateo
src/components/cotizaciones/ResumenCotizacion.jsx
src/components/cotizaciones/GraficoEvolucion.jsx
src/components/cotizaciones/TablaCotizaciones.jsx
src/pages/DetalleJugadorPage.jsx             # ruta /jugadores/:id, carga diferida desde AppRouter
src/components/catalogo/FichaJugador.jsx     # link "Ver cotizaciones"
src/test/handlers.js                         # handlers por defecto
tests/pages/detalleJugador.test.jsx
tests/components/cotizaciones.test.js
```

## Decisiones

- **Fallo parcial**: ficha y cotizaciones se piden con `Promise.allSettled`; si fallan solo las cotizaciones, la ficha se sigue viendo.
- **Reinicio por jugador**: el detalle se monta con `key={id}`, así el hook arranca limpio al cambiar de jugador sin `setState` sincrónico dentro del efecto (regla `react-hooks`).
- **Colores fijos en hex** para las líneas: los atributos SVG de Recharts no resuelven `var(--token)` de forma confiable.
- **Code splitting**: `DetalleJugadorPage` se importa con `lazy`; el bundle inicial queda en ~250 kB y Recharts va en su propio chunk.
- **Accesibilidad**: la tabla del historial es la alternativa textual del gráfico y es lo que verifican los tests (Recharts no dibuja en jsdom).
