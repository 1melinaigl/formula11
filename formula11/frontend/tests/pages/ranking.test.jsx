import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import App from '../../src/App'
import { SesionProvider } from '../../src/context/SesionContext'
import { server } from '../../src/test/server'

const jugador = (id, nombre, equipo, liga, posicion) => ({ id, nombre, equipo, liga, posicion })

// RankingItem del contrato: variacion en porcentaje (20 = 20 %), null en la primera semana.
const items = [
  { puesto: 1, jugador: jugador(5, 'Harry Kane', 'FC Bayern München', 'Bundesliga', 'DEL'), precio: 1.44, variacion: 20, score: 0.91 },
  { puesto: 2, jugador: jugador(7, 'Nicolò Barella', 'FC Internazionale Milano', 'Serie A', 'MED'), precio: 1.21, variacion: 10, score: 0.75 },
  { puesto: 3, jugador: jugador(2, 'Emiliano Martinez', 'Aston Villa', 'Premier League', 'POR'), precio: 0.9, variacion: -10, score: 0.4 },
  { puesto: 4, jugador: jugador(3, 'Jude Bellingham', 'Real Madrid', 'La Liga', 'MED'), precio: 1.05, variacion: null, score: 0.38 },
]

function pagina(content, { page = 0, totalPages = 1 } = {}) {
  return { content, page, size: 20, totalElements: content.length, totalPages, estrategia: 'RENDIMIENTO_POSICION', version: 2, fecha: '2026-10-19' }
}

function renderRanking({ sesion = true, ruta = '/ranking' } = {}) {
  window.history.pushState({}, '', ruta)
  if (sesion) {
    window.sessionStorage.setItem('formula11.session', JSON.stringify({ token: 'ranking-token', usuario: { id: 11, nombre: 'Ana Formula', rol: 'USUARIO' } }))
  }
  return render(<SesionProvider><App /></SesionProvider>)
}

function nombresEnTabla() {
  return within(screen.getByRole('table')).getAllByRole('link').map((link) => link.textContent)
}

