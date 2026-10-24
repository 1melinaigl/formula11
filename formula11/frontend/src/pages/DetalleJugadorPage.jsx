import { useEffect } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import PosicionJugador from '../components/catalogo/PosicionJugador'
import { posiciones } from '../components/catalogo/posiciones'
import AvisoError from '../components/comun/AvisoError'
import EstadoCarga from '../components/comun/EstadoCarga'
import EstadoVacio from '../components/comun/EstadoVacio'
import GraficoEvolucion from '../components/cotizaciones/GraficoEvolucion'
import ResumenCotizacion from '../components/cotizaciones/ResumenCotizacion'
import TablaCotizaciones from '../components/cotizaciones/TablaCotizaciones'
import { ESTRATEGIAS, resumenPorEstrategia } from '../components/cotizaciones/cotizaciones'
import PanelOperacion from '../components/mercado/PanelOperacion'
import { precioVigente } from '../components/mercado/mercado'
import { useDetalleJugador } from '../hooks/useDetalleJugador'
import { useMercado } from '../hooks/useMercado'
import { useSesion } from '../hooks/useSesion'
import AccesoDenegadoPage from './AccesoDenegadoPage'

function PanelCotizaciones({ cotizaciones, error, estrategiaActiva, puedeComparar }) {
  if (error && cotizaciones.length === 0) return <AvisoError error={error} />
  if (cotizaciones.length === 0) return <EstadoVacio titulo="Sin cotizaciones" detalle="Este jugador todavía no tiene cotizaciones calculadas." />
  return (
    <>
      {error && <AvisoError error={error} />}
      <ResumenCotizacion
        resumen={resumenPorEstrategia(cotizaciones, puedeComparar ? Object.keys(ESTRATEGIAS) : undefined)}
        estrategiaActiva={estrategiaActiva}
      />
      <GraficoEvolucion cotizaciones={cotizaciones} estrategiaActiva={estrategiaActiva} />
      <TablaCotizaciones cotizaciones={cotizaciones} />
    </>
  )
}

function DetalleJugador({ id, operacionInicial }) {
  const { jugador, cotizaciones, cargando, error, errorCotizaciones, estrategiaActiva, puedeComparar } = useDetalleJugador(id)
  const mercado = useMercado(id)

  if (cargando) return <EstadoCarga mensaje="Cargando jugador..." />
  if (error) return <AvisoError error={error} />

  return (
    <div className="player-detail-layout">
      <div className="player-detail-side">
        <article className="player-card">
          <div className="player-card-heading"><span>Ficha del jugador</span><PosicionJugador jugador={jugador} /></div>
          <h2>{jugador.nombre}</h2>
          <dl className="player-details">
            <dt>Equipo</dt><dd>{jugador.equipo}</dd>
            <dt>Liga</dt><dd>{jugador.liga}</dd>
            <dt>Posición</dt><dd>{jugador.posicionNombre || posiciones[jugador.posicion] || jugador.posicion || 'No informada'}</dd>
          </dl>
        </article>
        <PanelOperacion
          disponibles={jugador.tokensDisponibles ?? null}
          mercado={mercado}
          operacionInicial={operacionInicial}
          precio={precioVigente(jugador, cotizaciones)}
        />
      </div>
      <section className="player-card quote-panel" aria-labelledby="titulo-cotizaciones">
        <div className="player-card-heading"><span id="titulo-cotizaciones">Evolución de la cotización</span></div>
        <PanelCotizaciones cotizaciones={cotizaciones} error={errorCotizaciones} estrategiaActiva={estrategiaActiva} puedeComparar={puedeComparar} />
      </section>
    </div>
  )
}

export default function DetalleJugadorPage() {
  const { id } = useParams()
  const [parametros] = useSearchParams()
  const { usuario } = useSesion()
  const navigate = useNavigate()

  useEffect(() => {
    const volver = (event) => { if (event.key === 'Escape') navigate('/catalogo') }
    window.addEventListener('keydown', volver)
    return () => window.removeEventListener('keydown', volver)
  }, [navigate])

  if (!usuario) return <AccesoDenegadoPage />

  return (
    <section className="screen catalog-screen">
      <div className="catalog-content">
        <div className="catalog-title-row"><div><p className="eyebrow"></p><h1 className="panel-heading">Detalle del jugador</h1></div><button className="btn ghost" type="button" onClick={() => navigate('/catalogo')}>Volver al catálogo</button></div>
        <DetalleJugador id={id} key={id} operacionInicial={parametros.get('operacion')?.toUpperCase()} />
      </div>
    </section>
  )
}
