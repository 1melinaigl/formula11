import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import App from '../../src/App'
import { SesionProvider } from '../../src/context/SesionContext'
import { server } from '../../src/test/server'

const barella = { id: 7, nombre: 'Nicolò Barella', equipo: 'FC Internazionale Milano', liga: 'Serie A', posicion: 'MED', cotizacionActual: null }

// HistorialCotizacionesResponse del contrato: variacion en porcentaje (10 = 10 %), null en la primera semana.
const historial = {
  RENDIMIENTO_POSICION: {
    jugadorId: 7,
    estrategia: 'RENDIMIENTO_POSICION',
    cotizaciones: [
      { jugadorId: 7, fecha: '2026-10-12', precio: 1.1, score: 0.62, variacion: null, estrategia: 'RENDIMIENTO_POSICION', version: 1 },
      { jugadorId: 7, fecha: '2026-10-19', precio: 1.21, score: 0.75, variacion: 10, estrategia: 'RENDIMIENTO_POSICION', version: 2 },
    ],
  },
  CONSISTENCIA: {
    jugadorId: 7,
    estrategia: 'CONSISTENCIA',
    cotizaciones: [
      { jugadorId: 7, fecha: '2026-10-12', precio: 1.05, score: 0.55, variacion: null, estrategia: 'CONSISTENCIA', version: 1 },
      { jugadorId: 7, fecha: '2026-10-19', precio: 0.95, score: 0.31, variacion: -9.52, estrategia: 'CONSISTENCIA', version: 1 },
    ],
  },
}

function renderDetalle(ruta = '/jugadores/7', sesion = true, rol = 'USUARIO') {
  window.history.pushState({}, '', ruta)
  if (sesion) {
    window.sessionStorage.setItem('formula11.session', JSON.stringify({ token: 'detalle-token', usuario: { id: 11, nombre: 'Ana Formula', rol } }))
  }
  return render(<SesionProvider><App /></SesionProvider>)
}

