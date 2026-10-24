import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import App from '../../src/App'
import { SesionProvider } from '../../src/context/SesionContext'
import { server } from '../../src/test/server'

const barella = { id: 7, nombre: 'Nicolò Barella', equipo: 'FC Internazionale Milano', liga: 'Serie A', posicion: 'MED' }

// Formas del contrato: PortfolioResponse, MercadoJugadorResponse y OperacionResponse.
function portfolio(saldo, posiciones = []) {
  return { usuarioId: 11, fechaValuacion: '2026-10-19', saldo, invertido: 0, valorTenencias: 0, ganancia: 0, gananciaPorcentaje: 0, posiciones }
}

function posicion(cantidad, precioPromedio = 1) {
  return { jugador: barella, cantidad, precioPromedio, precioActual: 1.2, invertido: cantidad * precioPromedio, valorActual: cantidad * 1.2, ganancia: 0, gananciaPorcentaje: 0 }
}

function mercado(tokensDisponibles = 64, precioVigente = 1.2, fechaCotizacion = '2026-10-19') {
  return { jugadorId: 7, tokensEmitidos: 100, tokensDisponibles, precioVigente, fechaCotizacion }
}

function operacion(tipo, cantidad, precioUnitario, saldoPosterior) {
  return {
    id: 1, tipo, jugador: barella, cantidad, precioUnitario, total: cantidad * precioUnitario, saldoPosterior,
    contraparte: 'superusuario', fecha: '2026-10-24T12:00:00Z', estrategia: 'RENDIMIENTO_POSICION', version: 2,
  }
}

function renderDetalle(ruta = '/jugadores/7') {
  window.history.pushState({}, '', ruta)
  window.sessionStorage.setItem('formula11.session', JSON.stringify({ token: 'mercado-token', usuario: { id: 11, nombre: 'Ana Formula', rol: 'USUARIO' } }))
  return render(<SesionProvider><App /></SesionProvider>)
}

function usarDatos({ portfolios, mercados = [mercado()] }) {
  const llamadas = { portfolio: 0, mercado: 0 }
  server.use(
    http.get('/api/usuarios/:id/portfolio', ({ params, request }) => {
      expect(params.id).toBe('11')
      expect(request.headers.get('Authorization')).toBe('Bearer mercado-token')
      return HttpResponse.json(portfolios[Math.min(llamadas.portfolio++, portfolios.length - 1)])
    }),
    http.get('/api/jugadores/:id/mercado', ({ params }) => {
      expect(params.id).toBe('7')
      return HttpResponse.json(mercados[Math.min(llamadas.mercado++, mercados.length - 1)])
    }),
  )
  return llamadas
}

async function panelOperar() {
  return within(await screen.findByRole('region', { name: 'Operar' }))
}

