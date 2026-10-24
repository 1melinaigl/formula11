import { useEffect, useState } from 'react'
import { endpoints } from '../api/endpoints'
import { normalizarRanking } from '../components/ranking/ranking'
import { useSesion } from './useSesion'

export function useRanking() {
  const { usuario, solicitarProtegida } = useSesion()
  const [estado, setEstado] = useState({ ranking: [], cargando: Boolean(usuario), error: null })

  useEffect(() => {
    if (!usuario) return undefined

    let activo = true

    solicitarProtegida(endpoints.ranking)
      .then((data) => {
        if (activo) setEstado({ ranking: normalizarRanking(data), cargando: false, error: null })
      })
      .catch((error) => {
        if (activo) setEstado({ ranking: [], cargando: false, error })
      })

    return () => { activo = false }
  }, [solicitarProtegida, usuario])

  return { ...estado, cargando: Boolean(usuario && estado.cargando) }
}
