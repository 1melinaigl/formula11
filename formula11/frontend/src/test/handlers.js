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
  http.get('/api/jugadores', () => HttpResponse.json([])),
]
