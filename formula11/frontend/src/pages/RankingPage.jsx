import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import PaginacionCatalogo from '../components/catalogo/PaginacionCatalogo'
import { posiciones } from '../components/catalogo/posiciones'
import AvisoError from '../components/comun/AvisoError'
import EstadoCarga from '../components/comun/EstadoCarga'
import EstadoVacio from '../components/comun/EstadoVacio'
import { formatearFecha, nombreEstrategia } from '../components/cotizaciones/cotizaciones'
import TablaRanking from '../components/ranking/TablaRanking'
import { LIGAS } from '../hooks/useCatalogo'
import { useRanking } from '../hooks/useRanking'
import { useSesion } from '../hooks/useSesion'
import AccesoDenegadoPage from './AccesoDenegadoPage'

const FILTROS = [['', 'Todos'], ...Object.entries(posiciones)]

function describirEstrategia({ estrategia, version, fecha }) {
  if (!estrategia) return ''
  return `Estrategia activa: ${nombreEstrategia(estrategia)}${version ? ` v${version}` : ''}${fecha ? ` · al ${formatearFecha(fecha)}` : ''}`
}

function ContenidoRanking({ filas, posicion, liga }) {
  if (filas.length > 0) return <TablaRanking ranking={filas} />
  if (posicion || liga) return <EstadoVacio titulo="Sin jugadores" detalle="No hay jugadores en el ranking con esos filtros." />
  return <EstadoVacio titulo="Ranking vacío" detalle="Todavía no hubo un recálculo de cotizaciones." />
}

export default function RankingPage() {
  const { usuario } = useSesion()
  const navigate = useNavigate()
  const ranking = useRanking()

  useEffect(() => {
    const volver = (event) => { if (event.key === 'Escape') navigate('/') }
    window.addEventListener('keydown', volver)
    return () => window.removeEventListener('keydown', volver)
  }, [navigate])

  if (!usuario) return <AccesoDenegadoPage error={ranking.error} />

  return (
    <section className="screen catalog-screen">
      <div className="catalog-content">
        <div className="catalog-title-row">
          <div>
            <p className="eyebrow">{describirEstrategia(ranking)}</p>
            <h1 className="panel-heading">Ranking de jugadores</h1>
          </div>
          <button className="btn ghost" type="button" onClick={() => navigate('/')}>Menú</button>
        </div>
        <div className="ranking-toolbar">
          <div className="ranking-filters" role="group" aria-label="Filtrar por posición">
            {FILTROS.map(([codigo, nombre]) => (
              <button aria-pressed={ranking.posicion === codigo} className="filter-chip" key={codigo || 'todos'} type="button" onClick={() => ranking.cambiarPosicion(codigo)}>{nombre}</button>
            ))}
          </div>
          <div className="catalog-filter ranking-league">
            <label htmlFor="ranking-liga">Liga</label>
            <select id="ranking-liga" value={ranking.liga} onChange={(event) => ranking.cambiarLiga(event.target.value)}>
              <option value="">Todas</option>
              {LIGAS.map((liga) => <option key={liga} value={liga}>{liga}</option>)}
            </select>
          </div>
        </div>
        {ranking.cargando && <EstadoCarga mensaje="Cargando ranking..." />}
        {!ranking.cargando && ranking.error && <AvisoError error={ranking.error} />}
        {!ranking.cargando && !ranking.error && <ContenidoRanking filas={ranking.filas} liga={ranking.liga} posicion={ranking.posicion} />}
        {!ranking.cargando && !ranking.error && ranking.totalPages > 1 && (
          <PaginacionCatalogo cambiarPagina={ranking.cambiarPagina} etiqueta="Paginación del ranking" page={ranking.page} totalPages={ranking.totalPages} />
        )}
      </div>
    </section>
  )
}
