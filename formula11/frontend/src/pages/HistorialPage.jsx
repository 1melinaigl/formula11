import { useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import PaginacionCatalogo from '../components/catalogo/PaginacionCatalogo'
import AvisoError from '../components/comun/AvisoError'
import EstadoCarga from '../components/comun/EstadoCarga'
import TablaHistorial from '../components/historial/TablaHistorial'
import { useHistorial } from '../hooks/useHistorial'
import { useSesion } from '../hooks/useSesion'
import AccesoDenegadoPage from './AccesoDenegadoPage'

const FILTROS = [['', 'Todas'], ['COMPRA', 'Compras'], ['VENTA', 'Ventas']]
const VACIOS = {
  '': 'Todavía no hiciste operaciones.',
  COMPRA: 'Todavía no hiciste compras.',
  VENTA: 'Todavía no hiciste ventas.',
}

function ContenidoHistorial({ operaciones, tipo }) {
  if (operaciones.length === 0) {
    return (
      <div className="empty-state" role="status">
        <h2>Sin operaciones</h2>
        <p>{VACIOS[tipo]} Podés comprar tokens desde el <Link to="/ranking">ranking</Link> o el <Link to="/catalogo">catálogo</Link>.</p>
      </div>
    )
  }
  return <TablaHistorial operaciones={operaciones} />
}

export default function HistorialPage() {
  const { usuario } = useSesion()
  const navigate = useNavigate()
  const esSuperusuario = usuario?.rol === 'SUPERUSUARIO'
  const historial = useHistorial({ habilitado: !esSuperusuario })

  useEffect(() => {
    const volver = (event) => { if (event.key === 'Escape') navigate('/portfolio') }
    window.addEventListener('keydown', volver)
    return () => window.removeEventListener('keydown', volver)
  }, [navigate])

  if (!usuario) return <AccesoDenegadoPage error={historial.error} />

  return (
    <section className="screen catalog-screen">
      <div className="catalog-content">
        <div className="catalog-title-row">
          <div><p className="eyebrow"></p><h1 className="panel-heading">Historial de operaciones</h1></div>
          <div className="title-actions">
            <button className="btn ghost" type="button" onClick={() => navigate('/portfolio')}>Mi portfolio</button>
            <button className="btn ghost" type="button" onClick={() => navigate('/')}>Menú</button>
          </div>
        </div>
        {esSuperusuario ? (
          <div className="empty-state" role="status">
            <h2>Sin operaciones propias</h2>
            <p>El superusuario es la contraparte del mercado y no tiene operaciones propias.</p>
          </div>
        ) : (
          <>
          <div className="ranking-filters" role="group" aria-label="Filtrar por tipo de operación">
            {FILTROS.map(([codigo, nombre]) => (
              <button aria-pressed={historial.tipo === codigo} className="filter-chip" key={codigo || 'todas'} type="button" onClick={() => historial.cambiarTipo(codigo)}>{nombre}</button>
            ))}
          </div>
          {historial.cargando && <EstadoCarga mensaje="Cargando operaciones..." />}
          {!historial.cargando && historial.error && <AvisoError error={historial.error} />}
          {!historial.cargando && !historial.error && <ContenidoHistorial operaciones={historial.operaciones} tipo={historial.tipo} />}
          {!historial.cargando && !historial.error && historial.totalPages > 1 && (
            <PaginacionCatalogo cambiarPagina={historial.cambiarPagina} etiqueta="Paginación del historial" page={historial.page} totalPages={historial.totalPages} />
          )}
          </>
        )}
      </div>
    </section>
  )
}
