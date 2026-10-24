import { describe, expect, it } from 'vitest'
import { construirConsultaHistorial, efectoEnSaldo, formatearFechaHora, normalizarHistorial } from '../../src/components/historial/historial'

describe('helpers del historial', () => {
  it('normaliza operaciones planas o anidadas y descarta tipos desconocidos', () => {
    const { operaciones, page, totalPages } = normalizarHistorial({
      content: [
        { tipo: 'compra', jugador: { id: 3, nombre: 'Kane' }, cantidad: '4', precio: '1.5' },
        { tipo: 'VENTA', jugadorId: 4, nombre: 'Barella', cantidad: 1, precio: 2, total: 1.9 },
        { tipo: 'AJUSTE', cantidad: 1 },
      ],
      page: 2,
      totalPages: 5,
    })
    expect(operaciones).toHaveLength(2)
    expect(operaciones[0]).toMatchObject({ tipo: 'COMPRA', jugadorId: 3, nombre: 'Kane', cantidad: 4, precio: 1.5, total: 6 })
    expect(operaciones[1].total).toBe(1.9)
    expect({ page, totalPages }).toEqual({ page: 2, totalPages: 5 })
    expect(normalizarHistorial([])).toEqual({ operaciones: [], page: 0, totalPages: 0 })
  })

  it('una compra resta y una venta suma al saldo', () => {
    expect(efectoEnSaldo({ tipo: 'COMPRA', total: 6 })).toBe(-6)
    expect(efectoEnSaldo({ tipo: 'VENTA', total: 6 })).toBe(6)
  })

  it('formatea fechas sin hora sin correrlas y omite valores vacíos', () => {
    expect(formatearFechaHora('2026-01-01')).toBe('01/01/2026')
    expect(formatearFechaHora(null)).toBe('—')
    expect(formatearFechaHora('no es fecha')).toBe('no es fecha')
    expect(formatearFechaHora('2026-10-19T15:30:00')).toMatch(/19\/10\/2026.*15:30/)
  })

  it('arma la consulta con página, tamaño, orden y tipo opcional', () => {
    expect(construirConsultaHistorial({ page: 1, tipo: '' })).toBe('page=1&size=10&sort=fecha%2Cdesc')
    expect(construirConsultaHistorial({ page: 0, tipo: 'VENTA' })).toBe('page=0&size=10&sort=fecha%2Cdesc&tipo=VENTA')
  })
})
