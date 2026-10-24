import { useEffect, useState } from 'react'
import { endpoints } from '../api/endpoints'
import { enriquecerTenencias, normalizarEvolucion, resumenPortfolio } from '../components/portfolio/portfolio'
import { useSesion } from './useSesion'

export function usePortfolio() {
  const { usuario, solicitarProtegida } = useSesion()
  const [estado, setEstado] = useState({
    resumen: resumenPortfolio(null),
    tenencias: [],
    evolucion: [],
    cargando: Boolean(usuario),
    error: null,
    errorEvolucion: null,
  })

  useEffect(() => {
    if (!usuario) return undefined

    let activo = true

    Promise.allSettled([
      solicitarProtegida(endpoints.portfolio(usuario.id)),
      solicitarProtegida(endpoints.evolucionPortfolio(usuario.id)),
    ]).then(([portfolio, evolucion]) => {
      if (!activo) return
      const datos = portfolio.status === 'fulfilled' ? portfolio.value : null
      setEstado({
        resumen: resumenPortfolio(datos),
        tenencias: enriquecerTenencias(datos?.posiciones || []).filter((tenencia) => tenencia.cantidad > 0),
        evolucion: evolucion.status === 'fulfilled' ? normalizarEvolucion(evolucion.value) : [],
        cargando: false,
        error: portfolio.status === 'rejected' ? portfolio.reason : null,
        errorEvolucion: evolucion.status === 'rejected' ? evolucion.reason : null,
      })
    })

    return () => { activo = false }
  }, [solicitarProtegida, usuario])

  return { ...estado, cargando: Boolean(usuario && estado.cargando) }
}
