import { http, HttpResponse } from 'msw'

const cuenta = {
  id: 11,
  nombre: 'Ana Formula',
  email: 'ana@example.com',
  fechaRegistro: '2026-09-23T12:00:00Z',
}

function errorResponse(request, mensaje, status) {
  return HttpResponse.json(
    { mensaje, timestamp: new Date().toISOString(), correlationId: request.headers.get('X-Correlation-Id') },
    { status },
  )
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
  http.get('/api/estrategias', ({ request }) => {
    if (new URL(request.url).searchParams.get('error') === '503') {
      return errorResponse(request, 'No se pudo obtener la configuración de estrategias.', 503)
    }
    return HttpResponse.json([
    { codigo: 'RENDIMIENTO_POSICION', nombre: 'Rendimiento por posición', version: 1, activa: true, pesos: {}, parametros: {}, creadaEn: '2026-10-01T00:00:00Z' },
    { codigo: 'CONSISTENCIA', nombre: 'Consistencia', version: 1, activa: false, pesos: {}, parametros: {}, creadaEn: '2026-10-01T00:00:00Z' },
    ])
  }),
  http.get('/api/jugadores/:id', ({ params, request }) => {
    if (params.id === '401') return errorResponse(request, 'La sesión expiró.', 401)
    if (params.id === '404') return errorResponse(request, 'Jugador no encontrado.', 404)
    return HttpResponse.json({
      id: Number(params.id),
      nombre: 'Nicolò Barella',
      equipo: 'FC Internazionale Milano',
      liga: 'Serie A',
      posicion: 'MED',
      cotizacionActual: null,
    })
  }),
  http.get('/api/jugadores/:id/cotizaciones', ({ params, request }) => {
    const estrategia = new URL(request.url).searchParams.get('estrategia')
    if (params.id === '401') return errorResponse(request, 'La sesión expiró.', 401)
    if (params.id === '404') return errorResponse(request, 'No se encontraron cotizaciones.', 404)
    if (estrategia === 'NO_PERMITIDA') return errorResponse(request, 'La estrategia no está permitida para este usuario.', 403)
    if (estrategia === 'ERROR_PROVEEDOR') return errorResponse(request, 'El proveedor de cotizaciones no está disponible.', 500)
    return HttpResponse.json({
      jugadorId: Number(params.id),
      estrategia: estrategia || 'RENDIMIENTO_POSICION',
      cotizaciones: [],
    })
  }),
]
