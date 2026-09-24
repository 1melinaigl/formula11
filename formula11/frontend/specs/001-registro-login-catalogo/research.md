# Research: Registro, login y catálogo de jugadores

## Decisión 1: Mantener el scaffold React 19 + Vite 8 en JavaScript

- **Decision**: Conservar React 19, Vite 8 y ESLint, y reemplazar únicamente la demo de
  Vite por la aplicación Formula11.
- **Rationale**: Es la base existente y satisface la restricción del usuario sin introducir
  una migración de lenguaje o bundler.
- **Alternatives considered**: TypeScript o un framework adicional; se descartan porque no
  son necesarios para esta entrega y aumentarían el alcance.

## Decisión 2: Routing explícito con React Router

- **Decision**: Usar `react-router-dom` con rutas `/`, `/ingresar`, `/registro`, una ruta de
  confirmación de cuenta creada y `/catalogo`.
- **Rationale**: Permite deep links, protección declarativa de `/catalogo` y navegación por
  teclado sin mezclar URLs con el estado visual de las páginas.
- **Alternatives considered**: Estado manual de pantalla; se descarta porque no representa
  correctamente URLs recargables ni una ruta protegida.

## Decisión 3: Sesión centralizada con Context y `useSesion`

- **Decision**: El Context expone usuario, estado de carga, sesión activa, restauración,
  ingreso, registro y cierre de sesión; `useSesion` es el único hook de consumo.
- **Rationale**: Evita que páginas y componentes dupliquen la lógica de sesión. El token se
  guarda en `sessionStorage`, se usa internamente en el cliente API y nunca se renderiza ni
  se registra.
- **Alternatives considered**: Estado local por página; se descarta porque rompería la
  persistencia entre rutas y la limpieza global ante `401`.

## Decisión 4: Cliente API único y normalización defensiva de errores

- **Decision**: Centralizar todas las requests en `src/api/clienteApi.js`. Cada request lleva
  `X-Correlation-Id: crypto.randomUUID()` y, si existe sesión, `Authorization: Bearer`.
  Los errores se convierten a `{ mensaje, timestamp, correlationId }`, usando primero el
  cuerpo y luego el header de respuesta o el id enviado como respaldo.
- **Rationale**: Cumple la trazabilidad exigida y protege a las capas de UI de conocer
  detalles de HTTP. El contrato de error solo define un mensaje general, por lo que la UI
  conserva el aviso general y aplica validaciones de forma local bajo cada campo.
- **Alternatives considered**: `fetch` directo desde páginas o un cliente externo; se
  descartan por violar la separación de capas o añadir dependencias no requeridas.

## Decisión 5: Cargar el catálogo una vez y filtrar en memoria

- **Decision**: Ejecutar solo `GET /api/jugadores`; búsqueda por nombre y filtros por liga,
  posición y equipo se calculan sobre la colección recibida. Las posiciones `POR`, `DEF`,
  `MED`, `DEL` se presentan como Portero, Defensor, Mediocampista y Delantero con color;
  cualquier valor desconocido usa estilo neutro.
- **Rationale**: El contrato no define query parameters de filtrado. Inventar parámetros
  rompería la autoridad OpenAPI y el requisito explícito de filtrado local.
- **Alternatives considered**: Filtrado server-side con query params; se descarta hasta que
  exista una ampliación contractual.

## Decisión 6: CSS global dividido y fuentes locales

- **Decision**: Portar el mockup a `src/styles/base.css`, `layout.css`, `components.css` y
  `screens.css`, importados desde `index.css`. Usar `@fontsource/bebas-neue` y
  `@fontsource/barlow`, sin CDN.
- **Rationale**: Conserva el look and feel como una base auditable y evita depender de red
  para tipografía. La regla responsive de 760px se mantiene como contrato visual.
- **Alternatives considered**: Tailwind, una librería UI o Google Fonts por CDN; se
  descartan por las restricciones y por dificultar la fidelidad exacta del mockup.

## Decisión 7: Testing con Vitest, Testing Library y MSW

- **Decision**: Configurar Vitest con entorno `jsdom`, React Testing Library, user-event y
  MSW. Los handlers simulan registro, login, catálogo, `400`, `401`, `409` y catálogo vacío.
- **Rationale**: Verifica comportamiento y límites sin depender del backend en ejecución;
  también permite probar la limpieza de sesión y la normalización de errores.
- **Alternatives considered**: Tests manuales o mocks ad hoc de `fetch`; se descartan porque
  no cubren de forma consistente el contrato HTTP ni la interacción real de usuario.

## Decisión 8: Proxy de desarrollo

- **Decision**: Configurar en Vite el proxy `/api` hacia `http://localhost:8080` sin cambiar
  el backend ni habilitar CORS.
- **Rationale**: Mantiene las URLs relativas de la aplicación y evita el bloqueo de origen
  durante el desarrollo local.
- **Alternatives considered**: Modificar CORS del backend o usar URL absoluta en cada
  servicio; se descartan porque el backend está fuera del alcance y duplicar URLs aumenta
  la configuración.
