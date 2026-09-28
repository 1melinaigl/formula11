# Quickstart: Registro, login y catálogo

## Prerrequisitos

- Node.js compatible con el proyecto instalado.
- Dependencias del frontend instaladas con `npm install`.
- Backend Formula11 ejecutándose en `http://localhost:8080`.
- El proxy local de Vite configurado para `/api`.

## Desarrollo

```powershell
npm run dev
```

Abrir la URL local que informe Vite y comprobar:

1. El menú muestra `Sin sesión`, permite flechas/Enter/Esc y bloquea Ranking y Mercado con
   `PRÓXIMAMENTE`.
2. `/ingresar` permite alternar con `/registro`; los datos inválidos se muestran bajo los
   campos y los errores del servicio muestran mensaje y `correlationId`.
3. Un registro válido muestra id, nombre, email y fecha de registro, pero nunca token.
4. Después de recargar, el chip del nombre mantiene la sesión; `/catalogo` carga una sola
   colección y filtra nombre, liga, equipo y posición localmente.
5. Un `401` de catálogo muestra `Acceso denegado` y limpia la sesión; cerrar sesión vuelve
   al menú.
6. En un ancho de 760px o menor, lista y ficha se alternan y la acción volver funciona.

## Validación automatizada

```powershell
npm run lint
npm run build
npm run test
```

La suite con MSW debe cubrir respuestas exitosas, `400`, `401`, `409`, catálogo vacío,
restauración de sesión, logout, navegación de teclado y la ausencia del token en el DOM.

## Referencias

- Contrato del cliente: [contracts/api-client.md](contracts/api-client.md)
- Entidades y estados: [data-model.md](data-model.md)
- Requisitos: [../001-registro-login-catalogo/spec.md](../001-registro-login-catalogo/spec.md)
