import { useCallback, useEffect, useState } from 'react'
import { endpoints } from '../api/endpoints'
import { describirResultado, normalizarMercado, normalizarPortfolio, tenenciaDe } from '../components/mercado/mercado'
import { useSesion } from './useSesion'

// Saldo y tenencia salen del portfolio; precio y disponibilidad, de /jugadores/{id}/mercado.
// Después de cada operación se vuelven a pedir los dos: el backend es la fuente de verdad.
export function useMercado(jugadorId) {
  const { usuario, solicitarProtegida } = useSesion()
  const [datos, setDatos] = useState({ portfolio: null, mercado: normalizarMercado(null), cargando: Boolean(usuario), error: null })
  const [operacion, setOperacion] = useState({ enviando: false, resultado: null, error: null })
  const [version, setVersion] = useState(0)

  useEffect(() => {
    if (!usuario) return undefined

    let activo = true

    Promise.all([
      solicitarProtegida(endpoints.portfolio(usuario.id)),
      solicitarProtegida(endpoints.mercadoJugador(jugadorId)),
    ])
      .then(([portfolio, mercado]) => {
        if (activo) setDatos({ portfolio: normalizarPortfolio(portfolio), mercado: normalizarMercado(mercado), cargando: false, error: null })
      })
      .catch((error) => {
        if (activo) setDatos((actual) => ({ ...actual, cargando: false, error }))
      })

    return () => { activo = false }
  }, [jugadorId, solicitarProtegida, usuario, version])

  const operar = useCallback(async (tipo, cantidad) => {
    setOperacion({ enviando: true, resultado: null, error: null })
    try {
      const url = tipo === 'COMPRA' ? endpoints.comprar : endpoints.vender
      const orden = await solicitarProtegida(url, { method: 'POST', body: { jugadorId: Number(jugadorId), cantidad } })
      setOperacion({ enviando: false, resultado: describirResultado(orden), error: null })
      setVersion((actual) => actual + 1)
      return true
    } catch (error) {
      setOperacion({ enviando: false, resultado: null, error })
      return false
    }
  }, [jugadorId, solicitarProtegida])

  const limpiarOperacion = useCallback(() => setOperacion({ enviando: false, resultado: null, error: null }), [])

  return {
    cargando: Boolean(usuario && datos.cargando),
    error: datos.error,
    saldo: datos.portfolio?.saldo ?? 0,
    tenencia: datos.portfolio ? tenenciaDe(datos.portfolio, jugadorId) : null,
    ...datos.mercado,
    enviando: operacion.enviando,
    resultado: operacion.resultado,
    errorOperacion: operacion.error,
    operar,
    limpiarOperacion,
  }
}
