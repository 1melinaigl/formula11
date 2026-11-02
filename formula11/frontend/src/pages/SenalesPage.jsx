import { useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import PaginacionCatalogo from '../components/catalogo/PaginacionCatalogo'
import AvisoError from '../components/comun/AvisoError'
import EstadoCarga from '../components/comun/EstadoCarga'
import EstadoVacio from '../components/comun/EstadoVacio'
import PanelAciertos from '../components/senales/PanelAciertos'
import TablaSenales from '../components/senales/TablaSenales'
import { agregarVistas } from '../components/senales/senales'
import { useAciertos } from '../hooks/useAciertos'
import { useCompraSenal } from '../hooks/useCompraSenal'
import { useSenales } from '../hooks/useSenales'
import { useSesion } from '../hooks/useSesion'
import AccesoDenegadoPage from './AccesoDenegadoPage'

const VISTAS = [[true, 'Vigentes'], [false, 'Historial']]

function ContenidoSenales({ lista, compra, puedeComprar }) {
  if (lista.senales.length === 0) {
    return lista.vigentes
      ? <EstadoVacio titulo="No hay señales esta semana" detalle="Las señales se generan después de cada recálculo semanal de cotizaciones." />
      : <EstadoVacio titulo="Sin señales" detalle="Todavía no se generó ninguna señal." />
  }
  return <TablaSenales comprar={compra.comprar} enviandoId={compra.enviandoId} puedeComprar={puedeComprar} senales={lista.senales} />
}

export default function SenalesPage() {
  const { usuario } = useSesion()
  const navigate = useNavigate()
  const lista = useSenales()
  const compra = useCompraSenal()
  const aciertos = useAciertos()
  // Una sesión sin rol cuenta como USUARIO; el superusuario no opera.
  const puedeComprar = usuario?.rol !== 'SUPERUSUARIO'

  useEffect(() => {
    const volver = (event) => { if (event.key === 'Escape') navigate('/') }
    window.addEventListener('keydown', volver)
    return () => window.removeEventListener('keydown', volver)
  }, [navigate])

  // Las vigentes que están a la vista ya no tienen que alertar.
  useEffect(() => {
    if (usuario && lista.vigentes && lista.senales.length > 0) agregarVistas(usuario.id, lista.senales.map((senal) => senal.id))
  }, [lista.senales, lista.vigentes, usuario])

  if (!usuario) return <AccesoDenegadoPage error={lista.error} />

  return (
    <section className="screen catalog-screen">
      <div className="catalog-content">
        <div className="catalog-title-row">
          <div><p className="eyebrow">Jugadores subvaluados</p><h1 className="panel-heading">Señales</h1></div>
          <button className="btn ghost" type="button" onClick={() => navigate('/')}>Menú</button>
        </div>
        <div className="ranking-filters" role="group" aria-label="Ver señales">
          {VISTAS.map(([vigentes, nombre]) => (
            <button aria-pressed={lista.vigentes === vigentes} className="filter-chip" key={nombre} type="button" onClick={() => lista.cambiarVista(vigentes)}>{nombre}</button>
          ))}
        </div>
        {compra.resultado && <p className="market-success" role="status">{compra.resultado} <Link to="/operaciones">Ver historial</Link></p>}
        <AvisoError error={compra.error} />
        {lista.cargando && <EstadoCarga mensaje="Cargando señales..." />}
        {!lista.cargando && lista.error && <AvisoError error={lista.error} />}
        {!lista.cargando && !lista.error && <ContenidoSenales compra={compra} lista={lista} puedeComprar={puedeComprar} />}
        {/* Queda montada mientras carga la página nueva: así el foco no se pierde al pulsar Anterior/Siguiente. */}
        {!lista.error && lista.totalPages > 1 && (
          <PaginacionCatalogo cambiarPagina={lista.cambiarPagina} etiqueta="Paginación de señales" page={lista.page} totalPages={lista.totalPages} />
        )}
        <PanelAciertos aciertos={aciertos.aciertos} cargando={aciertos.cargando} error={aciertos.error} />
      </div>
    </section>
  )
}
