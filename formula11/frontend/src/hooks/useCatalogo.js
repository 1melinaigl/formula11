import { useCallback, useEffect, useMemo, useState } from 'react'
import { endpoints } from '../api/endpoints'
import { useSesion } from './useSesion'

export const LIGAS = ['Premier League', 'Bundesliga', 'La Liga', 'Serie A', 'Ligue 1']
export const CAMPOS_ORDEN = [
  ['nombre', 'Nombre'],
  ['equipo', 'Equipo'],
  ['liga', 'Liga'],
  ['posicion', 'Posición'],
]
const TAMANO_PAGINA = 10

const consultaInicial = {
  page: 0,
  sort: 'nombre,asc',
  liga: '',
  equipo: '',
  posicion: '',
}

function construirUrl(consulta) {
  const params = new URLSearchParams({ page: String(consulta.page), size: String(TAMANO_PAGINA), sort: consulta.sort })
  for (const nombre of ['liga', 'equipo', 'posicion']) {
    if (consulta[nombre].trim()) params.set(nombre, consulta[nombre].trim())
  }
  return `${endpoints.jugadores}?${params.toString()}`
}

export function useCatalogo() {
  const { usuario, solicitarProtegida } = useSesion()
  const [consulta, setConsulta] = useState(consultaInicial)
  const [equipoInput, setEquipoInput] = useState('')
  const [respuesta, setRespuesta] = useState({ content: [], page: 0, totalElements: 0, totalPages: 0 })
  const [cargando, setCargando] = useState(Boolean(usuario))
  const [error, setError] = useState(null)

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setConsulta((actual) => actual.equipo === equipoInput && actual.page === 0
        ? actual
        : { ...actual, equipo: equipoInput, page: 0 })
    }, 300)
    return () => window.clearTimeout(timer)
  }, [equipoInput])

  useEffect(() => {
    if (!usuario) return undefined

    let activo = true

    async function cargarTodo() {
      const todos = []
      let pagina = 0
      let totalPaginas = 1

      while (pagina < totalPaginas) {
        const data = await solicitarProtegida(`${endpoints.jugadores}?page=${pagina}&size=${TAMANIO_PAGINA}`)
        todos.push(...(data?.content ?? []))
        totalPaginas = data?.totalPages ?? 0
        pagina += 1
      }

      return todos
    }

    cargarTodo()
      .then((lista) => { if (activo) setJugadores(lista) })
      .catch((requestError) => { if (activo) setError(requestError) })
      .finally(() => { if (activo) setCargando(false) })
    solicitarProtegida(construirUrl(consulta))
      .then((data) => {
        if (!activo) return
        const paginaFinal = data.totalPages - 1
        if (data.content.length === 0 && data.totalElements > 0 && paginaFinal >= 0 && consulta.page >= data.totalPages) {
          setConsulta((actual) => actual.page === paginaFinal ? actual : { ...actual, page: paginaFinal })
          return
        }
        setRespuesta(data)
      })
      .catch((requestError) => {
        if (activo) setError(requestError)
      })
      .finally(() => {
        if (activo) setCargando(false)
      })

    return () => { activo = false }
  }, [consulta, solicitarProtegida, usuario])

  const actualizarFiltro = useCallback((nombre, valor) => {
    setCargando(true)
    setError(null)
    if (nombre === 'equipo') {
      setEquipoInput(valor)
      return
    }
    setConsulta((actual) => ({ ...actual, [nombre]: valor, page: 0 }))
  }, [])

  const actualizarOrden = useCallback((campo, direccion) => {
    setCargando(true)
    setError(null)
    setConsulta((actual) => ({ ...actual, sort: `${campo},${direccion}`, page: 0 }))
  }, [])

  const limpiarFiltros = useCallback(() => {
    setCargando(true)
    setError(null)
    setEquipoInput('')
    setConsulta((actual) => ({ ...actual, liga: '', equipo: '', posicion: '', page: 0 }))
  }, [])

  const cambiarPagina = useCallback((page) => {
    setCargando(true)
    setError(null)
    setConsulta((actual) => ({ ...actual, page }))
  }, [])

  const filtros = useMemo(() => ({ liga: consulta.liga, equipo: equipoInput, posicion: consulta.posicion }), [consulta.liga, consulta.posicion, equipoInput])
  const [campo, direccion] = consulta.sort.split(',')

  return {
    cargando: Boolean(usuario && cargando),
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