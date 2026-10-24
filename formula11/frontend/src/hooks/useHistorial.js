import { useCallback, useEffect, useState } from 'react'
import { endpoints } from '../api/endpoints'
import { construirConsultaHistorial, normalizarHistorial } from '../components/historial/historial'
import { useSesion } from './useSesion'

export function useHistorial() {
  const { usuario, solicitarProtegida } = useSesion()
  const [consulta, setConsulta] = useState({ page: 0, tipo: '' })
  const [respuesta, setRespuesta] = useState({ operaciones: [], page: 0, totalPages: 0 })
  const [cargando, setCargando] = useState(Boolean(usuario))
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!usuario) return undefined

    let activo = true

    solicitarProtegida(`${endpoints.transacciones(usuario.id)}?${construirConsultaHistorial(consulta)}`)
      .then((data) => {
        if (activo) setRespuesta(normalizarHistorial(data))
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

  const cambiarTipo = useCallback((tipo) => {
    if (tipo === consulta.tipo) return
    setCargando(true)
    setError(null)
    setConsulta({ tipo, page: 0 })
  }, [consulta.tipo])

  return {
    ...respuesta,
    tipo: consulta.tipo,
    cargando: Boolean(usuario && cargando),
    error,
    cambiarPagina,
    cambiarTipo,
  }
}
