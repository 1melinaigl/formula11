import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import App from '../../src/App'
import { SesionProvider } from '../../src/context/SesionContext'
import { server } from '../../src/test/server'

const barella = { id: 7, nombre: 'Nicolò Barella', equipo: 'FC Internazionale Milano', liga: 'Serie A', posicion: 'MED', cotizacionActual: null }

function renderDetalle() {
  window.history.pushState({}, '', '/jugadores/7')
  window.sessionStorage.setItem('formula11.session', JSON.stringify({ token: 'fecha-token', usuario: { id: 11, nombre: 'Ana Formula', rol: 'USUARIO' } }))
  server.use(
    http.get('/api/jugadores/:id', () => HttpResponse.json(barella)),
    http.get('/api/jugadores/:id/cotizaciones', () => HttpResponse.json({ jugadorId: 7, estrategia: 'RENDIMIENTO_POSICION', cotizaciones: [] })),
  )
  return render(<SesionProvider><App /></SesionProvider>)
}

describe('cotización de un jugador a una fecha', () => {
  beforeEach(() => window.sessionStorage.clear())

  it('consulta la cotización vigente a la fecha elegida y la muestra', async () => {
    let fechaPedida = null
    server.use(http.get('/api/jugadores/:id/cotizaciones/vigente', ({ request, params }) => {
      expect(params.id).toBe('7')
      expect(request.headers.get('Authorization')).toBe('Bearer fecha-token')
      fechaPedida = new URL(request.url).searchParams.get('fecha')
      return HttpResponse.json({ jugadorId: 7, fecha: '2026-09-13', precio: 1.15, score: 0.71, variacion: 10, estrategia: 'RENDIMIENTO_POSICION', version: 1 })
    }))
    renderDetalle()

    const campo = await screen.findByLabelText('Cotización a una fecha')
    const consultar = screen.getByRole('button', { name: 'Consultar' })
    expect(consultar).toBeDisabled()

    fireEvent.change(campo, { target: { value: '2026-09-17' } })
    await userEvent.click(consultar)

    const resultado = await screen.findByText(/valía/)
    expect(fechaPedida).toBe('2026-09-17')
    expect(resultado).toHaveTextContent('Al 17/09/2026 valía 1,15 créditos')
    expect(resultado).toHaveTextContent('cotización del 13/09/2026, Rendimiento por posición v1, +10,0% vs. semana anterior')
  })

  it('muestra el mensaje del backend si no hay cotizaciones hasta esa fecha', async () => {
    server.use(http.get('/api/jugadores/:id/cotizaciones/vigente', () => HttpResponse.json(
      { mensaje: 'El jugador no tiene cotizaciones hasta la fecha 2026-07-01', timestamp: '2026-11-02T12:00:00Z', correlationId: 'fecha-404' },
      { status: 404 },
    )))
    renderDetalle()

    fireEvent.change(await screen.findByLabelText('Cotización a una fecha'), { target: { value: '2026-07-01' } })
    await userEvent.click(screen.getByRole('button', { name: 'Consultar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('El jugador no tiene cotizaciones hasta la fecha 2026-07-01')
    expect(screen.queryByText(/valía/)).not.toBeInTheDocument()
  })
})
