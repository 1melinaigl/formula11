import { describe, expect, it } from 'vitest'
import { describirResultado, normalizarMercado, normalizarPortfolio, tenenciaDe, validarOperacion } from '../../src/components/mercado/mercado'

describe('helpers del mercado', () => {
  it('normaliza MercadoJugadorResponse', () => {
    expect(normalizarMercado({ jugadorId: 7, tokensEmitidos: 100, tokensDisponibles: 64, precioVigente: '1.1840', fechaCotizacion: '2026-10-18' }))
      .toEqual({ precio: 1.184, disponibles: 64, emitidos: 100, fechaCotizacion: '2026-10-18' })
    expect(normalizarMercado(null)).toEqual({ precio: null, disponibles: null, emitidos: null, fechaCotizacion: null })
  })

  it('normaliza las posiciones del portfolio', () => {
    const portfolio = normalizarPortfolio({ saldo: '12.5', posiciones: [{ jugador: { id: 3, nombre: 'Kane' }, cantidad: '2', precioPromedio: '1.1' }] })
    expect(portfolio.saldo).toBe(12.5)
    expect(tenenciaDe(portfolio, '3')).toMatchObject({ jugadorId: 3, cantidad: 2, precioPromedio: 1.1 })
    expect(tenenciaDe(portfolio, 4)).toBeNull()
    expect(normalizarPortfolio(null)).toEqual({ saldo: 0, tenencias: [] })
  })

  it('valida cantidad, máximo por orden, saldo, tenencia y disponibilidad', () => {
    const base = { precio: 2, saldo: 10, tenencia: 3 }
    expect(validarOperacion({ ...base, tipo: 'COMPRA', cantidad: '' })).toBe('Ingresá una cantidad de tokens.')
    expect(validarOperacion({ ...base, tipo: 'COMPRA', cantidad: '1.5' })).toMatch(/entero/)
    expect(validarOperacion({ ...base, tipo: 'COMPRA', cantidad: '101' })).toBe('Podés operar hasta 100 tokens por orden.')
    expect(validarOperacion({ ...base, tipo: 'COMPRA', cantidad: '5' })).toBeNull()
    expect(validarOperacion({ ...base, tipo: 'COMPRA', cantidad: '6' })).toMatch(/Saldo insuficiente/)
    expect(validarOperacion({ ...base, tipo: 'COMPRA', cantidad: '4', disponibles: 3 })).toBe('Solo quedan 3 tokens disponibles.')
    expect(validarOperacion({ ...base, tipo: 'VENTA', cantidad: '3' })).toBeNull()
    expect(validarOperacion({ ...base, tipo: 'VENTA', cantidad: '4' })).toBe('Solo tenés 3 tokens para vender.')
    expect(validarOperacion({ ...base, tenencia: 0, tipo: 'VENTA', cantidad: '1' })).toBe('No tenés tokens de este jugador para vender.')
  })

  it('describe el OperacionResponse con precio unitario, total y saldo posterior', () => {
    expect(describirResultado({ tipo: 'VENTA', cantidad: 2, precioUnitario: 1.25, total: 2.5, saldoPosterior: 12.5 }))
      .toBe('Vendiste 2 tokens a 1,25 créditos. Total: 2,50 créditos. Saldo: 12,50 créditos.')
    expect(describirResultado({ tipo: 'COMPRA', cantidad: 1, precioUnitario: 1, total: 1, saldoPosterior: 99 }))
      .toBe('Compraste 1 token a 1,00 créditos. Total: 1,00 créditos. Saldo: 99,00 créditos.')
  })
})
