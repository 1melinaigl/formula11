import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import App from '../../src/App'
import { SesionProvider } from '../../src/context/SesionContext'
import { server } from '../../src/test/server'

// OperacionResponse del contrato; las fechas de operaciones son date-time.
const compra = {
  id: 1, tipo: 'COMPRA', jugador: { id: 5, nombre: 'Harry Kane', equipo: 'FC Bayern München', liga: 'Bundesliga', posicion: 'DEL' },
  cantidad: 10, precioUnitario: 1, total: 10, saldoPosterior: 90, contraparte: 'superusuario', fecha: '2026-08-17T15:30:00', estrategia: null, version: null,
}
const venta = {
  id: 2, tipo: 'VENTA', jugador: { id: 7, nombre: 'Nicolò Barella', equipo: 'FC Internazionale Milano', liga: 'Serie A', posicion: 'MED' },
  cantidad: 2, precioUnitario: 1.25, total: 2.5, saldoPosterior: 92.5, contraparte: 'superusuario', fecha: '2026-10-19T09:05:00', estrategia: 'RENDIMIENTO_POSICION', version: 2,
}

function renderHistorial(ruta = '/operaciones', sesion = true) {
  window.history.pushState({}, '', ruta)
  if (sesion) {
    window.sessionStorage.setItem('formula11.session', JSON.stringify({ token: 'historial-token', usuario: { id: 11, nombre: 'Ana Formula', rol: 'USUARIO' } }))
  }
  return render(<SesionProvider><App /></SesionProvider>)
}

function pagina(content, page = 0, totalPages = 1) {
  return { content, page, size: 10, totalElements: content.length, totalPages }
}

describe('historial de operaciones', () => {
  beforeEach(() => window.sessionStorage.clear())

  it('lista las operaciones con fecha, tipo, jugador, tokens, precio y efecto en el saldo', async () => {
    server.use(http.get('/api/usuarios/:id/transacciones', ({ params, request }) => {
      const url = new URL(request.url)
      expect(params.id).toBe('11')
      expect(request.headers.get('Authorization')).toBe('Bearer historial-token')
      expect(url.searchParams.get('page')).toBe('0')
      expect(url.searchParams.get('size')).toBe('10')
      expect(url.searchParams.has('sort')).toBe(false)
      expect(url.searchParams.has('tipo')).toBe(false)
      return HttpResponse.json(pagina([venta, compra]))
    }))
    renderHistorial()

    const tabla = await screen.findByRole('table', { name: 'Historial de operaciones' })
    const [, filaVenta, filaCompra] = within(tabla).getAllByRole('row')
    expect(within(filaVenta).getByText('19/10/2026, 09:05')).toBeInTheDocument()
    expect(within(filaVenta).getByText('Venta')).toBeInTheDocument()
    expect(within(filaVenta).getByRole('link', { name: 'Nicolò Barella' })).toHaveAttribute('href', '/jugadores/7')
    expect(within(filaVenta).getByText('+2,50')).toHaveClass('up')
    expect(within(filaCompra).getByText('Compra')).toBeInTheDocument()
    expect(within(filaCompra).getByText('-10,00')).toHaveClass('down')
    expect(within(filaCompra).getByText('superusuario')).toBeInTheDocument()
    expect(within(filaCompra).getByText('90,00')).toBeInTheDocument()
    expect(within(filaVenta).getByText('1,25')).toBeInTheDocument()
    expect(within(filaVenta).getByText('92,50')).toBeInTheDocument()
    expect(screen.queryByRole('navigation', { name: 'Paginación del historial' })).not.toBeInTheDocument()
  })

  it('filtra por tipo en el backend y vuelve a la primera página', async () => {
    const consultas = []
    server.use(http.get('/api/usuarios/:id/transacciones', ({ request }) => {
      const params = new URL(request.url).searchParams
      consultas.push({ page: params.get('page'), tipo: params.get('tipo') })
      if (params.get('tipo') === 'VENTA') return HttpResponse.json(pagina([venta]))
      if (params.get('tipo') === 'COMPRA') return HttpResponse.json(pagina([]))
      return HttpResponse.json(pagina([venta, compra], Number(params.get('page')), 2))
    }))
    const user = userEvent.setup()
    renderHistorial()
    await screen.findByRole('table')

    await user.click(screen.getByRole('button', { name: 'Siguiente' }))
    await screen.findByText('Página 2 de 2')
    await user.click(screen.getByRole('button', { name: 'Ventas' }))
    expect(await screen.findByRole('link', { name: 'Nicolò Barella' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Ventas' })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.queryByText('Harry Kane')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Ventas' }))
    await user.click(screen.getByRole('button', { name: 'Compras' }))
    expect(await screen.findByText(/Todavía no hiciste compras/)).toBeInTheDocument()

    expect(consultas).toEqual([
      { page: '0', tipo: null },
      { page: '1', tipo: null },
      { page: '0', tipo: 'VENTA' },
      { page: '0', tipo: 'COMPRA' },
    ])
  })

  it('muestra el estado vacío', async () => {
    renderHistorial()
    expect(await screen.findByText('Sin operaciones')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'ranking' })).toHaveAttribute('href', '/ranking')
  })

  it('muestra el error del backend', async () => {
    server.use(http.get('/api/usuarios/:id/transacciones', () => HttpResponse.json({ mensaje: 'Historial no disponible.', correlationId: 'hist-500' }, { status: 500 })))
    renderHistorial()
    expect(await screen.findByRole('alert')).toHaveTextContent('Historial no disponible.')
    expect(screen.getByText(/hist-500/)).toBeInTheDocument()
  })

  it('no consulta la API sin sesión', () => {
    let consultas = 0
    server.use(http.get('/api/usuarios/:id/transacciones', () => { consultas += 1; return HttpResponse.json([]) }))
    renderHistorial('/operaciones', false)
    expect(screen.queryByRole('heading', { name: 'Historial de operaciones' })).not.toBeInTheDocument()
    expect(consultas).toBe(0)
  })

  it('se abre desde el portfolio y vuelve a él', async () => {
    const user = userEvent.setup()
    renderHistorial('/portfolio')
    await user.click(await screen.findByRole('link', { name: 'Historial' }))
    expect(await screen.findByRole('heading', { name: 'Historial de operaciones' })).toBeInTheDocument()
    expect(screen.getByText('Historial de operaciones', { selector: '.breadcrumb' })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Mi portfolio' }))
    expect(await screen.findByRole('heading', { name: 'Mi portfolio' })).toBeInTheDocument()
    window.history.pushState({}, '', '/')
  })
})

