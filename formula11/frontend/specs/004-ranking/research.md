# Research: Ranking de jugadores

## Decisión: Consumir el contrato de Entrega 2 sin adaptar la forma de la respuesta

- **Decision**: consumir `GET /api/jugadores/ranking` como
  `RankingPageResponse`, con filas en `content` y `jugador` anidado.
- **Rationale**: el contrato define `puesto`, `precio`, `variacion`, `score`,
  `estrategia`, `version` y `fecha`; el frontend no debe inventar una lista plana ni
  reordenar los resultados.
- **Alternatives considered**: reutilizar una respuesta de catálogo o derivar el puesto
  desde el índice local. Se descartan porque perderían el puesto contractual y la
  semántica de ranking del backend.

## Decisión: Filtros y paginación delegados al backend

- **Decision**: enviar `posicion`, `liga`, `page` y `size=20`; omitir parámetros vacíos.
  La primera página usa `page=0` y la UI presenta `page + 1`.
- `liga` usa un valor del enum `Liga` del contrato y lo envía exactamente igual a
  su etiqueta legible.
- **Rationale**: el contrato establece que el backend filtra, ordena por score y
  asigna `puesto`; evita resultados incorrectos al filtrar una sola página local.
- **Alternatives considered**: filtrar o paginar el array ya cargado. Se descarta por
  incompatibilidad con páginas largas y con el puesto calculado por el backend.

## Decisión: Estrategia activa únicamente en esta pantalla

- **Decision**: el frontend no envía `estrategia`, aun cuando el contrato general
  permite que un `SUPERUSUARIO` solicite una estrategia no activa.
- **Rationale**: la aclaración del producto limita esta feature al ranking de la
  estrategia activa; evita agregar selector, estado y autorización fuera del alcance.
- **Alternatives considered**: agregar selector de estrategia para superusuarios. Se
  descarta para mantener una sola vista y el alcance de `004-ranking`.

## Decisión: Errores normalizados y trazables

- **Decision**: reutilizar `solicitarProtegida`; mostrar `mensaje` y `correlationId`,
  limpiar sesión únicamente ante `401` y mantener la pantalla ante `400`/`403`.
- Aunque esta pantalla no envía `estrategia`, un `403` se interpreta como rechazo de
  autorización de la consulta de la estrategia activa; no se reintenta ni se cambia
  de estrategia.
- Si el backend no devuelve `correlationId`, `clienteApi` conserva el
  `X-Correlation-Id` generado para la solicitud. Si falta `mensaje`, muestra
  `No se pudo completar la solicitud.`.
- **Rationale**: coincide con el cliente existente y con el esquema `Error` del
  contrato, además de las reglas constitucionales de seguridad y observabilidad.
- **Alternatives considered**: convertir todo error en acceso denegado o mostrar texto
  genérico. Se descartan porque ocultan si el problema es validación o autorización.

## Decisión: Normalización mínima de datos

- **Decision**: validar `jugador.id` y precio numérico, convertir precio a número y
  transformar porcentajes contractuales a fracción solo para los formateadores
  existentes; conservar `puesto`, `score`, estrategia, versión y fecha de página.
- **Rationale**: mantiene fidelidad al contrato y reutiliza helpers ya probados de la
  feature de cotizaciones.
- **Alternatives considered**: recalcular score, variación o puesto en el frontend.
  Se descarta porque esos valores son responsabilidad del backend.

## Decisión: Estado sin recálculo

- **Decision**: cuando `version` y `fecha` son `null`, mostrar
  `Estrategia activa: <nombre> · sin recálculo todavía`; `estrategia` siempre viene
  informada como estrategia activa obligatoria.
- **Rationale**: el contrato indica que no hubo recálculo; no debe confundirse con
  error ni fabricarse una versión.
- **Alternatives considered**: ocultar metadatos o mostrar una versión ficticia.
  Se descartan por pérdida de información y ambigüedad.
