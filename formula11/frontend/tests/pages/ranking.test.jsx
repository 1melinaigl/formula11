import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import App from '../../src/App'
import { SesionProvider } from '../../src/context/SesionContext'
import { server } from '../../src/test/server'

const ranking = [
  { id: 5, nombre: 'Harry Kane', equipo: 'FC Bayern München', liga: 'Bundesliga', posicion: 'DEL', precio: 1.44, precioAnterior: 1.2, score: 0.91, estrategia: 'RENDIMIENTO', fecha: '2026-10-19' },
  { id: 7, nombre: 'Nicolò Barella', equipo: 'FC Internazionale Milano', liga: 'Serie A', posicion: 'MED', precio: 1.21, precioAnterior: 1.1, score: 0.75, estrategia: 'RENDIMIENTO', fecha: '2026-10-19' },
  { id: 2, nombre: 'Emiliano Martinez', equipo: 'Aston Villa', liga: 'Premier League', posicion: 'POR', precio: 0.9, precioAnterior: 1, score: 0.4, estrategia: 'RENDIMIENTO', fecha: '2026-10-19' },
  { id: 3, nombre: 'Jude Bellingham', equipo: 'Real Madrid', liga: 'La Liga', posicion: 'MED', precio: 1.05, score: null, estrategia: 'RENDIMIENTO', fecha: '2026-10-19' },
]

function renderRanking({ sesion = true, ruta = '/ranking' } = {}) {
  window.history.pushState({}, '', ruta)
  if (sesion) {
    window.sessionStorage.setItem('formula11.session', JSON.stringify({ token: 'ranking-token', usuario: { id: 11, nombre: 'Ana Formula' } }))
  }
  return render(<SesionProvider><App /></SesionProvider>)
}

function nombresEnTabla() {
  return within(screen.getByRole('table')).getAllByRole('link').map((link) => link.textContent)
}

describe('ranking de jugadores', () => {
  beforeEach(() => window.sessionStorage.clear())

  it('muestra el ranking en el orden del backend con precio, variación y estrategia activa', async () => {
    server.use(http.get('/api/jugadores/ranking', ({ request }) => {
      expect(request.headers.get('Authorization')).toBe('Bearer ranking-token')
      expect(request.headers.get('X-Correlation-Id')).toBeTruthy()
      return HttpResponse.json(ranking)
    }))
    renderRanking()

    expect(await screen.findByRole('table')).toBeInTheDocument()
    expect(nombresEnTabla()).toEqual(['Harry Kane', 'Nicolò Barella', 'Emiliano Martinez', 'Jude Bellingham'])
    expect(screen.getByText('Estrategia activa: Rendimiento por posición · al 19/10/2026')).toBeInTheDocument()

    const filas = within(screen.getByRole('table')).getAllByRole('row')
    expect(within(filas[1]).getByText('1')).toBeInTheDocument()
    expect(within(filas[1]).getByText('1,44')).toBeInTheDocument()
    expect(within(filas[1]).getByText('+20,0%')).toHaveClass('up')
    expect(within(filas[3]).getByText('-10,0%')).toHaveClass('down')
    expect(within(filas[4]).getAllByText('—')).toHaveLength(2)
  })

  it('acepta filas anidadas con el jugador y respuestas paginadas', async () => {
    server.use(http.get('/api/jugadores/ranking', () => HttpResponse.json({
      content: [{ jugador: { id: 9, nombre: 'Lautaro Martinez', equipo: 'FC Internazionale Milano', posicion: 'DEL' }, precio: 1.3, variacion: 0.05, estrategia: 'CONSISTENCIA' }],
    })))
    renderRanking()

    expect(await screen.findByRole('link', { name: 'Lautaro Martinez' })).toHaveAttribute('href', '/jugadores/9')
    expect(screen.getByText('+5,0%')).toBeInTheDocument()
    expect(screen.getByText('Estrategia activa: Consistencia')).toBeInTheDocument()
  })

  it('filtra por posición sin volver a consultar y renumera las posiciones', async () => {
    let consultas = 0
    server.use(http.get('/api/jugadores/ranking', () => { consultas += 1; return HttpResponse.json(ranking) }))
    const user = userEvent.setup()
    renderRanking()
    await screen.findByRole('table')

    await user.click(screen.getByRole('button', { name: 'Mediocampista' }))
    expect(screen.getByRole('button', { name: 'Mediocampista' })).toHaveAttribute('aria-pressed', 'true')
    expect(nombresEnTabla()).toEqual(['Nicolò Barella', 'Jude Bellingham'])
    expect(within(within(screen.getByRole('table')).getAllByRole('row')[1]).getByText('1')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Defensor' }))
    expect(screen.getByText('Sin jugadores')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Todos' }))
    expect(nombresEnTabla()).toHaveLength(4)
    expect(consultas).toBe(1)
  })

  it('navega al detalle del jugador desde el ranking', async () => {
    server.use(http.get('/api/jugadores/ranking', () => HttpResponse.json(ranking)))
    const user = userEvent.setup()
    renderRanking()

    await user.click(await screen.findByRole('link', { name: 'Nicolò Barella' }))
    expect(await screen.findByRole('heading', { name: 'Detalle del jugador' })).toBeInTheDocument()
    expect(window.location.pathname).toBe('/jugadores/7')
    window.history.pushState({}, '', '/')
  })

  it('muestra el estado vacío cuando todavía no hay cotizaciones', async () => {
    renderRanking()
    expect(await screen.findByText('Ranking vacío')).toBeInTheDocument()
  })

  it('muestra el error del backend con mensaje y correlationId', async () => {
    server.use(http.get('/api/jugadores/ranking', () => HttpResponse.json(
      { mensaje: 'No se pudo calcular el ranking.', correlationId: 'ranking-500' },
      { status: 500 },
    )))
    renderRanking()
    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo calcular el ranking.')
    expect(screen.getByText(/ranking-500/)).toBeInTheDocument()
  })

  it('no consulta la API sin sesión', () => {
    let consultas = 0
    server.use(http.get('/api/jugadores/ranking', () => { consultas += 1; return HttpResponse.json(ranking) }))
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
