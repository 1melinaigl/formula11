import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import App from '../../src/App'
import { SesionProvider } from '../../src/context/SesionContext'
import { server } from '../../src/test/server'

const jugadores = [
  { id: 1, nombre: 'Lionel Messi', equipo: 'Inter Miami', liga: 'MLS', posicion: 'DEL' },
  { id: 2, nombre: 'Emiliano Martinez', equipo: 'Aston Villa', liga: 'Premier League', posicion: 'POR' },
  { id: 3, nombre: 'Jude Bellingham', equipo: 'Real Madrid', liga: 'La Liga', posicion: 'MED' },
]

function renderCatalogo() {
  window.history.pushState({}, '', '/catalogo')
  window.sessionStorage.setItem('formula11.session', JSON.stringify({ token: 'catalog-token', usuario: { id: 11, nombre: 'Ana Formula' } }))
  return render(<SesionProvider><App /></SesionProvider>)
}

describe('catálogo de jugadores', () => {
  beforeEach(() => window.sessionStorage.clear())

  it('consulta una vez con Bearer y X-Correlation-Id y muestra lista y ficha', async () => {
    let solicitudes = 0
    server.use(http.get('/api/jugadores', ({ request }) => {
      solicitudes += 1
      expect(request.headers.get('Authorization')).toBe('Bearer catalog-token')
      expect(request.headers.get('X-Correlation-Id')).toBeTruthy()
      return HttpResponse.json(jugadores)
    }))

    const user = userEvent.setup()
    renderCatalogo()

    expect(await screen.findByText('Lionel Messi')).toBeInTheDocument()
    expect(solicitudes).toBe(1)
    await user.click(screen.getByRole('button', { name: /Lionel Messi/ }))
    expect(screen.getByText('Inter Miami', { selector: 'dd' })).toBeInTheDocument()
    expect(screen.getByText('Delantero')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Volver a la lista' })).toBeInTheDocument()
  })

  it('filtra por nombre y filtros combinados y permite limpiar', async () => {
    const user = userEvent.setup()
    server.use(http.get('/api/jugadores', () => HttpResponse.json(jugadores)))
    renderCatalogo()
    await screen.findByText('Lionel Messi')

    await user.selectOptions(screen.getByLabelText('Liga'), 'La Liga')
    await user.selectOptions(screen.getByLabelText('Posición'), 'MED')
    expect(screen.getByText('Jude Bellingham')).toBeInTheDocument()
    expect(screen.queryByText('Lionel Messi')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: /La Liga/ })).toBeInTheDocument()
    expect(screen.getByText('1 jugadores')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Limpiar' }))
    expect(screen.getByText('Lionel Messi')).toBeInTheDocument()
    expect(screen.getByText('3 jugadores')).toBeInTheDocument()
  })

  it('diferencia catálogo vacío y búsqueda sin coincidencias', async () => {
    const user = userEvent.setup()
    server.use(http.get('/api/jugadores', () => HttpResponse.json(jugadores)))
    renderCatalogo()
    await screen.findByText('Lionel Messi')
    await user.type(screen.getByLabelText('Buscar por nombre'), 'inexistente')
    expect(screen.getByText('Sin coincidencias')).toBeInTheDocument()

    window.sessionStorage.clear()
    window.history.pushState({}, '', '/')
  })

  it('limpia la sesión y muestra acceso denegado ante un 401', async () => {
    server.use(http.get('/api/jugadores', () => HttpResponse.json({ mensaje: 'Sesión vencida.', correlationId: 'catalog-401' }, { status: 401 })))
    renderCatalogo()

    expect(await screen.findByText('Sesión vencida.')).toBeInTheDocument()
    expect(screen.getByText(/catalog-401/)).toBeInTheDocument()
    await waitFor(() => expect(window.sessionStorage.getItem('formula11.session')).toBeNull())
    expect(screen.getByRole('link', { name: 'Ingresar' })).toBeInTheDocument()
  })
})
