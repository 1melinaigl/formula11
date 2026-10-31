# Quickstart: Señales de jugadores subvaluados y panel de aciertos

Las formas de los datos están en [contracts/frontend-api.md](./contracts/frontend-api.md)
y las reglas, en [data-model.md](./data-model.md).

## Prerrequisitos

- Dependencias instaladas en `formula11/frontend` (`npm install`).
- Para la validación manual: el backend de `dev` en `http://localhost:8080`, con la
  base de la demo ya recalculada (con señales vigentes y semanas medidas), un
  `USUARIO` demo con saldo y el `SUPERUSUARIO`.

## Validación automática

```bash
npm test
npm run lint
npm run build
```

- Las reglas (normalizar, señales nuevas, registro de vistas y formato) están en
  `tests/components/senales.test.js`.
- La alerta (nueva, repetida, consulta fallida, `401` y logout, con timers falsos)
  está en `tests/pages/alertaSenales.test.jsx`.
- La lista, el historial, la compra (`201`, `409`, `422`), el superusuario y el
  panel (con datos y vacío) están en `tests/pages/senales.test.jsx`.
- Los tests existentes tienen que seguir pasando sin cambios, gracias a los
  handlers por defecto.

## Validación manual

1. Iniciar sesión como `USUARIO`. En menos de 30 segundos aparece la alerta
   "N oportunidades nuevas", con las 3 de mayor brecha, en la pantalla en que se
   esté.
2. Cerrarla con `Escape` o con "Cerrar". Navegar y esperar más de 30 segundos: no
   tiene que volver a aparecer. Recargar la página: tampoco.
3. Cerrar la sesión y volver a entrar: la alerta aparece de nuevo.
4. Desde "Ver señales" o el menú "Señales", revisar la tabla de vigentes: tipo
   legible, brecha "+NN,NN%", precio, precio estimado y semana. Alternar a
   "Historial" y paginar.
5. Pulsar "Comprar 1 token" en una fila: aparece la confirmación con el total y el
   saldo, y el link "Ver historial" muestra la compra. Repetir sin saldo para ver
   el `422` con su `correlationId`.
6. Revisar el panel "Aciertos": la tasa, la diferencia y la tabla por semana tienen
   que coincidir con `GET /api/senales/aciertos`.
7. Entrar como `SUPERUSUARIO`: ve las señales y el panel, pero no los botones de
   compra.
8. Achicar la ventana a 760px y a 375px: se ocultan las columnas indicadas en
   FR-010 y no aparece scroll horizontal en la página.
9. Recorrer todo con el teclado: menú, alerta, Vigentes/Historial con
   `aria-pressed`, paginación y botones de compra.

Al validar, tener en cuenta que comprar cambia los datos de la demo. Conviene
hacerlo con un usuario de prueba o restaurar el backup después.
