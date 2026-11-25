import { useCallback } from 'react'
import { endpoints } from '../api/endpoints'
import { construirConsultaHistorial, normalizarHistorial } from '../components/historial/historial'
import { useConsultaPaginada } from './useConsultaPaginada'

const construirUrl = (consulta, usuario) =>
  `${endpoints.transacciones(usuario.id)}?${construirConsultaHistorial(consulta)}`

// Con habilitado=false (superusuario) no se consulta nada: no tiene operaciones propias.
export function useHistorial({ habilitado = true } = {}) {
  const { consulta, respuesta, cargando, error, actualizarConsulta } = useConsultaPaginada({
    consultaInicial: { page: 0, tipo: '' },
    construirUrl,
    normalizar: normalizarHistorial,
    habilitado,
  })

  const cambiarPagina = useCallback((page) => {
    if (page === consulta.page) return
    actualizarConsulta((actual) => ({ ...actual, page }))
  }, [actualizarConsulta, consulta.page])

  const cambiarTipo = useCallback((tipo) => {
    if (tipo === consulta.tipo) return
    actualizarConsulta({ tipo, page: 0 })
  }, [actualizarConsulta, consulta.tipo])

  return {
    ...respuesta,
    tipo: consulta.tipo,
    cargando,
    error,
    cambiarPagina,
    cambiarTipo,
  }
}
