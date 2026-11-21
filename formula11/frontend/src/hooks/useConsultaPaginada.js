import { useCallback, useEffect, useState } from 'react'
import { useSesion } from './useSesion'

// Si se pidió una página que ya no existe (por ejemplo, porque bajó el total), devuelve la última válida; si no, null.
// Como la última válida es menor que totalPages, la corrección no se repite y no hay bucle.
export function ultimaPaginaValida(data, paginaPedida) {
  const totalPages = Number(data?.totalPages ?? 0)
  const vacia = (data?.content?.length ?? 0) === 0
  const fueraDeRango = vacia && Number(data?.totalElements ?? 0) > 0 && totalPages > 0 && paginaPedida >= totalPages
  return fueraDeRango ? totalPages - 1 : null
}

// Consulta paginada a un endpoint protegido. Se vuelve a pedir cada vez que cambia la consulta, se ignoran las
// respuestas de consultas anteriores y una página fuera de rango se corrige a la última válida.
// construirUrl(consulta, usuario) y normalizar(data) tienen que ser estables (funciones de módulo), porque son
// dependencias del efecto. Con habilitado=false no se consulta nada.
export function useConsultaPaginada({ consultaInicial, construirUrl, normalizar, habilitado = true }) {
  const { usuario, solicitarProtegida } = useSesion()
  const activa = Boolean(usuario && habilitado)
  const [consulta, setConsulta] = useState(consultaInicial)
  const [respuesta, setRespuesta] = useState(() => normalizar(null))
  const [cargando, setCargando] = useState(activa)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!activa) return undefined

    let vigente = true

    solicitarProtegida(construirUrl(consulta, usuario))
      .then((data) => {
        if (!vigente) return
        const ultima = ultimaPaginaValida(data, consulta.page)
        if (ultima !== null) {
          setConsulta((actual) => (actual.page === ultima ? actual : { ...actual, page: ultima }))
          return
        }
        setRespuesta(normalizar(data))
      })
      .catch((requestError) => {
        if (vigente) setError(requestError)
      })
      .finally(() => {
        if (vigente) setCargando(false)
      })

    return () => { vigente = false }
  }, [activa, consulta, construirUrl, normalizar, solicitarProtegida, usuario])

  // Marca la carga y aplica el cambio. Hay que llamarla solo si la consulta cambia: si no, el efecto no vuelve a
  // correr y quedaría cargando.
  const actualizarConsulta = useCallback((cambio) => {
    setCargando(true)
    setError(null)
    setConsulta(cambio)
  }, [])

  return { consulta, respuesta, cargando: activa && cargando, error, actualizarConsulta }
}