function renderComo(usuario, ruta = '/operaciones') {
  window.history.pushState({}, '', ruta)
  window.sessionStorage.setItem('formula11.session', JSON.stringify({ token: 'historial-token', usuario }))
  return render(<SesionProvider><App /></SesionProvider>)
}

describe('historial: precio inicial, roles, paginación y estados (007)', () => {
  beforeEach(() => window.sessionStorage.clear())

  it('marca las operaciones hechas al precio inicial', async () => {
    server.use(http.get('/api/usuarios/:id/transacciones', () => HttpResponse.json(pagina([venta, compra]))))
    renderHistorial()
    const [, filaVenta, filaCompra] = within(await screen.findByRole('table', { name: 'Historial de operaciones' })).getAllByRole('row')
    expect(within(filaCompra).getByText('(precio inicial)')).toBeInTheDocument()
    expect(within(filaVenta).queryByText('(precio inicial)')).not.toBeInTheDocument()
  })

  it('al superusuario le muestra un aviso y no consulta la API', async () => {
    let consultas = 0
    server.use(http.get('/api/usuarios/:id/transacciones', () => { consultas += 1; return HttpResponse.json(pagina([compra])) }))
    renderComo({ id: 1, nombre: 'Admin', rol: 'SUPERUSUARIO' })
    expect(await screen.findByText('El superusuario es la contraparte del mercado y no tiene operaciones propias.')).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
    expect(consultas).toBe(0)
  })

  it('vuelve una sola vez a la última página válida si la pedida quedó fuera de rango', async () => {
    const paginas = []
    server.use(http.get('/api/usuarios/:id/transacciones', ({ request }) => {
      const page = Number(new URL(request.url).searchParams.get('page'))
      paginas.push(page)
      // Hasta pedir la página 3 hay 21 operaciones; ahí el total ya bajó a 11 (2 páginas).
      const encogio = paginas.includes(2)
      if (page === 0) return HttpResponse.json({ content: [venta], page: 0, size: 10, totalElements: 21, totalPages: 3 })
      if (page === 1) return HttpResponse.json({ content: [compra], page: 1, size: 10, totalElements: encogio ? 11 : 21, totalPages: encogio ? 2 : 3 })
      return HttpResponse.json({ content: [], page, size: 10, totalElements: 11, totalPages: 2 })
    }))
    const user = userEvent.setup()
    renderHistorial()
    await screen.findByRole('table')
    await user.click(screen.getByRole('button', { name: 'Siguiente' }))
    await screen.findByText('Página 2 de 3')
    await user.click(screen.getByRole('button', { name: 'Siguiente' }))

    expect(await screen.findByRole('link', { name: 'Harry Kane' })).toBeInTheDocument()
    expect(screen.getByText('Página 2 de 2')).toBeInTheDocument()
    expect(paginas).toEqual([0, 1, 2, 1])
  })

  it('una respuesta lenta del filtro anterior no pisa la del filtro vigente', async () => {
    const pendientes = {}
    server.use(http.get('/api/usuarios/:id/transacciones', ({ request }) => {
      const tipo = new URL(request.url).searchParams.get('tipo') ?? 'TODAS'
      if (tipo === 'TODAS' && !pendientes.primera) {
        pendientes.primera = true
        return HttpResponse.json(pagina([venta, compra]))
      }
      return new Promise((resolve) => { pendientes[tipo] = resolve })
    }))
    const user = userEvent.setup()
    renderHistorial()
    await screen.findByRole('table')

    await user.click(screen.getByRole('button', { name: 'Compras' }))
    await user.click(screen.getByRole('button', { name: 'Ventas' }))
    pendientes.VENTA(HttpResponse.json(pagina([venta])))
    expect(await screen.findByRole('link', { name: 'Nicolò Barella' })).toBeInTheDocument()
    pendientes.COMPRA(HttpResponse.json(pagina([compra])))
    await new Promise((resolve) => setTimeout(resolve, 50))
    expect(screen.queryByRole('link', { name: 'Harry Kane' })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Nicolò Barella' })).toBeInTheDocument()
  })

  it('pasar de página conserva el filtro activo', async () => {
    const consultas = []
    server.use(http.get('/api/usuarios/:id/transacciones', ({ request }) => {
      const params = new URL(request.url).searchParams
      consultas.push(`${params.get('page')}|${params.get('tipo') ?? ''}`)
      return HttpResponse.json(pagina([venta], Number(params.get('page')), 2))
    }))
    const user = userEvent.setup()
    renderHistorial()
    await screen.findByRole('table')
    await user.click(screen.getByRole('button', { name: 'Ventas' }))
    await screen.findByRole('table')
    await user.click(screen.getByRole('button', { name: 'Siguiente' }))
    await screen.findByText('Página 2 de 2')
    expect(consultas).toEqual(['0|', '0|VENTA', '1|VENTA'])
  })

  it('muestra el estado de carga', async () => {
    let responder
    server.use(http.get('/api/usuarios/:id/transacciones', () => new Promise((resolve) => { responder = resolve })))
    renderHistorial()
    expect(await screen.findByText('Cargando operaciones...')).toBeInTheDocument()
    responder(HttpResponse.json(pagina([compra])))
    expect(await screen.findByRole('table')).toBeInTheDocument()
  })

  it('un 401 limpia la sesión y muestra el acceso denegado', async () => {
    server.use(http.get('/api/usuarios/:id/transacciones', () => HttpResponse.json(
      { mensaje: 'Sesión vencida.', timestamp: '2026-10-24T12:00:00Z', correlationId: 'hist-401' },
      { status: 401 },
    )))
    renderHistorial()
    expect(await screen.findByRole('heading', { name: 'Acceso denegado' })).toBeInTheDocument()
    expect(window.sessionStorage.getItem('formula11.session')).toBeNull()
  })

  it('llega desde "Ver historial" después de una compra y vuelve al portfolio con Escape', async () => {
    const user = userEvent.setup()
    renderHistorial('/jugadores/7')
    const panel = within(await screen.findByRole('region', { name: 'Operar' }))
    await user.click(panel.getByRole('button', { name: 'Comprar 1 token' }))
    await user.click(await panel.findByRole('link', { name: 'Ver historial' }))
    expect(await screen.findByRole('heading', { name: 'Historial de operaciones' })).toBeInTheDocument()

    await user.keyboard('{Escape}')
    expect(await screen.findByRole('heading', { name: 'Mi portfolio' })).toBeInTheDocument()
    window.history.pushState({}, '', '/')
  })

  it('no muestra el token de la sesión en la página', async () => {
    server.use(http.get('/api/usuarios/:id/transacciones', () => HttpResponse.json(pagina([compra]))))
    renderHistorial()
    await screen.findByRole('table')
    expect(document.body.textContent).not.toContain('historial-token')
  })
})
