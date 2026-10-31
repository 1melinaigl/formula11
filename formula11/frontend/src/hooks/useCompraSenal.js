import { useCallback, useState } from 'react'
import { endpoints } from '../api/endpoints'
import { describirResultado } from '../components/mercado/mercado'
import { useSesion } from './useSesion'

const SIN_COMPRA = { enviandoId: null, resultado: null, error: null }

// Compra en un click: 1 token del jugador señalado, con la misma orden que el panel de la 005.
// Se muestra un solo resultado a la vez; el saldo lo valida el backend (422).
export function useCompraSenal() {
  const { solicitarProtegida } = useSesion()
  const [compra, setCompra] = useState(SIN_COMPRA)

  const comprar = useCallback(async (senal) => {
    setCompra({ enviandoId: senal.id, resultado: null, error: null })
    try {
      const orden = await solicitarProtegida(endpoints.comprar, { method: 'POST', body: { jugadorId: Number(senal.jugador.id), cantidad: 1 } })
      setCompra({ enviandoId: null, resultado: describirResultado(orden), error: null })
    } catch (error) {
      setCompra({ enviandoId: null, resultado: null, error })
    }
  }, [solicitarProtegida])

  return { ...compra, comprar }
}
