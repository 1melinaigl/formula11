import { useEffect, useState } from 'react'
import { endpoints } from '../api/endpoints'
import { ESTRATEGIAS, normalizarCotizaciones, obtenerEstrategiaActiva } from '../components/cotizaciones/cotizaciones'
import { useSesion } from './useSesion'

function pedirCotizaciones(solicitarProtegida, id, esSuperusuario) {
  // El público ve solo la estrategia activa; el superusuario puede pedir las dos y compararlas (si no, el backend da 403).
  const codigos = esSuperusuario ? Object.keys(ESTRATEGIAS) : [undefined]
  return Promise.allSettled(codigos.map((codigo) => solicitarProtegida(endpoints.cotizacionesJugador(id, codigo))))
    .then((resultados) => ({
      cotizaciones: resultados
        .filter((resultado) => resultado.status === 'fulfilled')
        .flatMap((resultado) => normalizarCotizaciones(resultado.value)),
      error: resultados.find((resultado) => resultado.status === 'rejected')?.reason || null,
    }))
}

export function useDetalleJugador(id) {
  const { usuario, solicitarProtegida } = useSesion()
  const [estado, setEstado] = useState({
    jugador: null,
    cotizaciones: [],
    estrategiaActiva: null,
    puedeComparar: false,
    cargando: Boolean(usuario),
    error: null,
    errorCotizaciones: null,
  })

  useEffect(() => {
    if (!usuario) return undefined

    // Se monta con key={id} desde la página, así que cada jugador arranca con el estado inicial.
    let activo = true
    const esSuperusuario = usuario.rol === 'SUPERUSUARIO'
    const consultas = [
      solicitarProtegida(endpoints.jugador(id)),
      pedirCotizaciones(solicitarProtegida, id, esSuperusuario),
    ]
    if (esSuperusuario) consultas.push(solicitarProtegida(endpoints.estrategias))

    Promise.allSettled(consultas).then((resultados) => {
      if (!activo) return
      const jugador = resultados[0]
      const cotizaciones = resultados[1]
      const estrategias = resultados[2]
      let estrategiaActiva = null
      let errorEstrategias = null

      if (esSuperusuario && estrategias?.status === 'fulfilled') {
        try {
          estrategiaActiva = obtenerEstrategiaActiva(estrategias.value)
        } catch (error) {
          errorEstrategias = error
        }
      } else if (esSuperusuario) {
        errorEstrategias = estrategias?.reason || null
      } else if (cotizaciones.status === 'fulfilled') {
        estrategiaActiva = cotizaciones.value.cotizaciones[0]?.estrategia || null
      }

      setEstado({
        jugador: jugador.status === 'fulfilled' ? jugador.value : null,
        cotizaciones: cotizaciones.status === 'fulfilled' ? cotizaciones.value.cotizaciones : [],
        estrategiaActiva,
        puedeComparar: esSuperusuario,
        cargando: false,
        error: jugador.status === 'rejected' ? jugador.reason : null,
        errorCotizaciones: cotizaciones.status === 'fulfilled' ? cotizaciones.value.error || errorEstrategias : cotizaciones.reason,
      })
    })

    return () => { activo = false }
  }, [id, solicitarProtegida, usuario])

  return { ...estado, cargando: Boolean(usuario && estado.cargando) }
}
