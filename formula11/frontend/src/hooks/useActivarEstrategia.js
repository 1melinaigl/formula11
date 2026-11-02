import { useCallback, useState } from 'react'
import { endpoints } from '../api/endpoints'
import { useSesion } from './useSesion'

const SIN_ENVIO = { enviando: false, error: null }

// Elige la estrategia con la que opera todo el mercado. Solo el superusuario: al resto el backend le da 403.
export function useActivarEstrategia(alActivar) {
  const { solicitarProtegida } = useSesion()
  const [estado, setEstado] = useState(SIN_ENVIO)

  const activar = useCallback(async (codigo) => {
    setEstado({ enviando: true, error: null })
    try {
      await solicitarProtegida(endpoints.activarEstrategia(codigo), { method: 'POST' })
      setEstado(SIN_ENVIO)
      alActivar()
    } catch (error) {
      setEstado({ enviando: false, error })
    }
  }, [alActivar, solicitarProtegida])

  return { ...estado, activar }
}
