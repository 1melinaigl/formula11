import { useEffect, useState } from 'react'
import { endpoints } from '../api/endpoints'
import { normalizarCotizaciones } from '../components/cotizaciones/cotizaciones'
import { useSesion } from './useSesion'

// Se monta con key={id} desde la página, así que cada jugador arranca con el estado inicial.
export function useDetalleJugador(id) {
  const { usuario, solicitarProtegida } = useSesion()
  const [estado, setEstado] = useState({
    jugador: null,
    cotizaciones: [],
    cargando: Boolean(usuario),
    error: null,
    errorCotizaciones: null,
  })

  useEffect(() => {
    if (!usuario) return undefined

    let activo = true

    Promise.allSettled([
      solicitarProtegida(endpoints.jugador(id)),
      solicitarProtegida(endpoints.cotizacionesJugador(id)),
    ]).then(([jugador, cotizaciones]) => {
      if (!activo) return
      setEstado({
        jugador: jugador.status === 'fulfilled' ? jugador.value : null,
        cotizaciones: cotizaciones.status === 'fulfilled' ? normalizarCotizaciones(cotizaciones.value) : [],
        cargando: false,
        error: jugador.status === 'rejected' ? jugador.reason : null,
        errorCotizaciones: cotizaciones.status === 'rejected' ? cotizaciones.reason : null,
      })
    })

    return () => { activo = false }
  }, [id, solicitarProtegida, usuario])

  return { ...estado, cargando: Boolean(usuario && estado.cargando) }
}
