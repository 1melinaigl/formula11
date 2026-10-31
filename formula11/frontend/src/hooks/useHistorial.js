import { useCallback, useEffect, useState } from 'react'
import { endpoints } from '../api/endpoints'
import { construirConsultaHistorial, normalizarHistorial } from '../components/historial/historial'
import { useSesion } from './useSesion'

// Con habilitado=false (superusuario) no se consulta nada: no tiene operaciones propias.
export function useHistorial({ habilitado = true } = {}) {
  const { usuario, solicitarProtegida } = useSesion()
  const [consulta, setConsulta] = useState({ page: 0, tipo: '' })
  const [respuesta, setRespuesta] = useState({ operaciones: [], page: 0, totalPages: 0 })
  const [cargando, setCargando] = useState(Boolean(usuario && habilitado))
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!usuario || !habilitado) return undefined

    let activo = true

    solicitarProtegida(`${endpoints.transacciones(usuario.id)}?${construirConsultaHistorial(consulta)}`)
      .then((data) => {
        if (!activo) return
        const pagina = normalizarHistorial(data)
        // Página fuera de rango (por ejemplo, si cambió el total): se vuelve una sola vez a la última válida.
        // Como la nueva página es menor que totalPages, esta condición no se repite y no hay bucle.
        if (pagina.operaciones.length === 0 && Number(data?.totalElements) > 0 && pagina.totalPages > 0 && consulta.page >= pagina.totalPages) {
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
  }, [consulta, habilitado, solicitarProtegida, usuario])

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
    cargando: Boolean(usuario && habilitado && cargando),
    error,
    cambiarPagina,
    cambiarTipo,
  }
}
