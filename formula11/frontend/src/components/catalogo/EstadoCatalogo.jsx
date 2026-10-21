import EstadoCarga from '../comun/EstadoCarga'
import EstadoVacio from '../comun/EstadoVacio'
import AvisoError from '../comun/AvisoError'

export default function EstadoCatalogo({ cargando, error, hayJugadores, hayResultados, hayFiltros }) {
  if (cargando) return <EstadoCarga mensaje="Cargando catálogo..." />
  if (error) return <AvisoError error={error} />
  if (!hayResultados && hayFiltros) return <EstadoVacio titulo="Sin coincidencias" detalle="No hay jugadores que coincidan con los filtros." />
  if (!hayJugadores) return <EstadoVacio titulo="Catálogo vacío" detalle="El catálogo todavía no tiene jugadores." />
  if (!hayResultados) return <EstadoVacio titulo="Catálogo vacío" detalle="El catálogo todavía no tiene jugadores." />
  return null
}