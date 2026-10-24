import { useEffect, useState } from 'react'
import { endpoints } from '../api/endpoints'
import { ESTRATEGIAS, normalizarCotizaciones } from '../components/cotizaciones/cotizaciones'
import { useSesion } from './useSesion'

// El público ve solo la estrategia activa; el superusuario puede pedir las dos y compararlas (si no, el backend da 403).
function pedirCotizaciones(solicitarProtegida, id, esSuperusuario) {
  if (!esSuperusuario) return solicitarProtegida(endpoints.cotizacionesJugador(id)).then(normalizarCotizaciones)
  return Promise.all(Object.keys(ESTRATEGIAS).map((codigo) => solicitarProtegida(endpoints.cotizacionesJugador(id, codigo))))
    .then((respuestas) => respuestas.flatMap(normalizarCotizaciones))
}

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
      pedirCotizaciones(solicitarProtegida, id, usuario.rol === 'SUPERUSUARIO'),
    ]).then(([jugador, cotizaciones]) => {
      if (!activo) return
      setEstado({
        jugador: jugador.status === 'fulfilled' ? jugador.value : null,
        cotizaciones: cotizaciones.status === 'fulfilled' ? cotizaciones.value : [],
        cargando: false,
        error: jugador.status === 'rejected' ? jugador.reason : null,
        errorCotizaciones: cotizaciones.status === 'rejected' ? cotizaciones.reason : null,
      })
    })

    return () => { activo = false }
  }, [id, solicitarProtegida, usuario])

  return { ...estado, cargando: Boolean(usuario && estado.cargando) }
}
