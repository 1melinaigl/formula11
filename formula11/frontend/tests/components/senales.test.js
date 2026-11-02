import { beforeEach, describe, expect, it } from 'vitest'
import {
  MAXIMO_VISTAS,
  TIPOS_SENAL,
  agregarVistas,
  borrarVistas,
  construirConsultaSenales,
  formatearPorcentaje,
  leerVistas,
  normalizarAciertos,
  normalizarSenales,
  senalesNuevas,
} from '../../src/components/senales/senales'

const jugador = { id: 7, nombre: 'Nicolò Barella', equipo: 'FC Internazionale Milano', liga: 'Serie A', posicion: 'MED' }

describe('senales', () => {
  beforeEach(() => window.sessionStorage.clear())

  it('normaliza la página, descarta tipos desconocidos o señales incompletas y conserva el orden', () => {
    const pagina = normalizarSenales({
      content: [
        { id: 3, jugador, fecha: '2026-10-25', tipo: 'PRECIO_ATRASADO', brecha: '40.5', precio: '0.5', precioEstimado: '0.7025' },
        { id: 4, jugador, fecha: '2026-10-25', tipo: 'OTRO', brecha: 30, precio: 1, precioEstimado: 1.3 },
        { id: 5, fecha: '2026-10-25', tipo: 'ENTRE_ESTRATEGIAS', brecha: 20, precio: 1, precioEstimado: 1.2 },
        { jugador, fecha: '2026-10-25', tipo: 'ENTRE_ESTRATEGIAS', brecha: 20, precio: 1, precioEstimado: 1.2 },
        { id: 1, jugador, fecha: '2026-10-25', tipo: 'ENTRE_ESTRATEGIAS', brecha: 18, precio: 1, precioEstimado: 1.18 },
      ],
      page: 0,
      totalPages: 1,
      totalElements: 5,
    })

    expect(pagina.senales.map((senal) => senal.id)).toEqual([3, 1])
    expect(pagina.senales[0]).toMatchObject({ brecha: 40.5, precio: 0.5, precioEstimado: 0.7025, jugador })
    expect(pagina).toMatchObject({ page: 0, totalPages: 1, totalElements: 5 })
    expect(normalizarSenales(null)).toEqual({ senales: [], page: 0, totalPages: 0, totalElements: 0 })
  })

  it('expone los tipos con texto legible y ayuda', () => {
    expect(TIPOS_SENAL.ENTRE_ESTRATEGIAS).toEqual({ nombre: 'Entre estrategias', ayuda: 'Otra estrategia lo valora más' })
    expect(TIPOS_SENAL.PRECIO_ATRASADO).toEqual({ nombre: 'Precio atrasado', ayuda: 'Jugadores con rendimiento parecido valen más' })
  })

  it('formatea porcentajes con dos decimales, coma y signo', () => {
    expect(formatearPorcentaje(18)).toBe('+18,00%')
    expect(formatearPorcentaje(-1.43)).toBe('-1,43%')
    expect(formatearPorcentaje(0)).toBe('0,00%')
    expect(formatearPorcentaje(252.6)).toBe('+252,60%')
    expect(formatearPorcentaje(50, { signo: false })).toBe('50,00%')
  })

  it('normaliza los aciertos y devuelve ceros sin datos', () => {
    expect(normalizarAciertos({
      semanas: [{ fecha: '2026-08-23', senales: 23, variacionSenalados: '0.32', variacionMercado: '-0.1', aciertos: 17 }],
      tasaAcierto: '50.0',
      diferenciaPromedio: '-0.12',
    })).toEqual({
      semanas: [{ fecha: '2026-08-23', senales: 23, variacionSenalados: 0.32, variacionMercado: -0.1, aciertos: 17 }],
      tasaAcierto: 50,
      diferenciaPromedio: -0.12,
    })
    expect(normalizarAciertos(null)).toEqual({ semanas: [], tasaAcierto: 0, diferenciaPromedio: 0 })
  })

  it('arma la consulta de la lista sin sort', () => {
    const consulta = new URLSearchParams(construirConsultaSenales({ vigentes: false, page: 2, size: 20 }))
    expect(Object.fromEntries(consulta)).toEqual({ vigentes: 'false', page: '2', size: '20' })
  })

  it('devuelve las señales no vistas en el mismo orden', () => {
    const senales = [{ id: 9 }, { id: 2 }, { id: 5 }]
    expect(senalesNuevas(senales, [2]).map((senal) => senal.id)).toEqual([9, 5])
    expect(senalesNuevas(senales, [9, 2, 5])).toEqual([])
  })

  it('guarda las vistas por usuario en sessionStorage, sin repetir y con un máximo', () => {
    agregarVistas(11, [1, 2])
    agregarVistas(11, [2, 3])
    expect(leerVistas(11)).toEqual([1, 2, 3])
    expect(JSON.parse(window.sessionStorage.getItem('formula11.senales.vistas.11'))).toEqual([1, 2, 3])
    expect(leerVistas(12)).toEqual([])

    agregarVistas(11, Array.from({ length: MAXIMO_VISTAS }, (_, indice) => 100 + indice))
    const vistas = leerVistas(11)
    expect(MAXIMO_VISTAS).toBe(500)
    expect(vistas).toHaveLength(500)
    expect(vistas).not.toContain(1)
    expect(vistas.at(-1)).toBe(100 + MAXIMO_VISTAS - 1)

    borrarVistas(11)
    expect(window.sessionStorage.getItem('formula11.senales.vistas.11')).toBeNull()
  })

  it('toma un registro inválido como vacío', () => {
    window.sessionStorage.setItem('formula11.senales.vistas.11', '{no es json')
    expect(leerVistas(11)).toEqual([])
    window.sessionStorage.setItem('formula11.senales.vistas.11', '{"a":1}')
    expect(leerVistas(11)).toEqual([])
  })
})
