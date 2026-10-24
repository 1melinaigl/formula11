import { useEffect, useState } from 'react'
import { endpoints } from '../api/endpoints'
import { normalizarPortfolio } from '../components/mercado/mercado'
import { enriquecerTenencias, normalizarEvolucion } from '../components/portfolio/portfolio'
import { useSesion } from './useSesion'

export function usePortfolio() {
  const { usuario, solicitarProtegida } = useSesion()
  const [estado, setEstado] = useState({
    saldo: 0,
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
      const datos = portfolio.status === 'fulfilled' ? normalizarPortfolio(portfolio.value) : null
      setEstado({
        saldo: datos?.saldo ?? 0,
        tenencias: datos ? enriquecerTenencias(datos.tenencias).filter((tenencia) => tenencia.cantidad > 0) : [],
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
