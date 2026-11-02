# Implementation Plan: Señales de jugadores subvaluados y panel de aciertos

**Branch**: `008-senales-aciertos` | **Date**: 2026-10-31 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/008-senales-aciertos/spec.md`

## Summary

La feature agrega al front las señales de la `010` del backend en tres piezas:

- **Alerta global**: un hook montado una sola vez en el shell de la app consulta
  `GET /api/senales` cada 30 segundos mientras hay sesión. Muestra en cualquier
  pantalla una alerta agrupada con las señales que el usuario todavía no vio en la
  pestaña, y recuerda las vistas en `sessionStorage`.
- **Pantalla `/senales`**: tabla de señales vigentes o del historial, paginada de a
  20, con compra de 1 token en un click (`POST /api/ordenes/compra`, igual que la
  005). El botón solo aparece para el rol `USUARIO`.
- **Panel de aciertos**: en la misma pantalla, con `GET /api/senales/aciertos`.
  Muestra la tasa, la diferencia contra el mercado y una tabla por semana.

No hay dependencias nuevas ni endpoints fuera del contrato.

## Technical Context

**Language/Version**: JavaScript ES modules, React 19, Vite 8.

**Primary Dependencies**: React Router 7. Para pruebas, MSW 2, Vitest 5, React
Testing Library y user-event. No se agregan dependencias (no hace falta Recharts:
el panel usa indicadores y una tabla).

**Storage**: `sessionStorage` para las señales vistas
(`formula11.senales.vistas.{usuarioId}`), con la misma estrategia que la sesión
existente (`formula11.session`).

**Testing**: Vitest + React Testing Library + MSW, con `server.use(...)` por caso.
Las alertas se prueban con `vi.useFakeTimers({ shouldAdvanceTime: true })`.
Definición de terminado: `npm test`, `npm run lint` y `npm run build`.

**Target Platform**: Los navegadores que soporta el frontend Vite, con layout
responsive (corte en 760px y prueba a 375px).

**Project Type**: Aplicación web frontend (SPA).

**Performance Goals**: Una consulta periódica cada 30 segundos como máximo, con un
ciclo salteado si la anterior sigue pendiente. Una consulta por cambio de vista o
de página, ninguna si el valor no cambia, y una sola del panel al entrar.

**Constraints**: El orden lo da el backend. Los porcentajes llegan como porcentaje
y se muestran con 2 decimales en `es-AR`. Las fechas `date` no se corren de día.
La compra es solo para `USUARIO`. La alerta no roba el foco.

**Scale/Scope**: Hasta 40 señales vigentes por semana y unas centenas en el
historial. Son una pantalla nueva, un componente global y un ítem de menú.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Fidelidad visual verificable**: PASS, con una desviación justificada. El
  mockup `design/formula11-mockup-retro-v3.html` no tiene una pantalla de señales
  ni alertas. Se reusan la paleta, las tipografías y los componentes que ya
  existen: la tabla con el estilo del ranking, `filter-chip` para
  Vigentes/Historial, la paginación del catálogo, los paneles del portfolio para
  los indicadores y los colores de ganancia y pérdida. La alerta es un `.panel` con
  borde de acento: fija arriba a la derecha en escritorio y a todo el ancho debajo
  del encabezado hasta 760px. Esto se valida visualmente antes de cerrar la
  feature (ver [research.md](./research.md) §9).
- **II. Contratos del backend como autoridad**: PASS. La fuente es
  `backend/specs/005-contratos-entrega2/contracts/openapi.yaml` (constitución
  v1.1.0): `GET /api/senales` y `GET /api/senales/aciertos` (`x-feature:
  010-feature-extra`) y `POST /api/ordenes/compra` (`007`). No se envía `sort` ni
  se inventan campos. Las señales con un `tipo` fuera del enum se descartan.
- **III. Capas y límites explícitos**: PASS. Las páginas (`SenalesPage`) y los
  componentes (`AlertaSenales`, `TablaSenales`, `PanelAciertos`) solo componen y
  muestran. Los hooks (`useAlertasSenales`, `useSenales`, `useAciertos`,
  `useCompraSenal`) coordinan las consultas a través de `solicitarProtegida`.
  `senales.js` normaliza, formatea y maneja el registro de vistas, sin `fetch`.
  `SesionContext` no cambia.
- **IV. Validación, seguridad y trazabilidad**: PASS. Los errores se muestran en
  español con `correlationId` (`AvisoError`). Un `401` en cualquier consulta limpia
  la sesión (`clienteApi`), lo que corta la consulta periódica y borra las
  señales vistas. El JWT no se muestra ni se guarda fuera de la sesión.
- **V. Calidad accesible y comprobable**: PASS. La alerta usa `role="status"`, se
  cierra con `Escape` y no roba el foco. Las tablas tienen `caption` y
  encabezados, y Vigentes/Historial usa `aria-pressed`. Los tests nuevos cubren
  los casos felices y los bordes (lista vacía, `401`, `409`, `422`, panel vacío y
  consulta fallida). **No se modifica ningún test existente**: solo se agregan
  handlers por defecto a `src/test/handlers.js` para las dos rutas nuevas
  (research §8).

**Re-check post-design**: PASS. El diseño de la Fase 1 no agrega violaciones. La
única desviación es la visual del principio I, ya justificada arriba.

## Project Structure

### Documentation (this feature)

```text
specs/008-senales-aciertos/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── frontend-api.md
├── checklists/
│   └── requirements.md
└── tasks.md              # lo genera /speckit-tasks
```

### Source Code (repository root)

```text
src/
├── App.jsx                                   # monta <AlertaSenales /> debajo del Encabezado
├── api/endpoints.js                          # senales, aciertos
├── components/
│   ├── navegacion/MenuPrincipal.jsx          # ítem "Señales" (solo con sesión)
│   ├── navegacion/Encabezado.jsx             # título "Señales" para /senales
│   └── senales/
│       ├── senales.js                        # normalizar, tipos, formatearPorcentaje, senalesNuevas, registro de vistas
│       ├── AlertaSenales.jsx                 # alerta global agrupada
│       ├── TablaSenales.jsx                  # tabla con compra en un click
│       └── PanelAciertos.jsx                 # indicadores y tabla por semana
├── hooks/
│   ├── useAlertasSenales.js                  # consulta cada 30 s, vistas, limpieza al salir
│   ├── useSenales.js                         # lista paginada, vigentes/historial, página fuera de rango
│   ├── useAciertos.js                        # consulta única del panel
│   └── useCompraSenal.js                     # POST compra con cantidad 1, estado por señal
├── pages/SenalesPage.jsx                     # /senales: lista + panel; acceso denegado sin sesión
├── router/AppRouter.jsx                      # ruta /senales
├── styles/components.css                     # estilos de la alerta
├── styles/screens.css                        # estilos de la lista y del panel (cortes en 1024px y 760px)
└── test/handlers.js                          # handlers por defecto de /api/senales y /api/senales/aciertos

