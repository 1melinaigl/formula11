# Research: Historial de operaciones

No quedaron puntos `NEEDS CLARIFICATION`. Decisiones de diseño y alternativas.

## 1. Paginación y filtro en el backend

- **Decision**: Pedir de a 10 con `page`, `size` y `tipo`; sin `sort`.
- **Rationale**: El historial crece con cada operación; el contrato pagina y ordena
  (de la más nueva a la más vieja) y no define `sort`.
- **Alternatives considered**: Traer todo y filtrar en el front (no escala); enviar
  `sort` (parámetro fuera del contrato).

## 2. Página fuera de rango

- **Decision**: Si la página llega vacía con `totalElements` mayor a 0 y la página
  pedida es mayor o igual a `totalPages`, volver una sola vez a `totalPages - 1`.
- **Rationale**: Mismo criterio que el catálogo (002); evita mostrar "Sin operaciones"
  cuando sí hay operaciones.
- **Alternatives considered**: Mostrar el vacío (engañoso); volver a la página 1 (pierde
  la posición del usuario).

## 3. Respuestas obsoletas

- **Decision**: Descartar la respuesta de una consulta que ya no es la vigente (flag por
  efecto).
- **Rationale**: Al cambiar rápido de filtro o página, una respuesta lenta no debe
  pisar la actual.
- **Alternatives considered**: Cancelar la petición con `AbortController` (el cliente
  de API no lo expone hoy).

## 4. Superusuario

- **Decision**: Aviso y ninguna consulta, como en el portfolio (006).
- **Rationale**: No opera y no tiene portfolio; su historial estaría vacío o confundiría.
- **Alternatives considered**: Mostrar su historial (vacío); ver las operaciones de
  todos (no hay endpoint).

## 5. Operaciones al precio inicial

- **Decision**: Marca "(precio inicial)" junto al precio unitario cuando `estrategia`
  es null.
- **Rationale**: Permite distinguir las compras del escenario 3 de la demo ("comprar al
  inicio del campeonato") sin agregar una columna.
- **Alternatives considered**: Columna de estrategia (más ancho); no distinguirlas.

## 6. Filtro por jugador

- **Decision**: Fuera de alcance.
- **Rationale**: No hay un acceso natural en la UI y suma tests a una semana de la entrega.
- **Alternatives considered**: Link desde el detalle; selector de jugador.
