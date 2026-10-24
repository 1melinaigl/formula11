import { http, HttpResponse } from 'msw'

const cuenta = {
  id: 11,
  nombre: 'Ana Formula',
  email: 'ana@example.com',
  fechaRegistro: '2026-09-23T12:00:00Z',
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
  http.get('/api/usuarios/:id/portfolio', () => HttpResponse.json({ saldo: 100, tenencias: [] })),
  http.post('/api/ordenes/compra', async ({ request }) => {
    const { cantidad } = await request.json()
    return HttpResponse.json({ id: 1, tipo: 'COMPRA', cantidad, precio: 1, total: cantidad }, { status: 201 })
  }),
  http.post('/api/ordenes/venta', async ({ request }) => {
    const { cantidad } = await request.json()
    return HttpResponse.json({ id: 2, tipo: 'VENTA', cantidad, precio: 1, total: cantidad }, { status: 201 })
  }),
]
