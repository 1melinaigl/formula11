import { describe, expect, it } from 'vitest'
import { composicion, enriquecerTenencias, gananciasPorJugador, normalizarEvolucion, resumenPortfolio } from '../../src/components/portfolio/portfolio'

const tenencias = enriquecerTenencias([
  { jugadorId: 1, nombre: 'Kane', cantidad: 10, precioPromedio: 1, precioActual: 1.5 },
  { jugador: { id: 2, nombre: 'Barella', posicion: 'MED' }, cantidad: '4', precioPromedio: '1.25', precioActual: '1' },
  { jugadorId: 3, nombre: 'Sin precio', cantidad: 2, precioPromedio: 1 },
])

describe('helpers del portfolio', () => {
  it('calcula invertido, valor actual, ganancia y rendimiento por tenencia', () => {
    expect(tenencias[0]).toMatchObject({ invertido: 10, valorActual: 15, ganancia: 5, rendimiento: 0.5 })
    expect(tenencias[1]).toMatchObject({ jugadorId: 2, nombre: 'Barella', posicion: 'MED', invertido: 5, valorActual: 4, ganancia: -1, rendimiento: -0.2 })
    expect(tenencias[2]).toMatchObject({ precioActual: null, valorActual: null, ganancia: null })
  })

  it('respeta los valores calculados por el backend', () => {
    const [tenencia] = enriquecerTenencias([{ jugadorId: 9, cantidad: 2, precioPromedio: 1, precioActual: 2, valorActual: 3.9, ganancia: 1.9 }])
    expect(tenencia).toMatchObject({ valorActual: 3.9, ganancia: 1.9 })
  })

  it('resume el portfolio sumando saldo y valor de los tokens', () => {
    expect(resumenPortfolio(tenencias, 20)).toEqual({ saldo: 20, invertido: 17, valorActual: 19, ganancia: 2, rendimiento: 2 / 17, total: 39 })
    expect(resumenPortfolio([], 100).rendimiento).toBeNull()
  })

  it('arma la composición ordenada y agrupa a partir del octavo jugador', () => {
    expect(composicion(tenencias)).toEqual([
      { nombre: 'Kane', valor: 15, porcentaje: 15 / 19 },
      { nombre: 'Barella', valor: 4, porcentaje: 4 / 19 },
    ])
    const muchas = enriquecerTenencias(Array.from({ length: 9 }, (_, i) => ({ jugadorId: i, nombre: `J${i}`, cantidad: 1, precioActual: 9 - i })))
    const filas = composicion(muchas)
    expect(filas).toHaveLength(8)
    expect(filas.at(-1)).toMatchObject({ nombre: 'Otros (2)', valor: 3 })
  })

  it('ordena ganancias de mayor a menor y omite las desconocidas', () => {
    expect(gananciasPorJugador(tenencias)).toEqual([{ nombre: 'Kane', ganancia: 5 }, { nombre: 'Barella', ganancia: -1 }])
  })

  it('normaliza la evolución y la ordena por fecha', () => {
    expect(normalizarEvolucion({ content: [{ fecha: '2026-10-19', valor: '12', invertido: 10 }, { fecha: '2026-10-12', valor: 10 }, { valor: 3 }] })).toEqual([
      { fecha: '2026-10-12', valor: 10, invertido: 0 },
      { fecha: '2026-10-19', valor: 12, invertido: 10 },
    ])
  })
})
