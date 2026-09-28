import { http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'
import { solicitar } from '../../src/api/clienteApi'
import { server } from '../../src/test/server'

describe('cliente API', () => {
  it('envía correlationId y Authorization cuando hay token', async () => {
    let receivedHeaders
    server.use(http.get('/api/test', ({ request }) => {
      receivedHeaders = request.headers
      return HttpResponse.json({ ok: true })
    }))

    await solicitar('/api/test', { token: 'token-secreto', protegida: true })

    expect(receivedHeaders.get('X-Correlation-Id')).toBeTruthy()
    expect(receivedHeaders.get('Authorization')).toBe('Bearer token-secreto')
  })

  it('normaliza el error y limpia sesión solo en un 401 protegido', async () => {
    const onUnauthorized = vi.fn()
    server.use(http.get('/api/test', () => HttpResponse.json({ mensaje: 'No autorizado.', correlationId: 'server-id' }, { status: 401 })))

    await expect(solicitar('/api/test', { protegida: true, onUnauthorized })).rejects.toMatchObject({ api: { correlationId: 'server-id', status: 401 } })
    expect(onUnauthorized).toHaveBeenCalledOnce()
  })
})
