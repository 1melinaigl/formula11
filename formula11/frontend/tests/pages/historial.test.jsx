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
