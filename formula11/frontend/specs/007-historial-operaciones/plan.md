# Implementation Plan: Historial de operaciones

**Branch**: `007-historial-operaciones` | **Spec**: [spec.md](./spec.md)

## Technical Context

Apilada sobre 006: se entra desde el portfolio y desde la confirmación del panel de operación (005). Reutiliza `PaginacionCatalogo` (ahora con etiqueta configurable), los estilos de tabla del ranking y los formateadores del 003. Sin dependencias nuevas ni gráficos, así que la página no se carga en diferido.

## Estructura

```text
src/api/endpoints.js                          # transacciones(usuarioId)
src/components/historial/historial.js         # normalizarHistorial, formatearFechaHora, efectoEnSaldo, construirConsultaHistorial
src/components/historial/TablaHistorial.jsx
src/hooks/useHistorial.js                     # consulta paginada con filtro de tipo
src/pages/HistorialPage.jsx                   # ruta /operaciones
src/components/catalogo/PaginacionCatalogo.jsx  # prop `etiqueta`
src/pages/PortfolioPage.jsx, src/components/mercado/PanelOperacion.jsx  # links al historial
tests/pages/historial.test.jsx, tests/components/historial.test.js
```

## Decisiones

- **Paginación y filtro en el backend**: el historial crece con cada operación; no se descarga completo.
- **Sin carga fantasma**: `cambiarPagina` y `cambiarTipo` no hacen nada si el valor no cambia; si no, el estado quedaría "cargando" porque el efecto no vuelve a correr.
- **Efecto en el saldo** en lugar de "total" a secas: el signo (y el color) dicen si la operación sumó o restó créditos; el tipo también va como texto, así no depende solo del color.
- **Hora en formato de 24 h** (`hourCycle: 'h23'`): Node y algunos navegadores formatean `es-AR` en 12 h.
