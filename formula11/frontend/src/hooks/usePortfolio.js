import { useEffect, useState } from 'react'
import { endpoints } from '../api/endpoints'
import { enriquecerTenencias, normalizarEvolucion, ordenarTenencias, resumenPortfolio } from '../components/portfolio/portfolio'
import { useSesion } from './useSesion'

// Con habilitado=false (superusuario) no se consulta nada: no tiene portfolio de inversión.
export function usePortfolio({ habilitado = true } = {}) {
  const { usuario, solicitarProtegida } = useSesion()
  const [estado, setEstado] = useState({
    resumen: resumenPortfolio(null),
    tenencias: [],
    evolucion: [],
    cargando: Boolean(usuario && habilitado),
    error: null,
    errorEvolucion: null,
  })

  useEffect(() => {
    if (!usuario || !habilitado) return undefined

    let activo = true

    Promise.allSettled([
      solicitarProtegida(endpoints.portfolio(usuario.id)),
      solicitarProtegida(endpoints.evolucionPortfolio(usuario.id)),
    ]).then(([portfolio, evolucion]) => {
      if (!activo) return
      const datos = portfolio.status === 'fulfilled' ? portfolio.value : null
      setEstado({
        resumen: resumenPortfolio(datos),
        tenencias: ordenarTenencias(enriquecerTenencias(datos?.posiciones || []).filter((tenencia) => tenencia.cantidad > 0)),
        evolucion: evolucion.status === 'fulfilled' ? normalizarEvolucion(evolucion.value) : [],
        cargando: false,
        error: portfolio.status === 'rejected' ? portfolio.reason : null,
        errorEvolucion: evolucion.status === 'rejected' ? evolucion.reason : null,
      })
    })

    return () => { activo = false }
  }, [habilitado, solicitarProtegida, usuario])

  return { ...estado, cargando: Boolean(usuario && habilitado && estado.cargando) }
}
