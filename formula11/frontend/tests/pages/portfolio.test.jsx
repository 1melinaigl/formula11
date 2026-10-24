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

function posicionDe(id, nombre, valorActual, ganancia = 0) {
  return {
    jugador: { id, nombre, equipo: 'Equipo', liga: 'Serie A', posicion: 'MED' },
    cantidad: 1, precioPromedio: valorActual - ganancia, precioActual: valorActual, invertido: valorActual - ganancia,
    valorActual, ganancia, gananciaPorcentaje: 0,
  }
}

function renderComo(usuario, ruta = '/portfolio') {
  window.history.pushState({}, '', ruta)
  window.sessionStorage.setItem('formula11.session', JSON.stringify({ token: 'portfolio-token', usuario }))
  return render(<SesionProvider><App /></SesionProvider>)
}

function usarEvolucion(puntos) {
  server.use(http.get('/api/usuarios/:id/portfolio/evolucion', () => HttpResponse.json({ usuarioId: 11, puntos })))
}

describe('portfolio: orden, roles, poca historia y estados (006)', () => {
  beforeEach(() => window.sessionStorage.clear())

  it('ordena la tabla y la composición por valor actual, con desempate por nombre', async () => {
    server.use(http.get('/api/usuarios/:id/portfolio', () => HttpResponse.json({
      ...portfolio,
      posiciones: [posicionDe(1, 'Zeta', 2), posicionDe(2, 'Kane', 9), posicionDe(3, 'Alfa', 2)],
    })))
    usarEvolucion([])
    renderPortfolio()

    const tabla = await screen.findByRole('table', { name: 'Tokens por jugador' })
    const nombres = within(tabla).getAllByRole('link', { name: /^(Zeta|Kane|Alfa)$/ }).map((link) => link.textContent)
    expect(nombres).toEqual(['Kane', 'Alfa', 'Zeta'])
  })

  it('muestra "Pérdida" con ganancia negativa y "Sin inversión" si no hay inversión', async () => {
    server.use(http.get('/api/usuarios/:id/portfolio', () => HttpResponse.json({
      ...portfolio, invertido: 10, valorTenencias: 8, ganancia: -2, gananciaPorcentaje: -20,
    })))
    const { unmount } = renderPortfolio()
    const resumen = await screen.findByRole('list', { name: 'Resumen del portfolio' })
    expect(within(resumen).getByText('Pérdida')).toBeInTheDocument()
    expect(within(resumen).getByText('-20,0% sobre lo invertido')).toHaveClass('down')
    unmount()

    server.use(http.get('/api/usuarios/:id/portfolio', () => HttpResponse.json({
      ...portfolio, invertido: 0, valorTenencias: 0, ganancia: 0, gananciaPorcentaje: 0, posiciones: [],
    })))
    renderPortfolio()
    expect(await screen.findByText('Sin inversión')).toBeInTheDocument()
  })

  it('al superusuario no le muestra el portfolio ni consulta la API', async () => {
    let consultas = 0
    server.use(
      http.get('/api/usuarios/:id/portfolio', () => { consultas += 1; return HttpResponse.json(portfolio) }),
      http.get('/api/usuarios/:id/portfolio/evolucion', () => { consultas += 1; return HttpResponse.json({ usuarioId: 1, puntos: [] }) }),
    )
    const { unmount } = renderComo({ id: 1, nombre: 'Admin', rol: 'SUPERUSUARIO' }, '/')
    expect(screen.queryByRole('button', { name: /Mi portfolio/ })).not.toBeInTheDocument()
    unmount()

    renderComo({ id: 1, nombre: 'Admin', rol: 'SUPERUSUARIO' })
    expect(await screen.findByText('El superusuario es la contraparte del mercado y no tiene un portfolio de inversión.')).toBeInTheDocument()
    expect(screen.queryByRole('list', { name: 'Resumen del portfolio' })).not.toBeInTheDocument()
    expect(consultas).toBe(0)
  })

  it('muestra el aviso de poca historia con un solo punto de evolución', async () => {
    server.use(http.get('/api/usuarios/:id/portfolio', () => HttpResponse.json(portfolio)))
    usarEvolucion([{ fecha: '2026-10-19', valor: 19, invertido: 15 }])
    renderPortfolio()

    expect(await screen.findByText('Todavía no hay suficiente historia para mostrar la evolución (hace falta al menos una semana más de cotizaciones).')).toBeInTheDocument()
    expect(screen.getByText('Valor del portfolio vs. invertido')).toBeInTheDocument()
    expect(screen.queryByRole('table', { name: 'Evolución semanal' })).not.toBeInTheDocument()
  })

  it('incluye la tabla "Evolución semanal" como alternativa textual del gráfico', async () => {
    server.use(http.get('/api/usuarios/:id/portfolio', () => HttpResponse.json(portfolio)))
    usarEvolucion([
      { fecha: '2026-10-19', valor: 19, invertido: 15 },
      { fecha: '2026-10-12', valor: 15, invertido: 15 },
    ])
    renderPortfolio()

    const tabla = await screen.findByRole('table', { name: 'Evolución semanal' })
    const filas = within(tabla).getAllByRole('row')
    expect(filas).toHaveLength(3)
    expect(within(filas[1]).getByText('12/10/2026')).toBeInTheDocument()
    expect(within(filas[2]).getByText('19,00')).toBeInTheDocument()
  })

  it('con más de 7 tenencias la tabla las lista todas y la composición agrupa "Otros"', async () => {
    const posiciones = Array.from({ length: 9 }, (_, i) => posicionDe(i + 1, `Jugador ${i + 1}`, 10 - i))
    server.use(http.get('/api/usuarios/:id/portfolio', () => HttpResponse.json({ ...portfolio, posiciones })))
    usarEvolucion([])
    renderPortfolio()

    const tabla = await screen.findByRole('table', { name: 'Tokens por jugador' })
    expect(within(tabla).getAllByRole('row')).toHaveLength(10)
    expect(screen.getByText('Composición por jugador')).toBeInTheDocument()
  })

  it('muestra el estado de carga', async () => {
    let responder
    server.use(http.get('/api/usuarios/:id/portfolio', () => new Promise((resolve) => { responder = resolve })))
    renderPortfolio()
    expect(await screen.findByText('Cargando portfolio...')).toBeInTheDocument()
    responder(HttpResponse.json(portfolio))
    expect(await screen.findByRole('list', { name: 'Resumen del portfolio' })).toBeInTheDocument()
  })

  it('un 401 en el portfolio limpia la sesión y muestra el acceso denegado', async () => {
    server.use(http.get('/api/usuarios/:id/portfolio', () => HttpResponse.json(
      { mensaje: 'Sesión vencida.', timestamp: '2026-10-24T12:00:00Z', correlationId: 'pf-401' },
      { status: 401 },
    )))
    renderPortfolio()
    expect(await screen.findByRole('heading', { name: 'Acceso denegado' })).toBeInTheDocument()
    expect(window.sessionStorage.getItem('formula11.session')).toBeNull()
  })

  it('si falla el portfolio solo muestra el error aunque la evolución responda', async () => {
    server.use(http.get('/api/usuarios/:id/portfolio', () => HttpResponse.json(
      { mensaje: 'Portfolio no disponible.', timestamp: '2026-10-24T12:00:00Z', correlationId: 'pf-503' },
      { status: 503 },
    )))
    usarEvolucion([
      { fecha: '2026-10-12', valor: 15, invertido: 15 },
      { fecha: '2026-10-19', valor: 19, invertido: 15 },
    ])
    renderPortfolio()
    expect(await screen.findByRole('alert')).toHaveTextContent('Portfolio no disponible.')
    expect(screen.queryByText('Valor del portfolio vs. invertido')).not.toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })

  it('no muestra el token de la sesión en la página', async () => {
    server.use(http.get('/api/usuarios/:id/portfolio', () => HttpResponse.json(portfolio)))
    renderPortfolio()
    await screen.findByRole('list', { name: 'Resumen del portfolio' })
    expect(document.body.textContent).not.toContain('portfolio-token')
  })
})
