import { useCallback, useEffect, useState } from 'react'
import { endpoints } from '../api/endpoints'
import { construirConsultaRanking, normalizarRanking } from '../components/ranking/ranking'
import { useSesion } from './useSesion'

const RESPUESTA_VACIA = normalizarRanking(null)

export function useRanking() {
  const { usuario, solicitarProtegida } = useSesion()
  const [consulta, setConsulta] = useState({ page: 0, posicion: '', liga: '' })
  const [respuesta, setRespuesta] = useState(RESPUESTA_VACIA)
  const [cargando, setCargando] = useState(Boolean(usuario))
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!usuario) return undefined

    let activo = true

    solicitarProtegida(`${endpoints.ranking}?${construirConsultaRanking(consulta)}`)
      .then((data) => {
        if (activo) setRespuesta(normalizarRanking(data))
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
  const actualizar = useCallback((cambios) => {
    if (Object.entries(cambios).every(([clave, valor]) => consulta[clave] === valor)) return
    setCargando(true)
    setError(null)
    setConsulta((actual) => ({ ...actual, page: 0, ...cambios }))
  }, [consulta])

  return {
    ...respuesta,
    posicion: consulta.posicion,
    liga: consulta.liga,
    cargando: Boolean(usuario && cargando),
    error,
    cambiarPagina: useCallback((page) => actualizar({ page }), [actualizar]),
    cambiarPosicion: useCallback((posicion) => actualizar({ posicion }), [actualizar]),
    cambiarLiga: useCallback((liga) => actualizar({ liga }), [actualizar]),
  }
}
