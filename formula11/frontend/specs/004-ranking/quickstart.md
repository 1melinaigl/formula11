# Quickstart: Ranking de jugadores

## Prerequisites

- Node.js compatible con el repositorio.
- Dependencias instaladas con `npm install`.
- Ejecutar desde `frontend/`.

## Automated validation

```powershell
npm test
npm run lint
npm run build
```

Resultado esperado: la suite Vitest pasa, ESLint no informa errores y Vite genera el
build de la aplicación.

## MSW scenarios

Sobrescribir el handler de `/api/jugadores/ranking` dentro de cada test para validar:

1. Respuesta contractual con filas anidadas, orden y puestos recibidos.
2. Headers protegidos y ausencia de `estrategia` en la consulta.
3. Filtro `MED` y liga `Serie A` (valor del enum `Liga` enviado igual a la etiqueta),
   con reinicio a `page=0`.
4. Segunda página con `page=1`, UI mostrando `Página 2`.
5. `content: []` sin filtros: `Ranking vacío`.
6. `content: []` con filtro: `Sin jugadores`.
7. Metadatos `estrategia` presente, `version` y `fecha` nulos:
   `Estrategia activa: <nombre> · sin recálculo todavía`.
8. Errores `400`, `401`, `403` y `5xx` con `mensaje` y `correlationId`; si falta
   `correlationId`, se usa el `X-Correlation-Id` generado por `clienteApi`, y si falta
   `mensaje`, se muestra `No se pudo completar la solicitud.`.
9. Ausencia de sesión: acceso denegado y cero llamadas protegidas.
10. Navegación desde el menú a `/ranking`, desde el nombre a `/jugadores/:id` y con
    `Escape` de vuelta a `/`.

## Manual flow

1. Ejecutar `npm run dev`.
2. Iniciar sesión con un usuario existente.
3. Abrir `Ranking` desde el menú.
4. Verificar puesto, posición, jugador, precio, variación y score.
5. Cambiar una posición y una liga; comprobar que la consulta se realiza al backend y
   vuelve a la primera página.
6. Avanzar de página y comprobar que la UI muestra números desde 1 mientras la API
   usa índices desde 0.
7. Reducir el viewport a 760px o menos y comprobar que Equipo y Score no desbordan la
   tabla.
8. Volver al menú con el botón o `Escape`.

Los payloads completos y estados de error están en
[frontend-api.md](./contracts/frontend-api.md); las entidades y reglas de
normalización están en [data-model.md](./data-model.md).
