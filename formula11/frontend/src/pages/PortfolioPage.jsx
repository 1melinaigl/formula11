import { useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import AvisoError from '../components/comun/AvisoError'
import EstadoCarga from '../components/comun/EstadoCarga'
import { GraficoComposicion, GraficoEvolucionPortfolio, GraficoGanancias } from '../components/portfolio/GraficosPortfolio'
import ResumenPortfolio from '../components/portfolio/ResumenPortfolio'
import TablaTenencias from '../components/portfolio/TablaTenencias'
import { formatearFecha } from '../components/cotizaciones/cotizaciones'
import { composicion, gananciasPorJugador } from '../components/portfolio/portfolio'
import { usePortfolio } from '../hooks/usePortfolio'
import { useSesion } from '../hooks/useSesion'
import AccesoDenegadoPage from './AccesoDenegadoPage'

function ContenidoPortfolio({ resumen, tenencias, evolucion, errorEvolucion }) {

  if (tenencias.length === 0) {
    return (
      <>
        <ResumenPortfolio resumen={resumen} />
        <div className="empty-state" role="status">
          <h2>Todavía no tenés tokens</h2>
          <p>Elegí jugadores desde el <Link to="/ranking">ranking</Link> o el <Link to="/catalogo">catálogo</Link> y comprá tus primeros tokens.</p>
        </div>
      </>
    )
  }

  const ganancias = gananciasPorJugador(tenencias)
  return (
    <>
      <ResumenPortfolio resumen={resumen} />
      <div className="portfolio-charts">
        {errorEvolucion && <AvisoError error={errorEvolucion} />}
        {!errorEvolucion && evolucion.length > 1 && <GraficoEvolucionPortfolio evolucion={evolucion} />}
        <GraficoComposicion filas={composicion(tenencias)} />
        {ganancias.length > 0 && <GraficoGanancias filas={ganancias} />}
      </div>
      <h2 className="portfolio-subtitle">Tus tokens</h2>
      <TablaTenencias tenencias={tenencias} />
    </>
  )
}

export default function PortfolioPage() {
  const { usuario } = useSesion()
  const navigate = useNavigate()
  const portfolio = usePortfolio()

  useEffect(() => {
    const volver = (event) => { if (event.key === 'Escape') navigate('/') }
    window.addEventListener('keydown', volver)
    return () => window.removeEventListener('keydown', volver)
  }, [navigate])

  if (!usuario) return <AccesoDenegadoPage error={portfolio.error} />

  return (
    <section className="screen catalog-screen">
      <div className="catalog-content">
        <div className="catalog-title-row">
          <div>
            <p className="eyebrow">{portfolio.resumen.fechaValuacion && `Valuado al ${formatearFecha(portfolio.resumen.fechaValuacion)}`}</p>
            <h1 className="panel-heading">Mi portfolio</h1>
          </div>
          <button className="btn ghost" type="button" onClick={() => navigate('/')}>Menú</button>
        </div>
        {portfolio.cargando && <EstadoCarga mensaje="Cargando portfolio..." />}
        {!portfolio.cargando && portfolio.error && <AvisoError error={portfolio.error} />}
        {!portfolio.cargando && !portfolio.error && <ContenidoPortfolio {...portfolio} />}
      </div>
    </section>
  )
}
