import { describe, expect, it } from 'vitest'
import { composicion, enriquecerTenencias, gananciasPorJugador, normalizarEvolucion, resumenPortfolio } from '../../src/components/portfolio/portfolio'

const jugador = (id, nombre, posicion = 'DEL') => ({ id, nombre, equipo: 'Equipo', liga: 'Serie A', posicion })

// PosicionPortfolio del contrato: valores ya calculados por el backend; gananciaPorcentaje en %.
const tenencias = enriquecerTenencias([
  { jugador: jugador(1, 'Kane'), cantidad: 10, precioPromedio: 1, precioActual: '1.5', invertido: 10, valorActual: 15, ganancia: 5, gananciaPorcentaje: 50 },
  { jugador: jugador(2, 'Barella', 'MED'), cantidad: 4, precioPromedio: 1.25, precioActual: 1, invertido: 5, valorActual: 4, ganancia: -1, gananciaPorcentaje: -20 },
])

describe('helpers del portfolio', () => {
  it('aplana el jugador y convierte el porcentaje de ganancia a fracción', () => {
    expect(tenencias[0]).toMatchObject({ jugadorId: 1, nombre: 'Kane', posicion: 'DEL', precioActual: 1.5, valorActual: 15, ganancia: 5, rendimiento: 0.5 })
    expect(tenencias[1]).toMatchObject({ jugadorId: 2, nombre: 'Barella', ganancia: -1, rendimiento: -0.2 })
  })

  it('toma los totales del PortfolioResponse y suma saldo y tenencias', () => {
    expect(resumenPortfolio({ saldo: 20, invertido: 15, valorTenencias: 19, ganancia: 4, gananciaPorcentaje: 26.67, fechaValuacion: '2026-10-19' })).toEqual({
      saldo: 20, invertido: 15, valorActual: 19, ganancia: 4, rendimiento: 0.2667, total: 39, fechaValuacion: '2026-10-19',
    })
    expect(resumenPortfolio({ saldo: 100, invertido: 0, valorTenencias: 0, ganancia: 0, gananciaPorcentaje: 0 }).rendimiento).toBeNull()
    expect(resumenPortfolio(null)).toMatchObject({ saldo: 0, total: 0, fechaValuacion: null })
  })

  it('arma la composición ordenada y agrupa a partir del octavo jugador', () => {
    expect(composicion(tenencias)).toEqual([
      { nombre: 'Kane', valor: 15, porcentaje: 15 / 19 },
      { nombre: 'Barella', valor: 4, porcentaje: 4 / 19 },
    ])
    const muchas = enriquecerTenencias(Array.from({ length: 9 }, (_, i) => ({
      jugador: jugador(i, `J${i}`), cantidad: 1, precioPromedio: 1, precioActual: 9 - i, invertido: 1, valorActual: 9 - i, ganancia: 8 - i, gananciaPorcentaje: 0,
    })))
    const filas = composicion(muchas)
    expect(filas).toHaveLength(8)
    expect(filas.at(-1)).toMatchObject({ nombre: 'Otros (2)', valor: 3 })
  })

  it('ordena las ganancias de mayor a menor', () => {
    expect(gananciasPorJugador(tenencias)).toEqual([{ nombre: 'Kane', ganancia: 5 }, { nombre: 'Barella', ganancia: -1 }])
  })

  it('normaliza los puntos de la evolución y los ordena por fecha', () => {
    expect(normalizarEvolucion({ usuarioId: 11, puntos: [{ fecha: '2026-10-19', valor: '12', invertido: 10 }, { fecha: '2026-10-12', valor: 10, invertido: 10 }, { valor: 3 }] })).toEqual([
      { fecha: '2026-10-12', valor: 10, invertido: 10 },
      { fecha: '2026-10-19', valor: 12, invertido: 10 },
    ])
    expect(normalizarEvolucion(null)).toEqual([])
  })
})
