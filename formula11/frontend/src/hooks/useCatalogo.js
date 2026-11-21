import { useCallback, useEffect, useMemo, useState } from 'react'
import { endpoints } from '../api/endpoints'
import { useConsultaPaginada } from './useConsultaPaginada'

export const LIGAS = ['Premier League', 'Bundesliga', 'La Liga', 'Serie A', 'Ligue 1']
export const CAMPOS_ORDEN = [
  ['nombre', 'Nombre'],
  ['equipo', 'Equipo'],
  ['liga', 'Liga'],
  ['posicion', 'Posición'],
]
const TAMANO_PAGINA = 9
const FILTROS = ['liga', 'equipo', 'posicion']

const consultaInicial = {
  page: 0,
  sort: 'nombre,asc',
  liga: '',
  equipo: '',
  posicion: '',
}

function construirUrl(consulta) {
  const params = new URLSearchParams({ page: String(consulta.page), size: String(TAMANO_PAGINA), sort: consulta.sort })
  for (const nombre of FILTROS) {
    if (consulta[nombre].trim()) params.set(nombre, consulta[nombre].trim())
  }
  return `${endpoints.jugadores}?${params.toString()}`
}

function normalizarCatalogo(data) {
  return {
    content: data?.content ?? [],
    page: data?.page ?? 0,
    totalElements: data?.totalElements ?? 0,
    totalPages: data?.totalPages ?? 0,
  }
}

export function useCatalogo() {
  const { consulta, respuesta, cargando, error, actualizarConsulta } = useConsultaPaginada({
    consultaInicial,
    construirUrl,
    normalizar: normalizarCatalogo,
  })
  const [equipoInput, setEquipoInput] = useState('')

  // El equipo se escribe a mano: se espera a que el usuario deje de tipear para consultar. Solo se actualiza la
  // consulta si el equipo cambió; si no, el efecto de la consulta no correría y quedaría cargando.
  useEffect(() => {
    if (equipoInput === consulta.equipo) return undefined
    const timer = window.setTimeout(() => {
      actualizarConsulta((actual) => ({ ...actual, equipo: equipoInput, page: 0 }))
    }, 300)
    return () => window.clearTimeout(timer)
  }, [actualizarConsulta, consulta.equipo, equipoInput])

  const actualizarFiltro = useCallback((nombre, valor) => {
    if (nombre === 'equipo') {
      setEquipoInput(valor)
      return
    }
    actualizarConsulta((actual) => ({ ...actual, [nombre]: valor, page: 0 }))
  }, [actualizarConsulta])

  const actualizarOrden = useCallback((campo, direccion) => {
    actualizarConsulta((actual) => ({ ...actual, sort: `${campo},${direccion}`, page: 0 }))
  }, [actualizarConsulta])

  const limpiarFiltros = useCallback(() => {
    setEquipoInput('')
    actualizarConsulta((actual) => ({ ...actual, liga: '', equipo: '', posicion: '', page: 0 }))
  }, [actualizarConsulta])

  const cambiarPagina = useCallback((page) => {
    actualizarConsulta((actual) => ({ ...actual, page }))
  }, [actualizarConsulta])

  const filtros = useMemo(() => ({ liga: consulta.liga, equipo: equipoInput, posicion: consulta.posicion }), [consulta.liga, consulta.posicion, equipoInput])
  const [campo, direccion] = consulta.sort.split(',')

  return {
    cargando,
    error,
    filtros,
    jugadores: respuesta.content,
    resultados: respuesta.content,
    page: respuesta.page,
    totalElements: respuesta.totalElements,
    totalPages: respuesta.totalPages,
    campoOrden: campo,
    direccionOrden: direccion,
    opciones: { ligas: LIGAS },
    actualizarFiltro,
    actualizarOrden,
    cambiarPagina,
    limpiarFiltros,
  }
}
