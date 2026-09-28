import { useCallback, useEffect, useMemo, useState } from 'react'
import { endpoints } from '../api/endpoints'
import { useSesion } from './useSesion'

const filtrosIniciales = { nombre: '', liga: '', posicion: '', equipo: '' }

export function useCatalogo() {
  const { usuario, solicitarProtegida } = useSesion()
  const [jugadores, setJugadores] = useState([])
  const [filtros, setFiltros] = useState(filtrosIniciales)
  const [cargando, setCargando] = useState(Boolean(usuario))
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!usuario) return undefined

    let activo = true
    solicitarProtegida(endpoints.jugadores)
      .then((data) => {
        if (activo) setJugadores(Array.isArray(data) ? data : [])
      })
      .catch((requestError) => {
        if (activo) setError(requestError)
      })
      .finally(() => {
        if (activo) setCargando(false)
      })

    return () => { activo = false }
  }, [solicitarProtegida, usuario])

  const actualizarFiltro = useCallback((nombre, valor) => {
    setFiltros((actuales) => ({ ...actuales, [nombre]: valor }))
  }, [])

  const limpiarFiltros = useCallback(() => setFiltros(filtrosIniciales), [])

  const resultados = useMemo(() => {
    const nombre = filtros.nombre.trim().toLocaleLowerCase()
    return jugadores.filter((jugador) => (
      (!nombre || jugador.nombre?.toLocaleLowerCase().includes(nombre))
      && (!filtros.liga || jugador.liga === filtros.liga)
      && (!filtros.posicion || jugador.posicionCodigo === filtros.posicion || jugador.posicion === filtros.posicion)
      && (!filtros.equipo || jugador.equipo === filtros.equipo)
    ))
  }, [filtros, jugadores])

  const opciones = useMemo(() => ({
    ligas: [...new Set(jugadores.map((jugador) => jugador.liga).filter(Boolean))].sort(),
    equipos: [...new Set(jugadores.map((jugador) => jugador.equipo).filter(Boolean))].sort(),
  }), [jugadores])

  return { cargando: Boolean(usuario && cargando), error, filtros, jugadores, resultados, opciones, actualizarFiltro, limpiarFiltros }
}