describe('ranking de jugadores', () => {
  beforeEach(() => window.sessionStorage.clear())

  it('muestra la página del backend con puesto, precio, variación y la estrategia activa', async () => {
    server.use(http.get('/api/jugadores/ranking', ({ request }) => {
      const params = new URL(request.url).searchParams
      expect(request.headers.get('Authorization')).toBe('Bearer ranking-token')
      expect(request.headers.get('X-Correlation-Id')).toBeTruthy()
      expect(params.get('page')).toBe('0')
      expect(params.get('size')).toBe('20')
      expect(params.has('posicion')).toBe(false)
      expect(params.has('estrategia')).toBe(false)
      return HttpResponse.json(pagina(items))
    }))
    renderRanking()

    expect(await screen.findByRole('table')).toBeInTheDocument()
    expect(nombresEnTabla()).toEqual(['Harry Kane', 'Nicolò Barella', 'Emiliano Martinez', 'Jude Bellingham'])
    expect(screen.getByText('Estrategia activa: Rendimiento por posición v2 · al 19/10/2026')).toBeInTheDocument()

    const filas = within(screen.getByRole('table')).getAllByRole('row')
    expect(within(filas[1]).getByText('1')).toBeInTheDocument()
    expect(within(filas[1]).getByText('1,44')).toBeInTheDocument()
    expect(within(filas[1]).getByText('+20,0%')).toHaveClass('up')
    expect(within(filas[3]).getByText('-10,0%')).toHaveClass('down')
    expect(within(filas[4]).getByText('—')).toBeInTheDocument()
    expect(screen.queryByRole('navigation', { name: 'Paginación del ranking' })).not.toBeInTheDocument()
  })

  it('filtra por posición y liga en el backend y respeta el puesto que manda', async () => {
    const consultas = []
    server.use(http.get('/api/jugadores/ranking', ({ request }) => {
      const params = new URL(request.url).searchParams
      consultas.push(`${params.get('posicion') ?? ''}|${params.get('liga') ?? ''}`)
      if (params.get('posicion') === 'DEF') return HttpResponse.json(pagina([]))
      if (params.get('posicion') === 'MED') return HttpResponse.json(pagina([items[1], items[3]]))
      return HttpResponse.json(pagina(items))
    }))
    const user = userEvent.setup()
    renderRanking()
    await screen.findByRole('table')

    await user.click(screen.getByRole('button', { name: 'Mediocampista' }))
    expect(screen.getByRole('button', { name: 'Mediocampista' })).toHaveAttribute('aria-pressed', 'true')
    await screen.findByRole('link', { name: 'Jude Bellingham' })
    expect(nombresEnTabla()).toEqual(['Nicolò Barella', 'Jude Bellingham'])
    expect(within(within(screen.getByRole('table')).getAllByRole('row')[1]).getByText('2')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Mediocampista' }))
    await user.click(screen.getByRole('button', { name: 'Defensor' }))
    expect(await screen.findByText('Sin jugadores')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Todos' }))
    await screen.findByRole('table')
    await user.selectOptions(screen.getByLabelText('Liga'), 'Serie A')
    await screen.findByRole('table')

    expect(consultas).toEqual(['|', 'MED|', 'DEF|', '|', '|Serie A'])
  })

  it('pagina el ranking', async () => {
    const paginas = []
    server.use(http.get('/api/jugadores/ranking', ({ request }) => {
      const page = Number(new URL(request.url).searchParams.get('page'))
      paginas.push(page)
      return HttpResponse.json(pagina(page === 0 ? items.slice(0, 2) : items.slice(2), { page, totalPages: 2 }))
    }))
    const user = userEvent.setup()
    renderRanking()
    await screen.findByRole('table')

    await user.click(screen.getByRole('button', { name: 'Siguiente' }))
    expect(await screen.findByRole('link', { name: 'Jude Bellingham' })).toBeInTheDocument()
    expect(screen.getByText('Página 2 de 2')).toBeInTheDocument()
    expect(paginas).toEqual([0, 1])
  })

  it('navega al detalle del jugador desde el ranking', async () => {
    server.use(http.get('/api/jugadores/ranking', () => HttpResponse.json(pagina(items))))
    const user = userEvent.setup()
    renderRanking()

    await user.click(await screen.findByRole('link', { name: 'Nicolò Barella' }))
    expect(await screen.findByRole('heading', { name: 'Detalle del jugador' })).toBeInTheDocument()
    expect(window.location.pathname).toBe('/jugadores/7')
    window.history.pushState({}, '', '/')
  })

  it('muestra el estado vacío cuando todavía no hubo recálculo', async () => {
    renderRanking()
    expect(await screen.findByText('Ranking vacío')).toBeInTheDocument()
    expect(screen.getByText('Todavía no hubo un recálculo de cotizaciones.')).toBeInTheDocument()
  })

  it('muestra el error del backend con mensaje y correlationId', async () => {
    server.use(http.get('/api/jugadores/ranking', () => HttpResponse.json(
      { mensaje: 'No se pudo calcular el ranking.', timestamp: '2026-10-24T12:00:00Z', correlationId: 'ranking-500' },
      { status: 500 },
    )))
    renderRanking()
    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo calcular el ranking.')
    expect(screen.getByText(/ranking-500/)).toBeInTheDocument()
  })

  it('no consulta la API sin sesión', () => {
    let consultas = 0
    server.use(http.get('/api/jugadores/ranking', () => { consultas += 1; return HttpResponse.json(pagina(items)) }))
    renderRanking({ sesion: false })
    expect(screen.queryByRole('heading', { name: 'Ranking de jugadores' })).not.toBeInTheDocument()
    expect(consultas).toBe(0)
  })

  it('se abre desde el menú principal', async () => {
    const user = userEvent.setup()
    renderRanking({ ruta: '/' })
    await user.click(screen.getByRole('button', { name: /Ranking/ }))
    expect(await screen.findByRole('heading', { name: 'Ranking de jugadores' })).toBeInTheDocument()
    expect(screen.getByText('Ranking', { selector: '.breadcrumb' })).toBeInTheDocument()
  })
})
