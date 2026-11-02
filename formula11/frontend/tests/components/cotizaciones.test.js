import { describe, expect, it } from 'vitest'
import {
  estrategiasPresentes,
  formatearFecha,
  normalizarCotizaciones,
  obtenerEstrategiaActiva,
  porcentajeAFraccion,
  resumenPorEstrategia,
  seriesPorFecha,
} from '../../src/components/cotizaciones/cotizaciones'

const rendimiento = normalizarCotizaciones({
  jugadorId: 7,
  estrategia: 'RENDIMIENTO_POSICION',
  cotizaciones: [
    { fecha: '2026-10-19', precio: '1.2000', variacion: 20, version: 1 },
    { fecha: '2026-10-12', precio: 1, variacion: null, version: 1 },
    { fecha: null, precio: 3 },
  ],
})
const consistencia = normalizarCotizaciones({
  estrategia: 'CONSISTENCIA',
  cotizaciones: [{ fecha: '2026-10-19', precio: 1.2, variacion: null, estrategia: 'CONSISTENCIA' }],
})

describe('helpers de cotizaciones', () => {
  it('normaliza el historial del contrato, descarta inválidas y ordena por fecha', () => {
    expect(rendimiento.map((c) => c.fecha)).toEqual(['2026-10-12', '2026-10-19'])
    expect(rendimiento[1]).toMatchObject({ precio: 1.2, variacion: 0.2, estrategia: 'RENDIMIENTO_POSICION', version: 1 })
    expect(normalizarCotizaciones(null)).toEqual([])
  })

  it('convierte porcentajes del contrato a fracción', () => {
    expect(porcentajeAFraccion(12.5)).toBe(0.125)
    expect(porcentajeAFraccion(null)).toBeNull()
  })

  it('ordena primero las estrategias conocidas y arma una fila por fecha', () => {
    const todas = [...consistencia, ...rendimiento]
    expect(estrategiasPresentes(todas)).toEqual(['RENDIMIENTO_POSICION', 'CONSISTENCIA'])
    expect(seriesPorFecha(todas)).toEqual([
      { fecha: '2026-10-12', RENDIMIENTO_POSICION: 1 },
      { fecha: '2026-10-19', CONSISTENCIA: 1.2, RENDIMIENTO_POSICION: 1.2 },
    ])
  })

  it('toma la variación que manda el backend para la última cotización de cada estrategia', () => {
    const [primera, segunda] = resumenPorEstrategia([...rendimiento, ...consistencia])
    expect(primera).toMatchObject({ estrategia: 'RENDIMIENTO_POSICION', variacion: 0.2 })
    expect(primera.actual.precio).toBe(1.2)
    expect(segunda.variacion).toBeNull()
  })

  it('formatea fechas ISO sin correrlas por zona horaria', () => {
    expect(formatearFecha('2026-01-01')).toBe('01/01/2026')
    expect(formatearFecha('2026-01-01T00:00:00Z', { corta: true })).toBe('01/01')
  })

  it('identifica una única estrategia activa', () => {
    expect(obtenerEstrategiaActiva([
      { codigo: 'RENDIMIENTO_POSICION', activa: true },
      { codigo: 'CONSISTENCIA', activa: false },
    ])).toBe('RENDIMIENTO_POSICION')
    expect(() => obtenerEstrategiaActiva([])).toThrow('No se pudo identificar')
    expect(() => obtenerEstrategiaActiva([
      { codigo: 'RENDIMIENTO_POSICION', activa: true },
      { codigo: 'CONSISTENCIA', activa: true },
    ])).toThrow('No se pudo identificar')
  })
})
