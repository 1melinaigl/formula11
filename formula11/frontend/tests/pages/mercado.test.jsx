import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import App from '../../src/App'
import { SesionProvider } from '../../src/context/SesionContext'
import { server } from '../../src/test/server'

const cotizaciones = [
  { fecha: '2026-10-12', precio: 1.1, estrategia: 'RENDIMIENTO' },
  { fecha: '2026-10-19', precio: 1.2, estrategia: 'RENDIMIENTO' },
  { fecha: '2026-10-19', precio: 0.9, estrategia: 'CONSISTENCIA' },
]

function renderDetalle(ruta = '/jugadores/7') {
  window.history.pushState({}, '', ruta)
  window.sessionStorage.setItem('formula11.session', JSON.stringify({ token: 'mercado-token', usuario: { id: 11, nombre: 'Ana Formula' } }))
  return render(<SesionProvider><App /></SesionProvider>)
}

function usarPortfolio(...respuestas) {
  let llamadas = 0
  server.use(http.get('/api/usuarios/:id/portfolio', ({ params, request }) => {
    expect(params.id).toBe('11')
    expect(request.headers.get('Authorization')).toBe('Bearer mercado-token')
    const respuesta = respuestas[Math.min(llamadas, respuestas.length - 1)]
    llamadas += 1
    return HttpResponse.json(respuesta)
  }))
  return () => llamadas
}

async function panelOperar() {
  return within(await screen.findByRole('region', { name: 'Operar' }))
}

describe('mercado: compra y venta de tokens', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
    server.use(http.get('/api/jugadores/:id/cotizaciones', () => HttpResponse.json(cotizaciones)))
  })

  it('compra tokens con la cotización vigente y actualiza saldo y tenencia', async () => {
    const llamadas = usarPortfolio(
      { saldo: 10, tenencias: [] },
      { saldo: 4, tenencias: [{ jugadorId: 7, cantidad: 5, precioPromedio: 1.2 }] },
    )
    let cuerpo
    server.use(http.post('/api/ordenes/compra', async ({ request }) => {
      expect(request.headers.get('Authorization')).toBe('Bearer mercado-token')
      expect(request.headers.get('X-Correlation-Id')).toBeTruthy()
      cuerpo = await request.json()
      return HttpResponse.json({ id: 1, tipo: 'COMPRA', cantidad: 5, precio: 1.2, total: 6 }, { status: 201 })
    }))
    const user = userEvent.setup()
    renderDetalle()
    const panel = await panelOperar()

    expect(panel.getByText('10,00 créditos')).toBeInTheDocument()
    expect(panel.getByRole('button', { name: 'Comprar' })).toHaveAttribute('aria-pressed', 'true')
    expect(panel.getByText(/Precio vigente 1,20/)).toBeInTheDocument()

    const cantidad = panel.getByLabelText('Cantidad de tokens')
    await user.clear(cantidad)
    await user.type(cantidad, '5')
    expect(panel.getByText('6,00 créditos')).toBeInTheDocument()
    await user.click(panel.getByRole('button', { name: 'Comprar 5 tokens' }))

    expect(await panel.findByRole('status')).toHaveTextContent('Compraste 5 tokens a 1,20 créditos. Total: 6,00 créditos.')
    expect(cuerpo).toEqual({ jugadorId: 7, cantidad: 5 })
    await waitFor(() => expect(panel.getByText('4,00 créditos')).toBeInTheDocument())
    expect(panel.getByText('5 · promedio 1,20')).toBeInTheDocument()
    expect(llamadas()).toBe(2)
    expect(cantidad).toHaveValue(1)
  })

  it('vende tokens que el usuario tiene', async () => {
    usarPortfolio({ saldo: 0, tenencias: [{ jugador: { id: 7 }, cantidad: 4, precioPromedio: 1 }] })
    let cuerpo
    server.use(http.post('/api/ordenes/venta', async ({ request }) => {
      cuerpo = await request.json()
      return HttpResponse.json({ cantidad: 3, precio: 1.2 })
    }))
    const user = userEvent.setup()
    renderDetalle()
    const panel = await panelOperar()

    await user.click(panel.getByRole('button', { name: 'Vender' }))
    const cantidad = panel.getByLabelText('Cantidad de tokens')
    await user.clear(cantidad)
    await user.type(cantidad, '3')
    await user.click(panel.getByRole('button', { name: 'Vender 3 tokens' }))

    expect(await panel.findByRole('status')).toHaveTextContent('Vendiste 3 tokens a 1,20 créditos. Total: 3,60 créditos.')
    expect(cuerpo).toEqual({ jugadorId: 7, cantidad: 3 })
  })

  it('valida en el cliente sin enviar órdenes inválidas', async () => {
    usarPortfolio({ saldo: 2, tenencias: [{ jugadorId: 7, cantidad: 1 }] })
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
    await user.type(cantidad, '0')
    expect(panel.getByText('La cantidad debe ser un número entero mayor a 0.')).toBeInTheDocument()

    await user.click(panel.getByRole('button', { name: 'Vender' }))
    await user.clear(cantidad)
    await user.type(cantidad, '3')
    await user.click(panel.getByRole('button', { name: 'Vender 3 tokens' }))
    expect(panel.getByText('Solo tenés 1 tokens para vender.')).toBeInTheDocument()
    expect(ordenes).toBe(0)
  })

  it('muestra el error del backend con mensaje y correlationId', async () => {
    usarPortfolio({ saldo: 50, tenencias: [] })
    server.use(http.post('/api/ordenes/compra', () => HttpResponse.json(
      { mensaje: 'No hay tokens disponibles para este jugador.', correlationId: 'orden-409' },
      { status: 409 },
    )))
    const user = userEvent.setup()
    renderDetalle()
    const panel = await panelOperar()

    await user.click(panel.getByRole('button', { name: 'Comprar 1 token' }))
    expect(await panel.findByRole('alert')).toHaveTextContent('No hay tokens disponibles para este jugador.')
    expect(panel.getByText(/orden-409/)).toBeInTheDocument()
    expect(panel.queryByRole('status')).not.toBeInTheDocument()
  })

  it('deshabilita el botón mientras la orden se envía', async () => {
    usarPortfolio({ saldo: 50, tenencias: [] })
    let responder
    server.use(http.post('/api/ordenes/compra', () => new Promise((resolve) => { responder = resolve })))
    const user = userEvent.setup()
    renderDetalle()
    const panel = await panelOperar()

    await user.click(panel.getByRole('button', { name: 'Comprar 1 token' }))
    expect(panel.getByRole('button', { name: 'Enviando...' })).toBeDisabled()
    responder(HttpResponse.json({ cantidad: 1, precio: 1.2 }))
    expect(await panel.findByRole('status')).toHaveTextContent('Compraste 1 token')
  })

  it('preselecciona la venta desde la URL', async () => {
    usarPortfolio({ saldo: 0, tenencias: [{ jugadorId: 7, cantidad: 2 }] })
    renderDetalle('/jugadores/7?operacion=venta')
    const panel = await panelOperar()
    expect(panel.getByRole('button', { name: 'Vender' })).toHaveAttribute('aria-pressed', 'true')
  })

  it('no permite operar sin cotización vigente', async () => {
    usarPortfolio({ saldo: 50, tenencias: [] })
    server.use(http.get('/api/jugadores/:id/cotizaciones', () => HttpResponse.json([])))
    renderDetalle()
    const panel = await panelOperar()
    expect(panel.getByText(/no tiene cotización vigente/)).toBeInTheDocument()
    expect(panel.queryByLabelText('Cantidad de tokens')).not.toBeInTheDocument()
  })
})
