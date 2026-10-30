import { render, screen, waitFor, within } from '@testing-library/react'
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

  it('envía todos los códigos contractuales de posición', async () => {
    const posicionesConsultadas = []
    server.use(http.get('/api/jugadores/ranking', ({ request }) => {
      const posicion = new URL(request.url).searchParams.get('posicion')
      if (posicion) posicionesConsultadas.push(posicion)
      return HttpResponse.json(pagina(items))
    }))
    const user = userEvent.setup()
    renderRanking()
    await screen.findByRole('table')

    for (const nombre of ['Portero', 'Defensor', 'Mediocampista', 'Delantero']) {
      await user.click(screen.getByRole('button', { name: nombre }))
      await screen.findByRole('table')
    }

    expect(posicionesConsultadas).toEqual(['POR', 'DEF', 'MED', 'DEL'])
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
    expect(screen.getByText('Estrategia activa: Rendimiento por posición')).toBeInTheDocument()
  })

  it('muestra el estado de carga mientras la respuesta está pendiente', async () => {
    let resolver
    server.use(http.get('/api/jugadores/ranking', () => new Promise((resolve) => {
      resolver = resolve
    })))
    renderRanking()
    expect(await screen.findByText('Cargando ranking...')).toBeInTheDocument()
    resolver(HttpResponse.json(pagina(items)))
    expect(await screen.findByRole('table')).toBeInTheDocument()
  })

  it.each([
    ['400', 400, 'La consulta del ranking no es válida.'],
    ['403', 403, 'No tiene permisos para consultar el ranking.'],
    ['500', 500, 'No se pudo calcular el ranking.'],
  ])('muestra el error %s del backend sin ocultar la pantalla', async (_nombre, status, mensaje) => {
    server.use(http.get('/api/jugadores/ranking', () => HttpResponse.json(
      { mensaje, timestamp: '2026-10-24T12:00:00Z', correlationId: `ranking-${status}` },
      { status },
    )))
    renderRanking()
    expect(await screen.findByRole('heading', { name: 'Ranking de jugadores' })).toBeInTheDocument()
    expect(screen.getByRole('alert')).toHaveTextContent(mensaje)
    expect(screen.getByText(new RegExp(`ranking-${status}`))).toBeInTheDocument()
  })

  it('usa los fallbacks de clienteApi cuando falta mensaje o correlationId', async () => {
    let consultas = 0
    server.use(http.get('/api/jugadores/ranking', () => {
      consultas += 1
      if (consultas === 1) {
        return HttpResponse.json(
          { timestamp: '2026-10-24T12:00:00Z' },
          { status: 500 },
        )
      }
      return HttpResponse.json(
        { mensaje: 'La segunda consulta falló.', timestamp: '2026-10-24T12:00:00Z' },
        { status: 500 },
      )
    }))
    const user = userEvent.setup()
    renderRanking()

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo completar la solicitud.')
    expect(screen.getByText(/correlationId:/)).not.toHaveTextContent('No disponible')

    await user.click(screen.getByRole('button', { name: 'Mediocampista' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('La segunda consulta falló.')
    expect(screen.getByText(/correlationId:/)).not.toHaveTextContent('No disponible')
  })

  it('limpia la sesión ante un 401 y renderiza el acceso denegado', async () => {
    server.use(http.get('/api/jugadores/ranking', () => HttpResponse.json(
      { mensaje: 'Sesión vencida.', timestamp: '2026-10-24T12:00:00Z', correlationId: 'ranking-401' },
      { status: 401 },
    )))
    renderRanking()
    expect(await screen.findByText(/acceso denegado/i)).toBeInTheDocument()
    expect(window.sessionStorage.getItem('formula11.session')).toBeNull()
  })

  it('descarta la respuesta obsoleta al cambiar el filtro durante una consulta', async () => {
    const resolvers = []
    server.use(http.get('/api/jugadores/ranking', () => new Promise((resolve) => {
      resolvers.push(resolve)
    })))
    const user = userEvent.setup()
    renderRanking()
    await waitFor(() => expect(resolvers).toHaveLength(1))

    await user.click(screen.getByRole('button', { name: 'Mediocampista' }))
    await waitFor(() => expect(resolvers).toHaveLength(2))

    resolvers[1](HttpResponse.json(pagina([items[1], items[3]])))
    expect(await screen.findByRole('link', { name: 'Jude Bellingham' })).toBeInTheDocument()
    resolvers[0](HttpResponse.json(pagina(items)))
    await new Promise((resolve) => setTimeout(resolve, 0))

    expect(screen.queryByRole('link', { name: 'Harry Kane' })).not.toBeInTheDocument()
    expect(nombresEnTabla()).toEqual(['Nicolò Barella', 'Jude Bellingham'])
  })

  it('vuelve al menú con Escape', async () => {
    server.use(http.get('/api/jugadores/ranking', () => HttpResponse.json(pagina(items))))
    const user = userEvent.setup()
    renderRanking()
    await screen.findByRole('table')

    await user.keyboard('{Escape}')
    expect(window.location.pathname).toBe('/')
    expect(await screen.findByRole('navigation', { name: 'Menú principal' })).toBeInTheDocument()
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
