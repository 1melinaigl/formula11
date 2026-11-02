import { useEffect, useState } from 'react'
import { endpoints } from '../api/endpoints'
import { normalizarAciertos } from '../components/senales/senales'
import { useSesion } from './useSesion'

// Panel de aciertos: una sola consulta al entrar a /senales, independiente de la lista.
export function useAciertos() {
  const { usuario, solicitarProtegida } = useSesion()
  const [estado, setEstado] = useState({ aciertos: normalizarAciertos(null), cargando: Boolean(usuario), error: null })

  useEffect(() => {
    if (!usuario) return undefined

    let activo = true

    solicitarProtegida(endpoints.aciertos)
      .then((data) => {
        if (activo) setEstado({ aciertos: normalizarAciertos(data), cargando: false, error: null })
      })
      .catch((error) => {
        if (activo) setEstado((actual) => ({ ...actual, cargando: false, error }))
      })

    return () => { activo = false }
  }, [solicitarProtegida, usuario])

  return { ...estado, cargando: Boolean(usuario && estado.cargando) }
}
