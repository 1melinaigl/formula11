import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import App from '../../src/App'
import { SesionProvider } from '../../src/context/SesionContext'
import { server } from '../../src/test/server'

// PortfolioResponse del contrato: totales y posiciones ya valuados por el backend; porcentajes en %.
const portfolio = {
  usuarioId: 11,
  fechaValuacion: '2026-10-19',
  saldo: 20,
  invertido: 15,
  valorTenencias: 19,
  ganancia: 4,
  gananciaPorcentaje: 26.67,
  posiciones: [
    {
      jugador: { id: 5, nombre: 'Harry Kane', equipo: 'FC Bayern München', liga: 'Bundesliga', posicion: 'DEL' },
      cantidad: 10, precioPromedio: 1, precioActual: 1.5, invertido: 10, valorActual: 15, ganancia: 5, gananciaPorcentaje: 50,
    },
    {
      jugador: { id: 7, nombre: 'Nicolò Barella', equipo: 'FC Internazionale Milano', liga: 'Serie A', posicion: 'MED' },
      cantidad: 4, precioPromedio: 1.25, precioActual: 1, invertido: 5, valorActual: 4, ganancia: -1, gananciaPorcentaje: -20,
    },
  ],
}

function renderPortfolio({ ruta = '/portfolio', sesion = true } = {}) {
  window.history.pushState({}, '', ruta)
  if (sesion) {
    window.sessionStorage.setItem('formula11.session', JSON.stringify({ token: 'portfolio-token', usuario: { id: 11, nombre: 'Ana Formula', rol: 'USUARIO' } }))
  }
  return render(<SesionProvider><App /></SesionProvider>)
}

describe('portfolio del usuario', () => {
  beforeEach(() => window.sessionStorage.clear())

  it('muestra el resumen, los gráficos y las tenencias con ganancia o pérdida', async () => {
    server.use(
      http.get('/api/usuarios/:id/portfolio', ({ params, request }) => {
        expect(params.id).toBe('11')
        expect(request.headers.get('Authorization')).toBe('Bearer portfolio-token')
        return HttpResponse.json(portfolio)
      }),
      http.get('/api/usuarios/:id/portfolio/evolucion', () => HttpResponse.json({
        usuarioId: 11,
        puntos: [
          { fecha: '2026-10-12', valor: 15, invertido: 15 },
          { fecha: '2026-10-19', valor: 19, invertido: 15 },
        ],
      })),
    )
    renderPortfolio()

    const resumen = await screen.findByRole('list', { name: 'Resumen del portfolio' })
    const tarjetas = within(resumen).getAllByRole('listitem')
    expect(within(tarjetas[0]).getByText(/39,00/)).toBeInTheDocument()
    expect(within(tarjetas[1]).getByText(/20,00/)).toBeInTheDocument()
    expect(within(tarjetas[2]).getByText(/15,00/)).toBeInTheDocument()
    expect(within(tarjetas[3]).getByText(/19,00/)).toBeInTheDocument()
    expect(within(tarjetas[4]).getByText(/\+4,00/)).toBeInTheDocument()
    expect(within(tarjetas[4]).getByText('+26,7% sobre lo invertido')).toHaveClass('up')

    expect(screen.getByText('Valor del portfolio vs. invertido')).toBeInTheDocument()
    expect(screen.getByText('Composición por jugador')).toBeInTheDocument()
    expect(screen.getByText('Ganancia o pérdida por jugador')).toBeInTheDocument()

    const filas = within(screen.getByRole('table', { name: 'Tokens por jugador' })).getAllByRole('row')
    expect(filas).toHaveLength(3)
    expect(within(filas[1]).getByRole('link', { name: 'Harry Kane' })).toHaveAttribute('href', '/jugadores/5')
    expect(within(filas[1]).getByText('+5,00')).toHaveClass('up')
    expect(within(filas[2]).getByText('-1,00')).toHaveClass('down')
    expect(within(filas[2]).getByRole('link', { name: 'Vender tokens de Nicolò Barella' })).toHaveAttribute('href', '/jugadores/7?operacion=venta')
    expect(within(filas[1]).getByText('(+50,0%)')).toBeInTheDocument()
    expect(screen.getByText('Valuado al 19/10/2026')).toBeInTheDocument()
  })

  it('invita a comprar cuando no hay tokens', async () => {
    renderPortfolio()
    expect(await screen.findByText('Todavía no tenés tokens')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'ranking' })).toHaveAttribute('href', '/ranking')
    expect(screen.getAllByText(/100,00/)).toHaveLength(2)
    expect(screen.queryByText('Composición por jugador')).not.toBeInTheDocument()
  })

  it('omite el gráfico de evolución si falla, sin ocultar el resto', async () => {
    server.use(
      http.get('/api/usuarios/:id/portfolio', () => HttpResponse.json(portfolio)),
      http.get('/api/usuarios/:id/portfolio/evolucion', () => HttpResponse.json({ mensaje: 'Serie no disponible.', correlationId: 'evo-500' }, { status: 500 })),
    )
    renderPortfolio()
    expect(await screen.findByRole('alert')).toHaveTextContent('Serie no disponible.')
    expect(screen.queryByText('Valor del portfolio vs. invertido')).not.toBeInTheDocument()
    expect(screen.getByText('Composición por jugador')).toBeInTheDocument()
    expect(screen.getByRole('table', { name: 'Tokens por jugador' })).toBeInTheDocument()
  })

  it('muestra el error del portfolio con correlationId', async () => {
    server.use(http.get('/api/usuarios/:id/portfolio', () => HttpResponse.json({ mensaje: 'Portfolio no disponible.', correlationId: 'pf-503' }, { status: 503 })))
    renderPortfolio()
    expect(await screen.findByRole('alert')).toHaveTextContent('Portfolio no disponible.')
    expect(screen.getByText(/pf-503/)).toBeInTheDocument()
  })

  it('no consulta la API sin sesión', () => {
    let consultas = 0
    server.use(http.get('/api/usuarios/:id/portfolio', () => { consultas += 1; return HttpResponse.json(portfolio) }))
    renderPortfolio({ sesion: false })
    expect(screen.queryByRole('heading', { name: 'Mi portfolio' })).not.toBeInTheDocument()
    expect(consultas).toBe(0)
  })

  it('se abre desde el menú principal', async () => {
    const user = userEvent.setup()
    renderPortfolio({ ruta: '/' })
    await user.click(screen.getByRole('button', { name: /Mi portfolio/ }))
    expect(await screen.findByRole('heading', { name: 'Mi portfolio' })).toBeInTheDocument()
    expect(screen.getByText('Mi portfolio', { selector: '.breadcrumb' })).toBeInTheDocument()
  })
})
