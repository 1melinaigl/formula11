import { useCallback } from 'react'
import { endpoints } from '../api/endpoints'
import { TAMANO_PAGINA_SENALES, construirConsultaSenales, normalizarSenales } from '../components/senales/senales'
import { useConsultaPaginada } from './useConsultaPaginada'

const construirUrl = (consulta) =>
  `${endpoints.senales}?${construirConsultaSenales({ ...consulta, size: TAMANO_PAGINA_SENALES })}`

// Lista de /senales: vigentes (último recálculo) o historial completo, paginada de a 20.
export function useSenales() {
  const { consulta, respuesta, cargando, error, actualizarConsulta } = useConsultaPaginada({
    consultaInicial: { vigentes: true, page: 0 },
    construirUrl,
    normalizar: normalizarSenales,
  })

  const cambiarPagina = useCallback((page) => {
    if (page === consulta.page) return
    actualizarConsulta((actual) => ({ ...actual, page }))
  }, [actualizarConsulta, consulta.page])

  const cambiarVista = useCallback((vigentes) => {
    if (vigentes === consulta.vigentes) return
    actualizarConsulta({ vigentes, page: 0 })
  }, [actualizarConsulta, consulta.vigentes])

  return {
    ...respuesta,
    vigentes: consulta.vigentes,
    cargando,
    error,
    cambiarPagina,
    cambiarVista,
  }
}
