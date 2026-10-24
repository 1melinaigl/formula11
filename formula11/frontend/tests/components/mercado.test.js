import { describe, expect, it } from 'vitest'
import { describirResultado, normalizarPortfolio, precioVigente, tenenciaDe, validarOperacion } from '../../src/components/mercado/mercado'

describe('helpers del mercado', () => {
  it('usa el precio del jugador si viene, y si no la última cotización de la estrategia activa', () => {
    const cotizaciones = [
      { fecha: '2026-10-12', precio: 1.1, estrategia: 'RENDIMIENTO' },
      { fecha: '2026-10-19', precio: 0.8, estrategia: 'CONSISTENCIA' },
    ]
    expect(precioVigente({ precioActual: 1.5 }, cotizaciones)).toBe(1.5)
    expect(precioVigente({ cotizacionActual: { precio: '1.3' } }, cotizaciones)).toBe(1.3)
    expect(precioVigente({}, cotizaciones)).toBe(1.1)
    expect(precioVigente({}, [cotizaciones[1]])).toBe(0.8)
    expect(precioVigente({}, [])).toBeNull()
  })

  it('normaliza el portfolio con jugador plano o anidado', () => {
    const portfolio = normalizarPortfolio({ saldo: '12.5', tenencias: [{ jugador: { id: 3 }, cantidad: '2', precioPromedio: '1.1' }] })
    expect(portfolio.saldo).toBe(12.5)
    expect(tenenciaDe(portfolio, '3')).toMatchObject({ jugadorId: 3, cantidad: 2, precioPromedio: 1.1 })
    expect(tenenciaDe(portfolio, 4)).toBeNull()
    expect(normalizarPortfolio(null)).toEqual({ saldo: 0, tenencias: [] })
  })

  it('valida cantidad, saldo, tenencia y disponibilidad', () => {
    const base = { precio: 2, saldo: 10, tenencia: 3 }
    expect(validarOperacion({ ...base, tipo: 'COMPRA', cantidad: '' })).toBe('Ingresá una cantidad de tokens.')
    expect(validarOperacion({ ...base, tipo: 'COMPRA', cantidad: '1.5' })).toMatch(/entero/)
    expect(validarOperacion({ ...base, tipo: 'COMPRA', cantidad: '5' })).toBeNull()
    expect(validarOperacion({ ...base, tipo: 'COMPRA', cantidad: '6' })).toMatch(/Saldo insuficiente/)
    expect(validarOperacion({ ...base, tipo: 'COMPRA', cantidad: '4', disponibles: 3 })).toBe('Solo quedan 3 tokens disponibles.')
    expect(validarOperacion({ ...base, tipo: 'VENTA', cantidad: '3' })).toBeNull()
    expect(validarOperacion({ ...base, tipo: 'VENTA', cantidad: '4' })).toBe('Solo tenés 3 tokens para vender.')
    expect(validarOperacion({ ...base, tenencia: 0, tipo: 'VENTA', cantidad: '1' })).toBe('No tenés tokens de este jugador para vender.')
  })

  it('describe el resultado y calcula el total si el backend no lo manda', () => {
    expect(describirResultado('VENTA', { cantidad: 2, precio: 1.25 })).toBe('Vendiste 2 tokens a 1,25 créditos. Total: 2,50 créditos.')
    expect(describirResultado('COMPRA', { cantidad: 1, precio: 1, total: 1 })).toBe('Compraste 1 token a 1,00 créditos. Total: 1,00 créditos.')
  })
})
