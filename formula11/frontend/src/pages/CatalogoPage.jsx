import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import BarraFiltros from '../components/catalogo/BarraFiltros'
import EstadoCatalogo from '../components/catalogo/EstadoCatalogo'
import FichaJugador from '../components/catalogo/FichaJugador'
import ListaJugadores from '../components/catalogo/ListaJugadores'
import AccesoDenegadoPage from './AccesoDenegadoPage'
import { useCatalogo } from '../hooks/useCatalogo'
import { useSesion } from '../hooks/useSesion'

export default function CatalogoPage() {
  const { usuario } = useSesion()
  const navigate = useNavigate()
  const catalogo = useCatalogo()
  const [seleccionado, setSeleccionado] = useState(null)
  const [mostrarFicha, setMostrarFicha] = useState(false)

  useEffect(() => {
    const volver = (event) => { if (event.key === 'Escape') navigate('/') }
    window.addEventListener('keydown', volver)
    return () => window.removeEventListener('keydown', volver)
  }, [navigate])

  if (!usuario) return <AccesoDenegadoPage error={catalogo.error} />

  const jugadorVisible = catalogo.resultados.find((jugador) => jugador.id === seleccionado?.id) || null
  const seleccionar = (jugador) => { setSeleccionado(jugador); setMostrarFicha(true) }
  const estado = catalogo.cargando || catalogo.error || catalogo.jugadores.length === 0 || catalogo.resultados.length === 0
    ? <EstadoCatalogo cargando={catalogo.cargando} error={catalogo.error} hayJugadores={catalogo.jugadores.length > 0} hayResultados={catalogo.resultados.length > 0} />
    : null

  return (
    <section className="screen catalog-screen">
      <div className="catalog-content">
        <div className="catalog-title-row"><div><p className="eyebrow">GET /API/JUGADORES</p><h1 className="panel-heading">Catálogo de jugadores</h1></div><button className="btn ghost" type="button" onClick={() => navigate('/')}>Menú</button></div>
        <BarraFiltros {...catalogo} />
        <p className="catalog-count" aria-live="polite">{catalogo.resultados.length} jugadores</p>
        {estado || <div className={`catalog-columns ${mostrarFicha ? 'show-detail' : ''}`}><div className="catalog-list-panel"><ListaJugadores jugadores={catalogo.resultados} seleccionado={jugadorVisible} seleccionar={seleccionar} /></div><div className="catalog-detail-panel"><FichaJugador jugador={jugadorVisible} volver={() => setMostrarFicha(false)} /></div></div>}
      </div>
    </section>
  )
}