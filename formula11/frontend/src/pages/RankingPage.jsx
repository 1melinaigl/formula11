import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { posiciones } from '../components/catalogo/posiciones'
import AvisoError from '../components/comun/AvisoError'
import EstadoCarga from '../components/comun/EstadoCarga'
import EstadoVacio from '../components/comun/EstadoVacio'
import { formatearFecha, nombreEstrategia } from '../components/cotizaciones/cotizaciones'
import TablaRanking from '../components/ranking/TablaRanking'
import { filtrarPorPosicion } from '../components/ranking/ranking'
import { useRanking } from '../hooks/useRanking'
import { useSesion } from '../hooks/useSesion'
import AccesoDenegadoPage from './AccesoDenegadoPage'

const FILTROS = [['', 'Todos'], ...Object.entries(posiciones)]

function ContenidoRanking({ ranking, posicion }) {
  if (ranking.length === 0) return <EstadoVacio titulo="Ranking vacío" detalle="Todavía no hay cotizaciones para armar el ranking." />
  const visibles = filtrarPorPosicion(ranking, posicion)
  if (visibles.length === 0) return <EstadoVacio titulo="Sin jugadores" detalle={`No hay jugadores en el ranking para la posición ${posiciones[posicion]}.`} />
  return <TablaRanking ranking={visibles} />
}

export default function RankingPage() {
  const { usuario } = useSesion()
  const navigate = useNavigate()
  const { ranking, cargando, error } = useRanking()
  const [posicion, setPosicion] = useState('')

  useEffect(() => {
    const volver = (event) => { if (event.key === 'Escape') navigate('/') }
    window.addEventListener('keydown', volver)
    return () => window.removeEventListener('keydown', volver)
  }, [navigate])

  if (!usuario) return <AccesoDenegadoPage error={error} />

  const referencia = ranking[0]

  return (
    <section className="screen catalog-screen">
      <div className="catalog-content">
        <div className="catalog-title-row">
          <div>
            <p className="eyebrow">{referencia?.estrategia && `Estrategia activa: ${nombreEstrategia(referencia.estrategia)}${referencia.fecha ? ` · al ${formatearFecha(referencia.fecha)}` : ''}`}</p>
            <h1 className="panel-heading">Ranking de jugadores</h1>
          </div>
          <button className="btn ghost" type="button" onClick={() => navigate('/')}>Menú</button>
        </div>
        <div className="ranking-filters" role="group" aria-label="Filtrar por posición">
          {FILTROS.map(([codigo, nombre]) => (
            <button aria-pressed={posicion === codigo} className="filter-chip" key={codigo || 'todos'} type="button" onClick={() => setPosicion(codigo)}>{nombre}</button>
          ))}
        </div>
        {cargando && <EstadoCarga mensaje="Cargando ranking..." />}
        {!cargando && error && <AvisoError error={error} />}
        {!cargando && !error && <ContenidoRanking ranking={ranking} posicion={posicion} />}
      </div>
    </section>
  )
}
