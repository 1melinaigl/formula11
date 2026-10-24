import { describe, expect, it } from 'vitest'
import {
  estrategiasPresentes,
  formatearFecha,
  normalizarCotizaciones,
  resumenPorEstrategia,
  seriesPorFecha,
} from '../../src/components/cotizaciones/cotizaciones'

const crudas = [
  { fecha: '2026-10-19', precio: '1.20', estrategia: 'CONSISTENCIA' },
  { fecha: '2026-10-12', precio: 1, estrategia: 'RENDIMIENTO' },
  { fecha: '2026-10-19', precio: 1.2, estrategia: 'RENDIMIENTO' },
  { fecha: null, precio: 3, estrategia: 'RENDIMIENTO' },
]

describe('helpers de cotizaciones', () => {
  it('normaliza listas y respuestas paginadas, descarta inválidas y ordena por fecha', () => {
    const lista = normalizarCotizaciones(crudas)
    expect(lista.map((c) => c.fecha)).toEqual(['2026-10-12', '2026-10-19', '2026-10-19'])
    expect(lista[1].precio).toBe(1.2)
    expect(normalizarCotizaciones({ content: crudas })).toHaveLength(3)
    expect(normalizarCotizaciones(null)).toEqual([])
  })

  it('ordena primero las estrategias conocidas y arma una fila por fecha', () => {
    const lista = normalizarCotizaciones(crudas)
    expect(estrategiasPresentes(lista)).toEqual(['RENDIMIENTO', 'CONSISTENCIA'])
    expect(seriesPorFecha(lista)).toEqual([
      { fecha: '2026-10-12', RENDIMIENTO: 1 },
      { fecha: '2026-10-19', CONSISTENCIA: 1.2, RENDIMIENTO: 1.2 },
    ])
  })

  it('calcula la variación contra la cotización anterior de la misma estrategia', () => {
    const [rendimiento, consistencia] = resumenPorEstrategia(normalizarCotizaciones(crudas))
    expect(rendimiento.actual.precio).toBe(1.2)
    expect(rendimiento.variacion).toBeCloseTo(0.2)
    expect(consistencia.variacion).toBeNull()
  })

  it('formatea fechas ISO sin correrlas por zona horaria', () => {
    expect(formatearFecha('2026-01-01')).toBe('01/01/2026')
    expect(formatearFecha('2026-01-01T00:00:00Z', { corta: true })).toBe('01/01')
  })
})
