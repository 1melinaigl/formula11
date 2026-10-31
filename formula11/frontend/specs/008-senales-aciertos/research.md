# Research: Señales de jugadores subvaluados y panel de aciertos

No quedan puntos `NEEDS CLARIFICATION`. Abajo, las decisiones de diseño y las
alternativas descartadas.

## 1. Dónde vive la consulta periódica

- **Decision**: Un hook `useAlertasSenales` montado una sola vez en el shell de la app
  (`App.jsx`, dentro del `BrowserRouter` y debajo del `Encabezado`), junto con el
  componente `AlertaSenales`.
- **Rationale**: La alerta tiene que aparecer en cualquier pantalla (SC-001). Si se
  monta una sola vez, hay una única consulta periódica activa (FR-002), aunque el
  usuario navegue.
- **Alternatives considered**: Consultar solo desde `/senales`, pero así no hay
  alertas en las demás pantallas. Llevar las señales al `SesionContext`, pero eso
  mezcla la sesión con una feature y agranda una interfaz que hoy es chica
  (principio III).

## 2. Frecuencia y ciclo de vida

- **Decision**: Una primera consulta apenas hay sesión y después cada 30 segundos
  con `setInterval`. El efecto depende del `usuario.id`, así que al cerrar la sesión
  (o ante un `401`, que la limpia) el intervalo se corta. Si la consulta anterior
  todavía no volvió, se saltea ese ciclo.
- **Rationale**: El contrato dice "el front consulta cada 30 segundos". Saltear
  ciclos evita acumular consultas si la red está lenta. El cleanup del efecto también
  cubre el doble montaje de `StrictMode` en desarrollo.
- **Alternatives considered**: `setTimeout` encadenado. Es equivalente, pero cuesta
  más testear con timers falsos. Pausar con la pestaña oculta tampoco se hace:
  queda fuera del alcance (ver Assumptions de la spec).

## 3. Señales vistas

- **Decision**: Guardar un arreglo de ids en `sessionStorage`, con la clave
  `formula11.senales.vistas.{usuarioId}`, que es la misma estrategia de la sesión
  (`formula11.session`). Cuando `usuario` pasa a `null`, el hook borra la clave del
  último usuario. Para no crecer sin límite, se guardan como máximo 500 ids y se
  descartan los más viejos.
- **Rationale**: FR-003 pide recordar las señales por sesión de pestaña y borrarlas
  al salir. Así no hace falta tocar `SesionContext`. Los ids son estables entre
  recálculos (spec 010, clarificación de ids), así que una señal ya vista no vuelve
  a alertar después de recalcular.
- **Alternatives considered**: `localStorage`, que alertaría una sola vez entre
  sesiones y pestañas, pero contradice FR-003 y la estrategia de sesión aprobada.
  Guardar solo en memoria no sirve: al recargar la página volverían a aparecer
  alertas ya cerradas.

## 4. Cuándo una señal pasa a "vista"

- **Decision**: Se marcan vistas las señales de la alerta cuando el usuario la
  cierra o pulsa "Ver señales". También se marcan las vigentes que se muestran en
  `/senales`.
- **Rationale**: Si se marcaran apenas llega la respuesta, una alerta que el
  usuario no vio (por ejemplo, porque recargó la página) se perdería. Si el usuario
  ya está en la lista, no tiene sentido alertarlo de lo que tiene en pantalla.
- **Alternatives considered**: Marcarlas al llegar, pero entonces se pierden
  alertas al recargar.

## 5. Tamaño de la consulta periódica

- **Decision**: `vigentes=true`, `page=0` y `size=100`.
- **Rationale**: El backend genera como máximo 20 señales por tipo y semana (40 en
  total), así que una página de 100 trae todas las vigentes en una sola consulta.
  100 es el máximo que permite el contrato.
- **Alternatives considered**: Paginar la consulta periódica, que no hace falta con
  40 señales como máximo.

## 6. Compra en un click

- **Decision**: Un hook nuevo y chico, `useCompraSenal`, que llama a
  `endpoints.comprar` con `{ jugadorId, cantidad: 1 }` mediante `solicitarProtegida`
  y describe el resultado con `describirResultado` de `mercado.js` (005). Guarda el
  estado de envío por `id` de señal.
- **Rationale**: `useMercado` está pensado para el detalle de un jugador: consulta
  el portfolio y `/mercado` de ese jugador y vuelve a pedirlos después de cada orden.
  Usarlo para cada fila haría 2 consultas por fila. La orden y el mensaje son los
  mismos que en la 005, así que la confirmación se ve igual.
- **Alternatives considered**: Validar el saldo en el front antes de comprar, pero
  la lista no tiene el saldo y el backend responde `422` con su mensaje.
  Generalizar `useMercado`, pero eso cambia un hook con tests existentes.

## 7. Formato de porcentajes

- **Decision**: Un helper `formatearPorcentaje` en `senales.js`, con
  `Intl.NumberFormat('es-AR')`, 2 decimales y signo cuando corresponde ("+18,00%",
  "-1,43%"). Brecha, variaciones, tasa y diferencia llegan como porcentaje (18 =
  18 %) y se muestran sin convertir.
- **Rationale**: FR-007 pide 2 decimales. `formatearVariacion` de la 003 usa un
  decimal y espera una fracción, así que reusarlo cambiaría el formato del ranking
  y del detalle.
- **Alternatives considered**: Agregar una opción a `formatearVariacion`, lo que
  toca un helper compartido que ya tiene tests.

## 8. Handlers de MSW por defecto

- **Decision**: Agregar a `src/test/handlers.js` respuestas vacías para
  `GET /api/senales` (página vacía) y `GET /api/senales/aciertos` (`semanas: []`,
  0 y 0).
- **Rationale**: Casi todos los tests de páginas renderizan `<App/>`, y el setup usa
  `onUnhandledRequest: 'error'`. Sin estos handlers, la consulta periódica rompería
  tests existentes que no tienen nada que ver con señales. Con señales vacías no
  aparece ninguna alerta, así que esos tests no cambian. Se agregan handlers y no
  se modifica ningún test existente (principio V).
- **Alternatives considered**: No montar la alerta en tests, lo que exigiría
  cambiar cómo los tests renderizan `<App/>`.

## 9. Referencia visual

- **Decision**: El mockup no tiene una pantalla de señales. La lista reusa la tabla
  del ranking (`TablaRanking`, como estilo), los filtros con `filter-chip` del
  historial, la paginación del catálogo y los paneles del portfolio para los
  indicadores. La alerta usa un panel con borde de acento, fijo arriba a la derecha
  en escritorio y a todo el ancho debajo del encabezado en celular.
- **Rationale**: Principio I: conservar la paleta, la tipografía y los componentes.
  La desviación queda registrada en el plan.
- **Alternatives considered**: Un toast de una librería externa, que trae estilos
  ajenos al mockup y una dependencia nueva.
