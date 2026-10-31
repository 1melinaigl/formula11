import { http, HttpResponse } from 'msw'

const cuenta = {
  id: 11,
  nombre: 'Ana Formula',
  email: 'ana@example.com',
  fechaRegistro: '2026-09-23T12:00:00Z',
}

// OperacionResponse del contrato, a precio inicial (1 crédito) contra el superusuario.
function operacionSimulada(tipo, { jugadorId, cantidad }, id) {
  return HttpResponse.json({
    id,
    tipo,
    jugador: { id: jugadorId, nombre: 'Nicolò Barella', equipo: 'FC Internazionale Milano', liga: 'Serie A', posicion: 'MED' },
    cantidad,
    precioUnitario: 1,
    total: cantidad,
    saldoPosterior: tipo === 'COMPRA' ? 100 - cantidad : 100 + cantidad,
    contraparte: 'superusuario',
    fecha: '2026-10-24T12:00:00Z',
    estrategia: null,
    version: null,
  }, { status: 201 })
}

export const handlers = [
  http.post('/api/usuarios/login', async ({ request }) => {
    const body = await request.json()

    if (body.email === 'invalida@example.com') {
      return HttpResponse.json(
        { mensaje: 'Email o contraseña incorrectos.', timestamp: new Date().toISOString(), correlationId: request.headers.get('X-Correlation-Id') },
        { status: 401 },
      )
    }

    return HttpResponse.json({ ...cuenta, token: 'test-token' })
  }),
  http.post('/api/usuarios/registro', async ({ request }) => {
    const body = await request.json()

    if (body.email === 'duplicado@example.com') {
      return HttpResponse.json(
        { mensaje: 'El email ya está registrado.', timestamp: new Date().toISOString(), correlationId: request.headers.get('X-Correlation-Id') },
        { status: 409 },
      )
    }

    return HttpResponse.json({ ...cuenta, ...body, token: 'test-token' }, { status: 201 })
  }),
  http.get('/api/jugadores', () => HttpResponse.json({
    content: [],
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0,
  })),
  // Entrega 2: formas de backend/specs/005-contratos-entrega2/contracts/openapi.yaml.
  http.get('/api/jugadores/ranking', () => HttpResponse.json({
    content: [],
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0,
    estrategia: 'RENDIMIENTO_POSICION',
    version: null,
    fecha: null,
  })),
  http.get('/api/estrategias', () => HttpResponse.json([
    { codigo: 'RENDIMIENTO_POSICION', nombre: 'Rendimiento por posición', version: 1, activa: true, pesos: {}, parametros: {}, creadaEn: '2026-10-01T00:00:00Z' },
    { codigo: 'CONSISTENCIA', nombre: 'Consistencia', version: 1, activa: false, pesos: {}, parametros: {}, creadaEn: '2026-10-01T00:00:00Z' },
  ])),
  http.get('/api/jugadores/:id', ({ params }) => HttpResponse.json({
    id: Number(params.id),
    nombre: 'Nicolò Barella',
    equipo: 'FC Internazionale Milano',
    liga: 'Serie A',
    posicion: 'MED',
    cotizacionActual: null,
  })),
  http.get('/api/jugadores/:id/cotizaciones', ({ params, request }) => HttpResponse.json({
    jugadorId: Number(params.id),
    estrategia: new URL(request.url).searchParams.get('estrategia') || 'RENDIMIENTO_POSICION',
    cotizaciones: [],
  })),
  http.get('/api/jugadores/:id/mercado', ({ params }) => HttpResponse.json({
    jugadorId: Number(params.id),
    tokensEmitidos: 100,
    tokensDisponibles: 100,
    precioVigente: 1,
    fechaCotizacion: null,
  })),
  http.get('/api/usuarios/:id/portfolio', ({ params }) => HttpResponse.json({
    usuarioId: Number(params.id),
    fechaValuacion: '2026-10-19',
    saldo: 100,
    invertido: 0,
    valorTenencias: 0,
    ganancia: 0,
    gananciaPorcentaje: 0,
    posiciones: [],
  })),
  http.post('/api/ordenes/compra', async ({ request }) => operacionSimulada('COMPRA', await request.json(), 1)),
  http.post('/api/ordenes/venta', async ({ request }) => operacionSimulada('VENTA', await request.json(), 2)),
  http.get('/api/usuarios/:id/portfolio/evolucion', ({ params }) => HttpResponse.json({ usuarioId: Number(params.id), puntos: [] })),
  http.get('/api/usuarios/:id/transacciones', () => HttpResponse.json({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 })),
]
