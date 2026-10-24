import { describe, expect, it } from 'vitest'
import { construirConsultaHistorial, efectoEnSaldo, formatearFechaHora, normalizarHistorial } from '../../src/components/historial/historial'

describe('helpers del historial', () => {
  it('normaliza la página de OperacionResponse y descarta tipos desconocidos', () => {
    const { operaciones, page, totalPages } = normalizarHistorial({
      content: [
        { id: 1, tipo: 'COMPRA', jugador: { id: 3, nombre: 'Kane', posicion: 'DEL' }, cantidad: 4, precioUnitario: '1.5', total: '6', saldoPosterior: '94', contraparte: 'superusuario', fecha: '2026-10-19T12:00:00Z' },
        { id: 2, tipo: 'VENTA', jugador: { id: 4, nombre: 'Barella' }, cantidad: 1, precioUnitario: 2, total: 2, saldoPosterior: 96 },
        { id: 3, tipo: 'AJUSTE', jugador: { id: 5 }, cantidad: 1 },
      ],
      page: 2,
      size: 10,
      totalElements: 23,
      totalPages: 3,
    })
    expect(operaciones).toHaveLength(2)
    expect(operaciones[0]).toMatchObject({ tipo: 'COMPRA', jugadorId: 3, nombre: 'Kane', posicion: 'DEL', cantidad: 4, precioUnitario: 1.5, total: 6, saldoPosterior: 94 })
    expect({ page, totalPages }).toEqual({ page: 2, totalPages: 3 })
    expect(normalizarHistorial(null)).toEqual({ operaciones: [], page: 0, totalPages: 0 })
  })

  it('una compra resta y una venta suma al saldo', () => {
    expect(efectoEnSaldo({ tipo: 'COMPRA', total: 6 })).toBe(-6)
    expect(efectoEnSaldo({ tipo: 'VENTA', total: 6 })).toBe(6)
  })

  it('formatea fechas de operaciones en 24 h y omite valores vacíos', () => {
    expect(formatearFechaHora('2026-10-19T15:30:00')).toMatch(/19\/10\/2026.*15:30/)
    expect(formatearFechaHora('2026-01-01')).toBe('01/01/2026')
    expect(formatearFechaHora(null)).toBe('—')
    expect(formatearFechaHora('no es fecha')).toBe('no es fecha')
  })

  it('arma la consulta con página, tamaño y filtros opcionales, sin orden', () => {
    expect(construirConsultaHistorial({ page: 1, tipo: '' })).toBe('page=1&size=10')
    expect(construirConsultaHistorial({ page: 0, tipo: 'VENTA', jugadorId: 7 })).toBe('page=0&size=10&tipo=VENTA&jugadorId=7')
  })
})
