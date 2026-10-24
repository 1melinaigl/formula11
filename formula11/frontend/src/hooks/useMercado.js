import { useCallback, useEffect, useState } from 'react'
import { endpoints } from '../api/endpoints'
import { describirResultado, normalizarPortfolio, tenenciaDe } from '../components/mercado/mercado'
import { useSesion } from './useSesion'

export function useMercado(jugadorId) {
  const { usuario, solicitarProtegida } = useSesion()
  const [portfolio, setPortfolio] = useState({ datos: null, cargando: Boolean(usuario), error: null })
  const [operacion, setOperacion] = useState({ enviando: false, resultado: null, error: null })
  const [version, setVersion] = useState(0)

  useEffect(() => {
    if (!usuario) return undefined

    let activo = true

    solicitarProtegida(endpoints.portfolio(usuario.id))
      .then((data) => {
        if (activo) setPortfolio({ datos: normalizarPortfolio(data), cargando: false, error: null })
      })
      .catch((error) => {
        if (activo) setPortfolio((actual) => ({ ...actual, cargando: false, error }))
      })

    return () => { activo = false }
  }, [solicitarProtegida, usuario, version])

  const operar = useCallback(async (tipo, cantidad) => {
    setOperacion({ enviando: true, resultado: null, error: null })
    try {
      const url = tipo === 'COMPRA' ? endpoints.comprar : endpoints.vender
      const orden = await solicitarProtegida(url, { method: 'POST', body: { jugadorId: Number(jugadorId), cantidad } })
      setOperacion({ enviando: false, resultado: describirResultado(tipo, { cantidad, ...orden }), error: null })
      setVersion((actual) => actual + 1)
      return true
    } catch (error) {
      setOperacion({ enviando: false, resultado: null, error })
      return false
    }
  }, [jugadorId, solicitarProtegida])

  const limpiarOperacion = useCallback(() => setOperacion({ enviando: false, resultado: null, error: null }), [])

  const datos = portfolio.datos
  return {
    cargando: Boolean(usuario && portfolio.cargando),
    error: portfolio.error,
    saldo: datos?.saldo ?? 0,
    tenencia: datos ? tenenciaDe(datos, jugadorId) : null,
    enviando: operacion.enviando,
    resultado: operacion.resultado,
    errorOperacion: operacion.error,
    operar,
    limpiarOperacion,
  }
}
