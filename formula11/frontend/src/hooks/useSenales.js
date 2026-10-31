import { useCallback, useEffect, useState } from 'react'
import { endpoints } from '../api/endpoints'
import { TAMANO_PAGINA_SENALES, construirConsultaSenales, normalizarSenales } from '../components/senales/senales'
import { useSesion } from './useSesion'

// Lista de /senales: vigentes (último recálculo) o historial completo, paginada de a 20.
export function useSenales() {
  const { usuario, solicitarProtegida } = useSesion()
  const [consulta, setConsulta] = useState({ vigentes: true, page: 0 })
  const [respuesta, setRespuesta] = useState(normalizarSenales(null))
  const [cargando, setCargando] = useState(Boolean(usuario))
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!usuario) return undefined

    let activo = true

    solicitarProtegida(`${endpoints.senales}?${construirConsultaSenales({ ...consulta, size: TAMANO_PAGINA_SENALES })}`)
      .then((data) => {
        if (!activo) return
        const pagina = normalizarSenales(data)
        // Página fuera de rango: se vuelve una sola vez a la última válida (la nueva página es menor que totalPages).
        if (pagina.senales.length === 0 && pagina.totalElements > 0 && pagina.totalPages > 0 && consulta.page >= pagina.totalPages) {
          setConsulta((actual) => ({ ...actual, page: pagina.totalPages - 1 }))
          return
        }
        setRespuesta(pagina)
      })
      .catch((requestError) => {
        if (activo) setError(requestError)
      })
      .finally(() => {
        if (activo) setCargando(false)
      })

    return () => { activo = false }
  }, [consulta, solicitarProtegida, usuario])

  // Solo se marca la carga si la consulta cambia; si no, el efecto no corre y quedaría cargando.
  const cambiarPagina = useCallback((page) => {
    if (page === consulta.page) return
    setCargando(true)
    setError(null)
    setConsulta((actual) => ({ ...actual, page }))
  }, [consulta.page])

  const cambiarVista = useCallback((vigentes) => {
    if (vigentes === consulta.vigentes) return
    setCargando(true)
    setError(null)
    setConsulta({ vigentes, page: 0 })
  }, [consulta.vigentes])

  return {
    ...respuesta,
    vigentes: consulta.vigentes,
    cargando: Boolean(usuario && cargando),
    error,
    cambiarPagina,
    cambiarVista,
  }
}
