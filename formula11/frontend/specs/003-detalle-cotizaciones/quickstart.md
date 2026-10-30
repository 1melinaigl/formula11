# Quickstart: Detalle de jugador con evolución de cotizaciones

## Prerequisites

- Node.js compatible with the repository.
- Dependencies installed with `npm install`.
- Work from `frontend/`.

## Automated validation

```powershell
npm install
npm run test
npm run lint
npm run build
```

Expected result: the Vitest suite passes, ESLint reports no errors, and Vite produces
the application build with the detail page and chart split into lazy-loaded chunks.

## MSW scenarios

The test server supplies the Entrega 2 shapes for:

- `GET /api/jugadores/:id`
- `GET /api/jugadores/:id/cotizaciones`
- `GET /api/estrategias` once T018 is implemented

Override handlers in a focused test to cover:

1. Authenticated `USUARIO`: only the active strategy is visible.
2. Authenticated `SUPERUSUARIO`: both strategy histories are available and the active
   one is marked from `GET /api/estrategias`.
3. Player without quotes: ficha visible and `Sin cotizaciones`.
4. Quote-history failure: ficha visible and error with `mensaje` and `correlationId`.
5. Player `404`: player error and no quote panel.
6. No session: access denied and no protected request.
7. Forbidden alternative strategy (`403`) for `USUARIO`.

## Manual flow

1. Start the dev server with `npm run dev`.
2. Open `/catalogo` with an authenticated session from the existing login flow.
3. Open a player card through `Ver cotizaciones`.
4. Check `Volver al catálogo` and `Escape`.
5. Repeat with a session whose `rol` is `SUPERUSUARIO` and inspect both strategy
   histories and the active marker.

The backend real is not required for this validation; MSW provides the contract-shaped
responses. Refer to [frontend-api.md](./contracts/frontend-api.md) for payloads and
[data-model.md](./data-model.md) for field and transformation rules.