describe('detalle de jugador con cotizaciones', () => {
  beforeEach(() => window.sessionStorage.clear())

  it('muestra la ficha, la cotización vigente de la estrategia activa y el historial', async () => {
    const estrategiasPedidas = []
    server.use(
      http.get('/api/jugadores/:id', ({ request, params }) => {
        expect(params.id).toBe('7')
        expect(request.headers.get('Authorization')).toBe('Bearer detalle-token')
        expect(request.headers.get('X-Correlation-Id')).toBeTruthy()
        return HttpResponse.json(barella)
      }),
      http.get('/api/jugadores/:id/cotizaciones', ({ request }) => {
        expect(request.headers.get('Authorization')).toBe('Bearer detalle-token')
        estrategiasPedidas.push(new URL(request.url).searchParams.get('estrategia'))
        return HttpResponse.json(historial.RENDIMIENTO_POSICION)
      }),
    )
    renderDetalle()

    expect(await screen.findByRole('heading', { name: 'Nicolò Barella' })).toBeInTheDocument()
    expect(screen.getByText('FC Internazionale Milano', { selector: 'dd' })).toBeInTheDocument()
    expect(screen.getByText('Mediocampista', { selector: 'dd' })).toBeInTheDocument()
    expect(estrategiasPedidas).toEqual([null])

    const resumen = screen.getByRole('list', { name: 'Cotización vigente' })
    const [rendimiento] = within(resumen).getAllByRole('listitem')
    expect(within(resumen).getAllByRole('listitem')).toHaveLength(1)
    expect(within(rendimiento).getByText('Rendimiento por posición')).toBeInTheDocument()
    expect(within(rendimiento).getByText(/1,21/)).toBeInTheDocument()
    expect(within(rendimiento).getByText('+10,0% vs. semana anterior')).toHaveClass('up')
    expect(within(rendimiento).getByText(/Al 19\/10\/2026/)).toBeInTheDocument()

    const filas = within(screen.getByRole('table')).getAllByRole('row')
    expect(filas).toHaveLength(3)
    expect(within(filas[1]).getByText('19/10/2026')).toBeInTheDocument()
    expect(within(filas[1]).getByText('Rendimiento por posición v2')).toBeInTheDocument()
  })

  it('al superusuario le muestra las dos estrategias para compararlas', async () => {
    const estrategiasPedidas = []
    server.use(http.get('/api/jugadores/:id/cotizaciones', ({ request }) => {
      const estrategia = new URL(request.url).searchParams.get('estrategia')
      estrategiasPedidas.push(estrategia)
      return HttpResponse.json(historial[estrategia])
    }))
    renderDetalle('/jugadores/7', true, 'SUPERUSUARIO')

    const resumen = await screen.findByRole('list', { name: 'Cotización vigente' })
    const [rendimiento, consistencia] = within(resumen).getAllByRole('listitem')
    expect(within(rendimiento).getByText('Rendimiento por posición')).toBeInTheDocument()
    expect(within(consistencia).getByText('Consistencia')).toBeInTheDocument()
    expect(within(consistencia).getByText('-9,5% vs. semana anterior')).toHaveClass('down')
    expect(estrategiasPedidas.sort()).toEqual(['CONSISTENCIA', 'RENDIMIENTO_POSICION'])
    expect(within(screen.getByRole('table')).getAllByRole('row')).toHaveLength(5)
  })

  it('indica que no hay cotización anterior en la primera semana', async () => {
    server.use(http.get('/api/jugadores/:id/cotizaciones', () => HttpResponse.json({
      ...historial.RENDIMIENTO_POSICION,
      cotizaciones: historial.RENDIMIENTO_POSICION.cotizaciones.slice(0, 1),
    })))
    renderDetalle()
    expect(await screen.findByText('Sin cotización anterior')).toBeInTheDocument()
  })

  it('muestra el estado vacío cuando el jugador no tiene cotizaciones', async () => {
    renderDetalle()
    expect(await screen.findByRole('heading', { name: 'Nicolò Barella' })).toBeInTheDocument()
    expect(screen.getByText('Sin cotizaciones')).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })

  it('mantiene la ficha visible si fallan solo las cotizaciones', async () => {
    server.use(http.get('/api/jugadores/:id/cotizaciones', () => HttpResponse.json(
      { mensaje: 'No se pudieron obtener las cotizaciones.', correlationId: 'quotes-500' },
      { status: 500 },
    )))
    renderDetalle()
    expect(await screen.findByRole('heading', { name: 'Nicolò Barella' })).toBeInTheDocument()
    expect(screen.getByRole('alert')).toHaveTextContent('No se pudieron obtener las cotizaciones.')
    expect(screen.getByText(/quotes-500/)).toBeInTheDocument()
  })

  it('informa el error si el jugador no existe', async () => {
    server.use(http.get('/api/jugadores/:id', () => HttpResponse.json(
      { mensaje: 'Jugador no encontrado.', correlationId: 'player-404' },
      { status: 404 },
    )))
    renderDetalle('/jugadores/999')
    expect(await screen.findByRole('alert')).toHaveTextContent('Jugador no encontrado.')
    expect(screen.queryByText('Evolución de la cotización')).not.toBeInTheDocument()
  })

  it('no consulta la API cuando no hay sesión', () => {
    let consultas = 0
    server.use(http.get('/api/jugadores/:id', () => { consultas += 1; return HttpResponse.json(barella) }))
    renderDetalle('/jugadores/7', false)
    expect(screen.queryByText('Cargando jugador...')).not.toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Detalle del jugador' })).not.toBeInTheDocument()
    expect(consultas).toBe(0)
  })

  it('se abre desde la ficha del catálogo y vuelve al catálogo', async () => {
    server.use(http.get('/api/jugadores', () => HttpResponse.json({ content: [barella], page: 0, size: 9, totalElements: 1, totalPages: 1 })))
    const user = userEvent.setup()
    renderDetalle('/catalogo')

    await user.click(await screen.findByRole('button', { name: /Nicolò Barella/ }))
    await user.click(screen.getByRole('link', { name: 'Ver cotizaciones' }))
    expect(await screen.findByRole('heading', { name: 'Detalle del jugador' })).toBeInTheDocument()
    expect(window.location.pathname).toBe('/jugadores/7')

    await user.click(screen.getByRole('button', { name: 'Volver al catálogo' }))
    expect(await screen.findByRole('heading', { name: 'Catálogo de jugadores' })).toBeInTheDocument()
    window.history.pushState({}, '', '/')
  })
})