tests/
├── components/senales.test.js
├── pages/alertaSenales.test.jsx
└── pages/senales.test.jsx
```

**Structure Decision**: Se mantiene la estructura única del frontend. La feature
agrega la ruta `/senales`, sin carga diferida porque no usa gráficos. La alerta
se monta en `App.jsx` para que exista una sola consulta periódica en toda la app.

## Riesgos

- **Tests existentes**: la consulta periódica corre en todos los tests que
  renderizan `<App/>`. Los handlers por defecto devuelven listas vacías, así que no
  aparece ninguna alerta ni cambia ninguna aserción existente. Se revisó que los
  contadores de pedidos existentes (`mercado.test.jsx`, `useCatalogo.test.jsx`,
  `ranking.test.jsx`) son por endpoint (portfolio, `/mercado`, `/jugadores`,
  ranking), así que la consulta a `/api/senales` no los afecta.
- **Timers en tests**: el intervalo de 30 segundos se prueba con timers falsos.
  Hay que restaurar los reales en el `afterEach` de esos archivos para no afectar
  al resto de la suite.
- **Datos de la demo**: la validación manual de la compra cambia el saldo y las
  tenencias. Conviene usar un usuario de prueba o restaurar el backup.

## Complexity Tracking

No aplica: no hay violaciones de la constitución. La desviación visual del
principio I está justificada en el chequeo de arriba, como pide la constitución.
