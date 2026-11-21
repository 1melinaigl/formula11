import { useCallback } from 'react'
import { endpoints } from '../api/endpoints'
import { construirConsultaRanking, normalizarRanking } from '../components/ranking/ranking'
import { useConsultaPaginada } from './useConsultaPaginada'

const construirUrl = (consulta) => `${endpoints.ranking}?${construirConsultaRanking(consulta)}`

export function useRanking() {
  const { consulta, respuesta, cargando, error, actualizarConsulta } = useConsultaPaginada({
    consultaInicial: { page: 0, posicion: '', liga: '' },
    construirUrl,
    normalizar: normalizarRanking,
  })

  const actualizar = useCallback((cambios) => {
    if (Object.entries(cambios).every(([clave, valor]) => consulta[clave] === valor)) return
    actualizarConsulta((actual) => ({ ...actual, page: 0, ...cambios }))
  }, [actualizarConsulta, consulta])

  // Vuelve a pedir el ranking aunque los filtros no cambien, por ejemplo al activar otra estrategia.
  const recargar = useCallback(() => {
    actualizarConsulta((actual) => ({ ...actual, page: 0 }))
  }, [actualizarConsulta])

  return {
    ...respuesta,
    posicion: consulta.posicion,
    liga: consulta.liga,
    cargando,
    error,
    cambiarPagina: useCallback((page) => actualizar({ page }), [actualizar]),
    cambiarPosicion: useCallback((posicion) => actualizar({ posicion }), [actualizar]),
    cambiarLiga: useCallback((liga) => actualizar({ liga }), [actualizar]),
    recargar,
  }
}
