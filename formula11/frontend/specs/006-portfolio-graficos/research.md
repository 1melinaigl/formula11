# Research: Portfolio y gráficos del usuario

No quedaron puntos `NEEDS CLARIFICATION`. Decisiones de diseño y alternativas.

## 1. Totales del resumen

- **Decision**: Tomar `invertido`, `valorTenencias`, `ganancia` y `gananciaPorcentaje`
  del `PortfolioResponse`; el valor total es `saldo` + `valorTenencias`.
- **Rationale**: El backend valúa con la cotización vigente de la estrategia activa;
  recalcular en el front podría diferir por redondeo o por la estrategia.
- **Alternatives considered**: Sumar las posiciones en el front (descartado: duplica la
  regla y puede no coincidir con el backend).

## 2. Superusuario

- **Decision**: Sin ítem "Mi portfolio" en el menú; por URL, un aviso y ninguna consulta.
- **Rationale**: Es la contraparte de todas las operaciones; su "portfolio" serían
  todos los tokens sin vender, sin sentido como inversión (coherente con el 005).
- **Alternatives considered**: Mostrar su portfolio sin "Vender" (lista enorme y
  confusa); igual que un usuario (contradice el 005).

## 3. Rango de la evolución

- **Decision**: Toda la historia, sin `desde` ni `hasta` y sin selector.
- **Rationale**: Una temporada tiene menos de 52 puntos; un selector suma controles y
  tests sin aportar a la demo.
- **Alternatives considered**: Selector de rango; últimas 12 semanas fijas.

## 4. Poca historia

- **Decision**: Con 0 o 1 punto, en lugar del gráfico se muestra una tarjeta con un
  aviso explicativo.
- **Rationale**: Al inicio del campeonato (escenario 3 de la demo) el gráfico no
  aparecería y parecería un error.
- **Alternatives considered**: No mostrar nada (estado actual); dibujar un solo punto.

## 5. Orden de la tabla y la composición

- **Decision**: Mayor valor actual primero y, a igual valor, por nombre, en ambos.
- **Rationale**: El contrato no define el orden de las posiciones; un orden común hace
  que el mismo jugador ocupe la misma posición en la tabla y en el gráfico y deja los
  tests deterministas.
- **Alternatives considered**: Orden del backend (puede variar); alfabético (no
  destaca las tenencias principales).

## 6. Alternativa textual de la evolución

- **Decision**: Una tabla oculta visualmente ("Evolución semanal": fecha, valor,
  invertido) dentro de la tarjeta del gráfico.
- **Rationale**: Composición y ganancia ya están en la tabla visible; la evolución no.
- **Alternatives considered**: Un `details` visible como en el 003 (agrega altura a una
  página ya larga); solo `aria-label` (no transmite los valores).
