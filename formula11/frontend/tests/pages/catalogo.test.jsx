import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import App from '../../src/App'
import { SesionProvider } from '../../src/context/SesionContext'
import { server } from '../../src/test/server'

const jugadores = [
  { id: 1, nombre: 'Enzo Fernandez', equipo: 'Manchester City FC', liga: 'Premier League', posicion: 'MED' },
  { id: 2, nombre: 'Emiliano Martinez', equipo: 'Aston Villa', liga: 'Premier League', posicion: 'POR' },
  { id: 3, nombre: 'Jude Bellingham', equipo: 'Real Madrid', liga: 'La Liga', posicion: 'MED' },
  { id: 4, nombre: 'Lionel Messi', equipo: 'Inter Miami', liga: 'MLS', posicion: 'DEL' },
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
      expect(new URL(request.url).searchParams.get('page')).toBe('0')
      expect(new URL(request.url).searchParams.get('size')).toBe('9')
      return HttpResponse.json({ content: jugadores, page: 0, size: 9, totalElements: jugadores.length, totalPages: 1 })
    }))

    const user = userEvent.setup()
    renderCatalogo()

    expect(await screen.findByText('Enzo Fernandez')).toBeInTheDocument()
    expect(solicitudes).toBe(1)
    expect(screen.getByText('Página 1 de 1')).toBeInTheDocument()
    expect(screen.queryByText(/\d+ jugadores/)).not.toBeInTheDocument()
    expect(screen.queryByRole('option', { name: 'ID' })).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Por página')).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /Lionel Messi/ }))
    expect(screen.getByText('Inter Miami', { selector: 'dd' })).toBeInTheDocument()
    expect(screen.getByText('Delantero', { selector: 'dd' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Volver a la lista' })).toBeInTheDocument()
  })

  it('envía filtros combinados al backend, reinicia la página y permite limpiar', async () => {
    const user = userEvent.setup()
    server.use(http.get('/api/jugadores', ({ request }) => {
      const params = new URL(request.url).searchParams
      expect(params.get('size')).toBe('9')
      if (params.get('liga') === 'La Liga' && params.get('posicion') === 'Mediocampista') {
        return HttpResponse.json({ content: [jugadores[2]], page: 0, size: 9, totalElements: 1, totalPages: 1 })
      }
      return HttpResponse.json({ content: jugadores, page: 0, size: 9, totalElements: jugadores.length, totalPages: 1 })
    }))
    renderCatalogo()
    await screen.findByText('Enzo Fernandez')

    await user.selectOptions(screen.getByLabelText('Liga'), 'La Liga')
    await user.selectOptions(screen.getByLabelText('Posición'), 'Mediocampista')
    expect(await screen.findByText('Jude Bellingham')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /La Liga/ })).toBeInTheDocument()
    expect(screen.queryByText(/\d+ jugadores/)).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Limpiar' }))
    expect(screen.getByText('Lionel Messi')).toBeInTheDocument()
    expect(screen.queryByText(/\d+ jugadores/)).not.toBeInTheDocument()
  })

  it('diferencia catálogo vacío y sin coincidencias filtradas', async () => {
    const user = userEvent.setup()
    server.use(http.get('/api/jugadores', ({ request }) => {
      const equipo = new URL(request.url).searchParams.get('equipo')
      expect(new URL(request.url).searchParams.get('size')).toBe('9')
      if (equipo) return HttpResponse.json({ content: [], page: 0, size: 9, totalElements: 0, totalPages: 0 })
      return HttpResponse.json({ content: [], page: 0, size: 9, totalElements: 0, totalPages: 0 })
    }))
    renderCatalogo()
    expect(await screen.findByText('Catálogo vacío')).toBeInTheDocument()
    await user.type(screen.getByLabelText('Equipo'), 'inexistente')
    expect(await screen.findByText('Sin coincidencias')).toBeInTheDocument()

    window.sessionStorage.clear()
    window.history.pushState({}, '', '/')
  })

  it('navega páginas y recupera una página fuera de rango una sola vez', async () => {
    const user = userEvent.setup()
    const paginas = [
      { content: jugadores.slice(0, 2), page: 0, size: 9, totalElements: 3, totalPages: 2 },
      { content: [jugadores[2]], page: 1, size: 9, totalElements: 3, totalPages: 2 },
    ]
    const solicitudes = []
    server.use(http.get('/api/jugadores', ({ request }) => {
      const page = Number(new URL(request.url).searchParams.get('page'))
      solicitudes.push(page)
      expect(new URL(request.url).searchParams.get('size')).toBe('9')
      if (page >= 2) return HttpResponse.json({ content: [], page, size: 9, totalElements: 3, totalPages: 2 })
      return HttpResponse.json(paginas[page])
    }))
    renderCatalogo()
    await screen.findByText('Enzo Fernandez')
    await user.click(screen.getByRole('button', { name: 'Siguiente' }))
    expect(await screen.findByText('Jude Bellingham')).toBeInTheDocument()
    expect(screen.getByText('Página 2 de 2')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Siguiente' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Anterior' })).toBeEnabled()

    await user.click(screen.getByRole('button', { name: 'Anterior' }))
    expect(await screen.findByText('Enzo Fernandez')).toBeInTheDocument()
    expect(screen.getByText('Página 1 de 2')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Anterior' })).toBeDisabled()
    expect(solicitudes).toEqual([0, 1, 0])
  })

  it('ordena en el backend con campo y dirección, sin opción ID, y vuelve a la primera página', async () => {
    const user = userEvent.setup()
    const consultas = []
    server.use(http.get('/api/jugadores', ({ request }) => {
      const params = new URL(request.url).searchParams
      consultas.push({ page: params.get('page'), sort: params.get('sort') })
      const page = Number(params.get('page'))
      return HttpResponse.json({ content: jugadores.slice(page * 2, page * 2 + 2), page, size: 9, totalElements: jugadores.length, totalPages: 2 })
    }))
    renderCatalogo()
    await screen.findByText('Enzo Fernandez')

    const campos = within(screen.getByLabelText('Ordenar por')).getAllByRole('option').map((opcion) => opcion.textContent)
    expect(campos).toEqual(['Nombre', 'Equipo', 'Liga', 'Posición'])

    await user.click(screen.getByRole('button', { name: 'Siguiente' }))
    await screen.findByText('Jude Bellingham')
    await user.selectOptions(screen.getByLabelText('Ordenar por'), 'Equipo')
    await user.selectOptions(screen.getByLabelText('Dirección'), 'Descendente')
    await screen.findByText('Página 1 de 2')

    expect(consultas.at(-2)).toEqual({ page: '0', sort: 'equipo,asc' })
    expect(consultas.at(-1)).toEqual({ page: '0', sort: 'equipo,desc' })
  })

  it('muestra el estado de carga y luego el error con mensaje y correlationId', async () => {
    let responder
    server.use(http.get('/api/jugadores', () => new Promise((resolve) => { responder = resolve })))
    renderCatalogo()

    expect(await screen.findByText('Cargando catálogo...')).toBeInTheDocument()
    expect(screen.queryByText('Enzo Fernandez')).not.toBeInTheDocument()

    responder(HttpResponse.json({ mensaje: 'Servicio no disponible.', correlationId: 'catalog-503' }, { status: 503 }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Servicio no disponible.')
    expect(screen.getByText(/catalog-503/)).toBeInTheDocument()
    expect(screen.queryByText(/catalog-token/)).not.toBeInTheDocument()
    expect(window.sessionStorage.getItem('formula11.session')).not.toBeNull()
  })

  it('limpia la sesión y muestra acceso denegado ante un 401', async () => {
    server.use(http.get('/api/jugadores', ({ request }) => {
      expect(new URL(request.url).searchParams.get('size')).toBe('9')
      return HttpResponse.json({ mensaje: 'Sesión vencida.', correlationId: 'catalog-401' }, { status: 401 })
    }))
    renderCatalogo()

    expect(await screen.findByText('Sesión vencida.')).toBeInTheDocument()
    expect(screen.getByText(/catalog-401/)).toBeInTheDocument()
    await waitFor(() => expect(window.sessionStorage.getItem('formula11.session')).toBeNull())
    expect(screen.getByRole('link', { name: 'Ingresar' })).toBeInTheDocument()
  })
})
