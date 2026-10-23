import { render, screen, waitFor } from '@testing-library/react'
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
]

const paginar = (lista) => ({
  content: lista,
  page: 0,
  size: 100,
  totalElements: lista.length,
  totalPages: 1,
})

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
      expect(new URL(request.url).searchParams.get('size')).toBe('10')
      return HttpResponse.json({ content: jugadores, page: 0, size: 10, totalElements: 3, totalPages: 1 })
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
      expect(params.get('size')).toBe('10')
      if (params.get('liga') === 'La Liga' && params.get('posicion') === 'Mediocampista') {
        return HttpResponse.json({ content: [jugadores[2]], page: 0, size: 10, totalElements: 1, totalPages: 1 })
      }
      return HttpResponse.json({ content: jugadores, page: 0, size: 10, totalElements: 3, totalPages: 1 })
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
      expect(new URL(request.url).searchParams.get('size')).toBe('10')
      if (equipo) return HttpResponse.json({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 })
      return HttpResponse.json({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 })
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
      { content: jugadores.slice(0, 2), page: 0, size: 10, totalElements: 3, totalPages: 2 },
      { content: [jugadores[2]], page: 1, size: 10, totalElements: 3, totalPages: 2 },
    ]
    const solicitudes = []
    server.use(http.get('/api/jugadores', ({ request }) => {
      const page = Number(new URL(request.url).searchParams.get('page'))
      solicitudes.push(page)
      expect(new URL(request.url).searchParams.get('size')).toBe('10')
      if (page >= 2) return HttpResponse.json({ content: [], page, size: 10, totalElements: 3, totalPages: 2 })
      return HttpResponse.json(paginas[page])
    }))
    renderCatalogo()
    await screen.findByText('Lionel Messi')
    await user.click(screen.getByRole('button', { name: 'Siguiente' }))
    expect(await screen.findByText('Jude Bellingham')).toBeInTheDocument()
    expect(screen.getByText('Página 2 de 2')).toBeInTheDocument()
    expect(solicitudes).toEqual([0, 1])
  })

  it('limpia la sesión y muestra acceso denegado ante un 401', async () => {
    server.use(http.get('/api/jugadores', ({ request }) => {
      expect(new URL(request.url).searchParams.get('size')).toBe('10')
      return HttpResponse.json({ mensaje: 'Sesión vencida.', correlationId: 'catalog-401' }, { status: 401 })
    }))
    renderCatalogo()

    expect(await screen.findByText('Sesión vencida.')).toBeInTheDocument()
    expect(screen.getByText(/catalog-401/)).toBeInTheDocument()
    await waitFor(() => expect(window.sessionStorage.getItem('formula11.session')).toBeNull())
    expect(screen.getByRole('link', { name: 'Ingresar' })).toBeInTheDocument()
  })
})
