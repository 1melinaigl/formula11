import { useCallback, useEffect, useRef, useState } from 'react'
import { endpoints } from '../api/endpoints'
import {
  INTERVALO_ALERTAS_MS,
  TAMANO_CONSULTA_ALERTAS,
  agregarVistas,
  borrarVistas,
  construirConsultaSenales,
  leerVistas,
  normalizarSenales,
  senalesNuevas,
} from '../components/senales/senales'
import { useSesion } from './useSesion'

const CONSULTA_VIGENTES = construirConsultaSenales({ vigentes: true, page: 0, size: TAMANO_CONSULTA_ALERTAS })

// Se monta una sola vez en el shell de la app: consulta las señales vigentes cada 30 s mientras haya sesión.
// Un error no se muestra (se reintenta en el ciclo siguiente); un 401 limpia la sesión y corta el intervalo.
export function useAlertasSenales() {
  const { usuario, solicitarProtegida } = useSesion()
  const usuarioId = usuario?.id ?? null
  // Las nuevas se guardan con el usuario que las pidió, para no mostrar las de una sesión anterior.
  const [alerta, setAlerta] = useState({ usuarioId: null, nuevas: [] })
  const ultimoUsuario = useRef(null)

  useEffect(() => {
    if (usuarioId == null) {
      // Logout o 401: se olvidan las señales vistas del último usuario.
      if (ultimoUsuario.current != null) borrarVistas(ultimoUsuario.current)
      ultimoUsuario.current = null
      return undefined
    }

    ultimoUsuario.current = usuarioId
    let activo = true
    let pendiente = false

    const consultar = () => {
      // Si la consulta anterior no volvió, se saltea este ciclo para no acumular pedidos.
      if (pendiente) return
      pendiente = true
      solicitarProtegida(`${endpoints.senales}?${CONSULTA_VIGENTES}`)
        .then((data) => {
          if (activo) setAlerta({ usuarioId, nuevas: senalesNuevas(normalizarSenales(data).senales, leerVistas(usuarioId)) })
        })
        .catch(() => {})
        .finally(() => { pendiente = false })
    }

    consultar()
    const intervalo = setInterval(consultar, INTERVALO_ALERTAS_MS)
    return () => {
      activo = false
      clearInterval(intervalo)
    }
  }, [solicitarProtegida, usuarioId])

  // Se vuelve a filtrar contra el registro: la lista de /senales puede haberlas marcado como vistas
  // después de la última consulta, y no deben reaparecer al salir de esa pantalla.
  const nuevas = alerta.usuarioId === usuarioId && usuarioId != null ? senalesNuevas(alerta.nuevas, leerVistas(usuarioId)) : []

  // Cerrar la alerta o ir a la lista marca como vistas las señales que mostraba.
  const marcarVistas = useCallback(() => {
    if (usuarioId != null) agregarVistas(usuarioId, alerta.nuevas.map((senal) => senal.id))
    setAlerta({ usuarioId, nuevas: [] })
  }, [alerta.nuevas, usuarioId])

  return { nuevas, cerrar: marcarVistas, verSenales: marcarVistas }
}
