import { useCallback, useEffect, useRef, useState } from 'react'
import { endpoints } from '../api/endpoints'
import { ESTRATEGIAS, normalizarCotizaciones, obtenerEstrategiaActiva } from '../components/cotizaciones/cotizaciones'
import { useSesion } from './useSesion'

// Semanas por página del historial. La página 0 tiene las más recientes.
export const TAMANO_PAGINA_COTIZACIONES = 12

function pedirCotizaciones(solicitarProtegida, id, esSuperusuario, page) {
  // El público ve solo la estrategia activa; el superusuario puede pedir las dos y compararlas (si no, el backend da 403).
  const codigos = esSuperusuario ? Object.keys(ESTRATEGIAS) : [undefined]
  const pedidos = codigos.map((estrategia) => solicitarProtegida(
    endpoints.cotizacionesJugador(id, { estrategia, page, size: TAMANO_PAGINA_COTIZACIONES })))
  return Promise.allSettled(pedidos).then((resultados) => {
    const respuestas = resultados.filter((resultado) => resultado.status === 'fulfilled').map((resultado) => resultado.value)
    return {
      cotizaciones: respuestas.flatMap(normalizarCotizaciones),
      // Con dos estrategias, la que tenga más semanas define cuántas páginas hay.
      totalPages: Math.max(0, ...respuestas.map((respuesta) => Number(respuesta?.totalPages ?? 0))),
      error: resultados.find((resultado) => resultado.status === 'rejected')?.reason || null,
    }
  })
}

export function useDetalleJugador(id) {
  const { usuario, solicitarProtegida } = useSesion()
  const esSuperusuario = usuario?.rol === 'SUPERUSUARIO'
  const [estado, setEstado] = useState({
    jugador: null,
    // Cotizaciones de la página que se está mirando, para el gráfico y la tabla.
    cotizaciones: [],
    // Cotizaciones de la página 0: el resumen siempre muestra la vigente, aunque se mire una página anterior.
    actuales: [],
    page: 0,
    totalPages: 0,
    estrategiaActiva: null,
    puedeComparar: false,
    cargando: Boolean(usuario),
    cargandoPagina: false,
    error: null,
    errorCotizaciones: null,
  })
  const paginaPedida = useRef(0)

  useEffect(() => {
    if (!usuario) return undefined

    // Se monta con key={id} desde la página, así que cada jugador arranca con el estado inicial.
    let activo = true
    const consultas = [
      solicitarProtegida(endpoints.jugador(id)),
      pedirCotizaciones(solicitarProtegida, id, esSuperusuario, 0),
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

      const pagina = cotizaciones.status === 'fulfilled' ? cotizaciones.value : { cotizaciones: [], totalPages: 0 }
      setEstado({
        jugador: jugador.status === 'fulfilled' ? jugador.value : null,
        cotizaciones: pagina.cotizaciones,
        actuales: pagina.cotizaciones,
        page: 0,
        totalPages: pagina.totalPages,
        estrategiaActiva,
        puedeComparar: esSuperusuario,
        cargando: false,
        cargandoPagina: false,
        error: jugador.status === 'rejected' ? jugador.reason : null,
        errorCotizaciones: cotizaciones.status === 'fulfilled' ? cotizaciones.value.error || errorEstrategias : cotizaciones.reason,
      })
    })

    return () => { activo = false }
  }, [esSuperusuario, id, solicitarProtegida, usuario])

  // Solo vuelve a pedir el historial; la ficha, el resumen y la estrategia activa no cambian de página.
  // Si se cambia de página antes de que llegue la respuesta anterior, esa respuesta se descarta.
  const cambiarPagina = useCallback((page) => {
    paginaPedida.current = page
    setEstado((actual) => ({ ...actual, cargandoPagina: true }))
    pedirCotizaciones(solicitarProtegida, id, esSuperusuario, page).then((pagina) => {
      if (paginaPedida.current !== page) return
      setEstado((actual) => ({
        ...actual,
        cotizaciones: pagina.cotizaciones,
        page,
        totalPages: pagina.totalPages,
        cargandoPagina: false,
        errorCotizaciones: pagina.error,
      }))
    })
  }, [esSuperusuario, id, solicitarProtegida])

  return { ...estado, cargando: Boolean(usuario && estado.cargando), cambiarPagina }
}
