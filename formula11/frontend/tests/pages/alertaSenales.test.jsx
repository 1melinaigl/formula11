import { act, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../../src/App'
import { SesionProvider } from '../../src/context/SesionContext'
import { server } from '../../src/test/server'

const NOMBRES = ['Abde Rebbach', 'Ville Koski', 'Florian Thauvin', 'Pedri', 'Michael Olise', 'Andy Diouf']

// Senal del contrato: brecha en porcentaje, ordenadas por brecha descendente como las manda el backend.
function senal(id, indice = id - 1, tipo = indice % 2 ? 'PRECIO_ATRASADO' : 'ENTRE_ESTRATEGIAS') {
  return {
    id,
    jugador: { id: 100 + id, nombre: NOMBRES[indice], equipo: 'Equipo', liga: 'La Liga', posicion: 'DEL' },
    fecha: '2026-10-25',
    tipo,
    brecha: 250 - indice * 10,
    precio: 0.68,
    precioEstimado: 2.39,
  }
}

function pagina(content) {
  return { content, page: 0, size: 100, totalElements: content.length, totalPages: content.length ? 1 : 0 }
}

// Cada llamada devuelve la siguiente respuesta (la última se repite) y cuenta los pedidos.
function usarSenales(...respuestas) {
  const estado = { pedidos: 0, urls: [] }
  server.use(http.get('/api/senales', ({ request }) => {
    estado.urls.push(new URL(request.url))
    const respuesta = respuestas[Math.min(estado.pedidos, respuestas.length - 1)]
    estado.pedidos += 1
    return typeof respuesta === 'function' ? respuesta() : HttpResponse.json(respuesta)
  }))
  return estado
}

function renderApp(ruta = '/catalogo', sesion = true) {
  window.history.pushState({}, '', ruta)
  if (sesion) {
    window.sessionStorage.setItem('formula11.session', JSON.stringify({ token: 'senales-token', usuario: { id: 11, nombre: 'Ana Formula', rol: 'USUARIO' } }))
  }
  return render(<SesionProvider><App /></SesionProvider>)
}

const avanzar = (ms) => act(() => vi.advanceTimersByTimeAsync(ms))

describe('alerta de señales', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
    vi.useFakeTimers({ shouldAdvanceTime: true })
  })
  afterEach(() => vi.useRealTimers())

  it('muestra en cualquier pantalla una alerta agrupada con las 3 señales de mayor brecha', async () => {
    const estado = usarSenales(pagina([1, 2, 3, 4, 5].map((id) => senal(id))))
    renderApp('/catalogo')

    const alerta = await screen.findByRole('status', { name: 'Oportunidades nuevas' })
    expect(within(alerta).getByText('5 oportunidades nuevas')).toBeInTheDocument()
    expect(within(alerta).getByText('Abde Rebbach')).toBeInTheDocument()
    expect(within(alerta).getByText('Ville Koski')).toBeInTheDocument()
    expect(within(alerta).getByText('Florian Thauvin')).toBeInTheDocument()
    expect(within(alerta).queryByText('Pedri')).not.toBeInTheDocument()
    expect(within(alerta).getAllByText('Entre estrategias')).toHaveLength(2)
    expect(within(alerta).getAllByText('Precio atrasado')).toHaveLength(1)
    expect(within(alerta).getByText('+250,00%')).toBeInTheDocument()

    const url = estado.urls[0]
    expect(url.searchParams.get('vigentes')).toBe('true')
    expect(url.searchParams.get('page')).toBe('0')
    expect(url.searchParams.get('size')).toBe('100')
    expect(url.searchParams.has('sort')).toBe(false)
  })

  it('al cerrarla no vuelve a aparecer con las mismas señales y avisa solo de las nuevas', async () => {
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    const estado = usarSenales(pagina([senal(1), senal(2)]), pagina([senal(1), senal(2)]), pagina([senal(6, 3), senal(1), senal(2)]))
    renderApp('/catalogo')

    const alerta = await screen.findByRole('status', { name: 'Oportunidades nuevas' })
    await user.click(within(alerta).getByRole('button', { name: 'Cerrar' }))
    expect(screen.queryByRole('status', { name: 'Oportunidades nuevas' })).not.toBeInTheDocument()

    await avanzar(30000)
    expect(estado.pedidos).toBe(2)
    expect(screen.queryByRole('status', { name: 'Oportunidades nuevas' })).not.toBeInTheDocument()

    await avanzar(30000)
    const nueva = await screen.findByRole('status', { name: 'Oportunidades nuevas' })
    expect(within(nueva).getByText('1 oportunidad nueva')).toBeInTheDocument()
    expect(within(nueva).getByText('Pedri')).toBeInTheDocument()
    expect(within(nueva).queryByText('Abde Rebbach')).not.toBeInTheDocument()
  })

  it('se cierra con Escape sin disparar el volver de la página', async () => {
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    usarSenales(pagina([senal(1)]))
    renderApp('/ranking')

    const alerta = await screen.findByRole('status', { name: 'Oportunidades nuevas' })
    within(alerta).getByRole('button', { name: 'Cerrar' }).focus()
    await user.keyboard('{Escape}')

    expect(screen.queryByRole('status', { name: 'Oportunidades nuevas' })).not.toBeInTheDocument()
    expect(window.location.pathname).toBe('/ranking')
    // El foco no se pierde en el body: pasa al contenido principal.
    expect(document.activeElement).toBe(screen.getByRole('main'))
    expect(JSON.parse(window.sessionStorage.getItem('formula11.senales.vistas.11'))).toEqual([1])
  })

  it('no mueve el foco al aparecer y "Ver señales" lleva a la lista marcándolas como vistas', async () => {
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    usarSenales(pagina([senal(1), senal(2)]))
    renderApp('/catalogo')

    const alerta = await screen.findByRole('status', { name: 'Oportunidades nuevas' })
    expect(alerta).not.toContainElement(document.activeElement)
    await user.click(within(alerta).getByRole('button', { name: 'Ver señales' }))

    expect(window.location.pathname).toBe('/senales')
    expect(JSON.parse(window.sessionStorage.getItem('formula11.senales.vistas.11'))).toEqual([1, 2])
  })

  it('ignora una consulta fallida y muestra la alerta en la siguiente', async () => {
    const estado = usarSenales(
      () => HttpResponse.json({ mensaje: 'Error interno', correlationId: 'corr-500' }, { status: 500 }),
      pagina([senal(1)]),
    )
    renderApp('/catalogo')

    await vi.waitFor(() => expect(estado.pedidos).toBe(1))
    expect(screen.queryByRole('status', { name: 'Oportunidades nuevas' })).not.toBeInTheDocument()
    expect(screen.queryByText(/corr-500/)).not.toBeInTheDocument()

    await avanzar(30000)
    expect(await screen.findByRole('status', { name: 'Oportunidades nuevas' })).toHaveTextContent('1 oportunidad nueva')
  })

  it('un 401 limpia la sesión y corta las consultas', async () => {
    const estado = usarSenales(() => HttpResponse.json({ mensaje: 'Token vencido', correlationId: 'corr-401' }, { status: 401 }))
    renderApp('/catalogo')

    await vi.waitFor(() => expect(window.sessionStorage.getItem('formula11.session')).toBeNull())
    await avanzar(60000)
    expect(estado.pedidos).toBe(1)
  })

  it('al cerrar la sesión corta las consultas y olvida las señales vistas', async () => {
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime })
    window.sessionStorage.setItem('formula11.senales.vistas.11', JSON.stringify([1]))
    const estado = usarSenales(pagina([senal(1)]))
    renderApp('/')

    await vi.waitFor(() => expect(estado.pedidos).toBe(1))
    expect(screen.queryByRole('status', { name: 'Oportunidades nuevas' })).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: /Cerrar sesión/ }))

    expect(window.sessionStorage.getItem('formula11.senales.vistas.11')).toBeNull()
    await avanzar(60000)
    expect(estado.pedidos).toBe(1)
  })

  it('sin sesión no consulta y con la lista vacía no hay alerta', async () => {
    const estado = usarSenales(pagina([]))
    const { unmount } = renderApp('/catalogo', false)
    await avanzar(30000)
    expect(estado.pedidos).toBe(0)
    unmount()

    renderApp('/catalogo')
    await vi.waitFor(() => expect(estado.pedidos).toBe(1))
    expect(screen.queryByRole('status', { name: 'Oportunidades nuevas' })).not.toBeInTheDocument()
  })

  it('saltea el ciclo si la consulta anterior todavía no respondió', async () => {
    let responder
    const estado = usarSenales(
      () => new Promise((resolve) => { responder = () => resolve(HttpResponse.json(pagina([senal(1)]))) }),
      pagina([senal(1)]),
    )
    renderApp('/catalogo')

    await vi.waitFor(() => expect(estado.pedidos).toBe(1))
    await avanzar(30000)
    expect(estado.pedidos).toBe(1)

    responder()
    expect(await screen.findByRole('status', { name: 'Oportunidades nuevas' })).toBeInTheDocument()
    await avanzar(30000)
    expect(estado.pedidos).toBe(2)
  })
})
