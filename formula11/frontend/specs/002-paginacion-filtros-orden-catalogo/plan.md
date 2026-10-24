# Implementation Plan: Paginación, filtros y orden del catálogo conectado al backend

**Branch**: `002-paginacion-filtros-orden-catalogo` | **Date**: 2026-10-07 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/002-paginacion-filtros-orden-catalogo/spec.md`

**Backend contract**: `backend/specs/003-paginacion-catalogo-jugadores/contracts/openapi.yaml`; el frontend lo consume sin redefinirlo.

## Summary

Reemplazar la carga única y el filtrado local de `useCatalogo` por un flujo de consulta paginada contra `GET /api/jugadores`. El hook mantendrá por separado la consulta (`page` base 0, `sort`, `liga`, `equipo`, `posicion`), la respuesta paginada y el estado de carga/error; construirá siempre `size=9`, añadirá query params solo para valores activos y conservará `solicitarProtegida` para JWT, correlation ID y manejo de `401`. `BarraFiltros` dejará de derivar opciones desde los datos, incorporará las cinco ligas fijas, equipo como texto con debounce, posición y orden sin ID. `CatalogoPage` mostrará solo navegación de páginas, mantendrá la ficha actual y distinguirá catálogo vacío de sin coincidencias usando `totalElements` internamente. MSW y los tests migrarán de arrays a respuestas paginadas.

## Technical Context

**Language/Version**: JavaScript ES modules, React 19, Vite 8

**Primary Dependencies**: React, React Router, `@fontsource/bebas-neue`, `@fontsource/barlow`; Vitest, React Testing Library, user-event, MSW, jsdom

**Storage**: Estado del catálogo en memoria; sesión y JWT continúan en `sessionStorage` mediante `SesionContext` sin mostrarse en pantalla.

**Testing**: Vitest + React Testing Library + user-event + MSW en jsdom; tests del hook y componentes/página del catálogo.

**Target Platform**: Navegador moderno, conservando el breakpoint retro responsive existente de 760px.

**Project Type**: Aplicación web frontend React/Vite.

**Performance Goals**: Una request por consulta estabilizada; no descargar ni filtrar localmente el catálogo completo; el filtro de equipo no debe enviar una request por cada tecla; una recuperación fuera de rango debe limitarse a una única solicitud a la última página.

**Constraints**: No cambios en backend; no librería UI nueva; ningún componente realiza `fetch`; solo `src/api` y `useCatalogo` coordinan HTTP; no se muestra el JWT; se conserva la sesión, protección de `/catalogo`, estilo retro, detalle de jugador, mercado fuera de alcance y API de errores existente.

**Scale/Scope**: Un hook de catálogo actualizado, toolbar de filtros/orden, control de paginación, estados adaptados, handlers MSW y tests de hook/página/componentes; sin nuevas rutas de negocio ni selector de tamaño.

## Constitution Check

*GATE: Must pass before implementation. Re-check after design and tests.*

- [x] **Contratos backend como autoridad**: nombres de params, campos de orden, ligas, respuesta paginada y `ErrorResponse` se toman de `backend/specs/003-paginacion-catalogo-jugadores/`.
- [x] **Capas y límites**: `useCatalogo` coordina estado y requests mediante `solicitarProtegida`; componentes reciben props y no ejecutan HTTP.
- [x] **Seguridad**: se conserva el token en la sesión existente, el header Bearer se añade por el cliente API, el JWT no se renderiza y `401` mantiene el cierre de sesión actual.
- [x] **UX retro y accesibilidad**: se reutilizan clases y estilos actuales, con labels, foco visible, botones deshabilitados y controles operables por teclado.
- [x] **Estados verificables**: carga, catálogo vacío, sin coincidencias, error con `mensaje`/`correlationId` y fuera de rango tienen ramas separadas y tests.
- [x] **Scope**: no se modifica backend, detalle remoto, mercado, gráficos ni autenticación.

No se requiere una dependencia nueva ni una excepción de arquitectura.

## Design

### Query state and request lifecycle

1. `useCatalogo` inicializa `page=0`, orden `nombre,asc` y filtros vacíos; cada URL fija `size=9`.
2. El hook mantiene `respuesta` con `content` y metadatos, `cargando`, `error` y la última página válida conocida.
3. Una función de construcción de URL serializa `page`, `size=9`, `sort`, `liga`, `equipo` y `posicion`, omitiendo valores vacíos y conservando los filtros al cambiar de página.
4. Cambiar filtro u orden actualiza el estado de consulta con `page=0`; cambiar de página solo modifica `page`.
5. El valor de `equipo` se estabiliza con un debounce cercano a 300 ms antes de actualizar la consulta enviada. El cleanup cancela timers y respuestas de efectos desmontados.
6. El hook no ejecuta filtrado, ordenamiento ni paginación local: `content` se entrega tal como llega del backend.

### Out-of-range recovery

Cuando `content` está vacío, `totalElements > 0` y la página solicitada no puede ser válida, el hook calcula `ultimaPagina = totalPages - 1`. Si la página actual es distinta de `ultimaPagina`, actualiza una sola vez a esa página y vuelve a consultar; guarda una marca de recuperación para impedir bucles. Si el backend devuelve `totalElements=0`, no se recupera y se deja que la UI distinga catálogo vacío o sin coincidencias.

### Catalog controls

`BarraFiltros` conservará los estilos y chips actuales, pero eliminará búsqueda por nombre y opciones de equipos derivadas. Mostrará:

- Liga fija: `Premier League`, `Bundesliga`, `La Liga`, `Serie A`, `Ligue 1`.
- Posición a partir del dominio visual existente.
- Equipo como input libre.
- Selectores de campo/dirección para `nombre`, `equipo`, `liga`, `posicion` y `asc`/`desc`; `id` no se ofrece en la UI.
- Acción `Limpiar` para reiniciar filtros a la primera página.

Un componente `PaginacionCatalogo` puede concentrar botones anterior/siguiente y página visible base 1; recibirá solo estado y callbacks, sin conocer la API ni mostrar `totalElements`.

### View states

`CatalogoPage` usará `respuesta.content`, `respuesta.totalElements` y filtros activos para decidir el estado, pero no renderizará `totalElements`:

- carga mientras la request vigente está pendiente;
- catálogo vacío sin filtros y `totalElements=0`;
- sin coincidencias con filtros activos y `totalElements=0`;
- error con el objeto ya normalizado por `clienteApi`, incluyendo `mensaje` y `correlationId` mediante `AvisoError`;
- lista/ficha y controles paginados cuando hay contenido.

La numeración visible será base 1 (`page + 1`), mientras que el hook seguirá enviando base 0.

## Project Structure

### Documentation (this feature)

```text
frontend/specs/002-paginacion-filtros-orden-catalogo/
├── plan.md              # This file (/speckit-plan command output)
├── spec.md              # Feature requirements (/speckit-specify output)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
frontend/
├── src/hooks/useCatalogo.js                         # Consulta paginada, debounce y recovery
├── src/components/catalogo/BarraFiltros.jsx         # Filtros remotos y orden
├── src/components/catalogo/PaginacionCatalogo.jsx   # Navegación y metadatos, si se separa
├── src/components/catalogo/EstadoCatalogo.jsx       # Estados basados en respuesta paginada
├── src/pages/CatalogoPage.jsx                        # Orquestación de respuesta y controles
├── src/test/handlers.js                              # Mock paginado de /api/jugadores
├── tests/hooks/useCatalogo.test.jsx                  # Query params y transiciones del hook
├── tests/components/catalogo.test.jsx                # Toolbar, estados y paginación
└── tests/pages/catalogo.test.jsx                     # Flujo E2E de la vista con MSW
```

**Structure Decision**: Se mantiene la arquitectura de la entrega anterior. El hook continúa siendo dueño del estado remoto y la sesión; `CatalogoPage` compone; los componentes de catálogo solo presentan controles. No se crea un cliente HTTP paralelo ni se modifica `SesionContext`, `clienteApi` o el backend.

## Implementation Sequence

1. Definir constantes de ligas, campos de orden visibles (sin `id`), direcciones y el tamaño fijo 9 en el ámbito del catálogo.
2. Refactorizar `useCatalogo` para respuesta paginada, query params, reinicio a página 0, debounce de equipo y recuperación fuera de rango.
3. Actualizar `BarraFiltros` para retirar nombre/equipos derivados y añadir liga fija, equipo libre, posición y orden sin selector de tamaño.
4. Añadir `PaginacionCatalogo` o integrar sus controles en el toolbar, mostrando solo páginas base 1, total de páginas y disabled states.
5. Adaptar `EstadoCatalogo` y `CatalogoPage` a `content`/metadatos, distinguiendo catálogo vacío de sin coincidencias y conservando selección de ficha.
6. Actualizar handlers MSW y fixtures al esquema paginado; añadir escenarios de páginas, filtros, orden, error, vacío y fuera de rango.
7. Actualizar tests del hook y de componentes/página, incluyendo inspección de query params, debounce con timers, reset a 0, navegación, estados y ausencia de JWT en DOM.
8. Ejecutar `npm test`, `npm run lint` y `npm run build` desde `frontend/`.

## Complexity Tracking

No aplica: la recuperación fuera de rango y el debounce son estados necesarios del contrato; se implementan dentro del hook existente sin dependencias ni capas nuevas. El componente de paginación solo se separa si reduce la complejidad de `CatalogoPage` y conserva el estilo actual.