describe('mercado: compra y venta de tokens', () => {
  beforeEach(() => window.sessionStorage.clear())

  it('compra con el precio de /mercado y actualiza saldo, tenencia y disponibilidad', async () => {
    const llamadas = usarDatos({
      portfolios: [portfolio(10), portfolio(4, [posicion(5, 1.2)])],
      mercados: [mercado(64), mercado(59)],
    })
    let cuerpo
    server.use(http.post('/api/ordenes/compra', async ({ request }) => {
      expect(request.headers.get('Authorization')).toBe('Bearer mercado-token')
      expect(request.headers.get('X-Correlation-Id')).toBeTruthy()
      cuerpo = await request.json()
      return HttpResponse.json(operacion('COMPRA', 5, 1.2, 4), { status: 201 })
    }))
    const user = userEvent.setup()
    renderDetalle()
    const panel = await panelOperar()

    expect(panel.getByText('10,00 créditos')).toBeInTheDocument()
    expect(panel.getByText('64 de 100')).toBeInTheDocument()
    expect(panel.getByRole('button', { name: 'Comprar' })).toHaveAttribute('aria-pressed', 'true')
    expect(panel.getByText(/Precio vigente 1,20 \(al 19\/10\/2026\)/)).toBeInTheDocument()

    const cantidad = panel.getByLabelText('Cantidad de tokens')
    await user.clear(cantidad)
    await user.type(cantidad, '5')
    expect(panel.getByText('6,00 créditos')).toBeInTheDocument()
    await user.click(panel.getByRole('button', { name: 'Comprar 5 tokens' }))

    expect(await panel.findByRole('status')).toHaveTextContent('Compraste 5 tokens a 1,20 créditos. Total: 6,00 créditos. Saldo: 4,00 créditos.')
    expect(cuerpo).toEqual({ jugadorId: 7, cantidad: 5 })
    await waitFor(() => expect(panel.getByText('4,00 créditos')).toBeInTheDocument())
    expect(panel.getByText('5 · promedio 1,20')).toBeInTheDocument()
    expect(panel.getByText('59 de 100')).toBeInTheDocument()
    expect(llamadas).toEqual({ portfolio: 2, mercado: 2 })
    expect(cantidad).toHaveValue(1)
  })

  it('vende tokens que el usuario tiene', async () => {
    usarDatos({ portfolios: [portfolio(0, [posicion(4)])] })
    let cuerpo
    server.use(http.post('/api/ordenes/venta', async ({ request }) => {
      cuerpo = await request.json()
      return HttpResponse.json(operacion('VENTA', 3, 1.2, 3.6), { status: 201 })
    }))
    const user = userEvent.setup()
    renderDetalle()
    const panel = await panelOperar()

    await user.click(panel.getByRole('button', { name: 'Vender' }))
    const cantidad = panel.getByLabelText('Cantidad de tokens')
    await user.clear(cantidad)
    await user.type(cantidad, '3')
    await user.click(panel.getByRole('button', { name: 'Vender 3 tokens' }))

    expect(await panel.findByRole('status')).toHaveTextContent('Vendiste 3 tokens a 1,20 créditos. Total: 3,60 créditos. Saldo: 3,60 créditos.')
    expect(cuerpo).toEqual({ jugadorId: 7, cantidad: 3 })
  })

  it('valida en el cliente sin enviar órdenes inválidas', async () => {
    usarDatos({ portfolios: [portfolio(2, [posicion(1)])], mercados: [mercado(3)] })
    let ordenes = 0
    server.use(
      http.post('/api/ordenes/compra', () => { ordenes += 1; return HttpResponse.json({}) }),
      http.post('/api/ordenes/venta', () => { ordenes += 1; return HttpResponse.json({}) }),
    )
    const user = userEvent.setup()
    renderDetalle()
    const panel = await panelOperar()
    const cantidad = panel.getByLabelText('Cantidad de tokens')

    await user.clear(cantidad)
    await user.type(cantidad, '2')
    await user.click(panel.getByRole('button', { name: 'Comprar 2 tokens' }))
    expect(panel.getByText('Saldo insuficiente: necesitás 2,40 créditos.')).toBeInTheDocument()
    expect(cantidad).toHaveAttribute('aria-invalid', 'true')

    await user.clear(cantidad)
    await user.type(cantidad, '4')
    expect(panel.getByText('Solo quedan 3 tokens disponibles.')).toBeInTheDocument()

    await user.clear(cantidad)
    await user.type(cantidad, '101')
    expect(panel.getByText('Podés operar hasta 100 tokens por orden.')).toBeInTheDocument()

    await user.clear(cantidad)
    await user.type(cantidad, '0')
    expect(panel.getByText('La cantidad debe ser un número entero mayor a 0.')).toBeInTheDocument()

    await user.click(panel.getByRole('button', { name: 'Vender' }))
    await user.clear(cantidad)
    await user.type(cantidad, '3')
    await user.click(panel.getByRole('button', { name: 'Vender 3 tokens' }))
    expect(panel.getByText('Solo tenés 1 tokens para vender.')).toBeInTheDocument()
    expect(ordenes).toBe(0)
  })

  it.each([
    [409, 'El superusuario no tiene tokens suficientes de ese jugador.'],
    [422, 'El usuario no tiene saldo suficiente.'],
  ])('muestra el error %i del backend con mensaje y correlationId', async (status, mensaje) => {
    usarDatos({ portfolios: [portfolio(50)] })
    server.use(http.post('/api/ordenes/compra', () => HttpResponse.json(
      { mensaje, timestamp: '2026-10-24T12:00:00Z', correlationId: `orden-${status}` },
      { status },
    )))
    const user = userEvent.setup()
    renderDetalle()
    const panel = await panelOperar()

    await user.click(panel.getByRole('button', { name: 'Comprar 1 token' }))
    expect(await panel.findByRole('alert')).toHaveTextContent(mensaje)
    expect(panel.getByText(new RegExp(`orden-${status}`))).toBeInTheDocument()
    expect(panel.queryByRole('status')).not.toBeInTheDocument()
  })

  it('deshabilita el botón mientras la orden se envía', async () => {
    usarDatos({ portfolios: [portfolio(50)] })
    let responder
    server.use(http.post('/api/ordenes/compra', () => new Promise((resolve) => { responder = resolve })))
    const user = userEvent.setup()
    renderDetalle()
    const panel = await panelOperar()

    await user.click(panel.getByRole('button', { name: 'Comprar 1 token' }))
    expect(panel.getByRole('button', { name: 'Enviando...' })).toBeDisabled()
    responder(HttpResponse.json(operacion('COMPRA', 1, 1.2, 48.8), { status: 201 }))
    expect(await panel.findByRole('status')).toHaveTextContent('Compraste 1 token')
  })

  it('preselecciona la venta desde la URL', async () => {
    usarDatos({ portfolios: [portfolio(0, [posicion(2)])] })
    renderDetalle('/jugadores/7?operacion=venta')
    const panel = await panelOperar()
    expect(panel.getByRole('button', { name: 'Vender' })).toHaveAttribute('aria-pressed', 'true')
  })

  it('indica el precio inicial cuando todavía no hubo recálculo', async () => {
    usarDatos({ portfolios: [portfolio(50)], mercados: [mercado(100, 1, null)] })
    renderDetalle()
    const panel = await panelOperar()
    expect(panel.getByText(/Precio vigente 1,00 \(precio inicial\)/)).toBeInTheDocument()
  })

  it('no permite operar si no se pudo obtener el precio', async () => {
    usarDatos({ portfolios: [portfolio(50)] })
    server.use(http.get('/api/jugadores/:id/mercado', () => HttpResponse.json(
      { mensaje: 'Jugador no encontrado.', timestamp: '2026-10-24T12:00:00Z', correlationId: 'mercado-404' },
      { status: 404 },
    )))
    renderDetalle()
    const panel = await panelOperar()
    expect(panel.getByRole('alert')).toHaveTextContent('Jugador no encontrado.')
    expect(panel.getByText(/no se puede operar/)).toBeInTheDocument()
    expect(panel.queryByLabelText('Cantidad de tokens')).not.toBeInTheDocument()
  })
})
