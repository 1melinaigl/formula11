import { useState } from 'react'
import { endpoints } from '../../api/endpoints'
import { useSesion } from '../../hooks/useSesion'
import AvisoError from '../comun/AvisoError'
import { formatearFecha, formatearPrecio, formatearVariacion, nombreEstrategia, porcentajeAFraccion } from './cotizaciones'

// Cotización de un jugador a una fecha dada: GET /api/jugadores/{id}/cotizaciones/vigente?fecha=AAAA-MM-DD.
// El backend devuelve la última cotización con fecha menor o igual (las cotizaciones son semanales, de domingo).
export default function CotizacionAFecha({ jugadorId }) {
  const { solicitarProtegida } = useSesion()
  const [fecha, setFecha] = useState('')
  const [estado, setEstado] = useState({ cotizacion: null, error: null, cargando: false })

  async function consultar(event) {
    event.preventDefault()
    if (!fecha) return
    setEstado({ cotizacion: null, error: null, cargando: true })
    try {
      const cotizacion = await solicitarProtegida(endpoints.cotizacionAFecha(jugadorId, fecha))
      setEstado({ cotizacion, error: null, cargando: false })
    } catch (error) {
      setEstado({ cotizacion: null, error, cargando: false })
    }
  }

  const { cotizacion, error, cargando } = estado
  const variacion = porcentajeAFraccion(cotizacion?.variacion)

  return (
    <form className="quote-at-date" aria-label="Consulta de cotización a una fecha" onSubmit={consultar}>
      <div className="catalog-search">
        <label htmlFor="cotizacion-fecha">Cotización a una fecha</label>
        <input id="cotizacion-fecha" type="date" value={fecha} onChange={(event) => setFecha(event.target.value)} required />
      </div>
      <button className="btn" type="submit" disabled={!fecha || cargando}>{cargando ? 'Consultando...' : 'Consultar'}</button>
      <AvisoError error={error} />
      {cotizacion && (
        <p className="quote-at-date-result" aria-live="polite">
          Al {formatearFecha(fecha)} valía <strong>{formatearPrecio(cotizacion.precio)} créditos</strong>
          {' '}(cotización del {formatearFecha(cotizacion.fecha)}, {nombreEstrategia(cotizacion.estrategia)} v{cotizacion.version}
          {variacion === null ? '' : `, ${formatearVariacion(variacion)} vs. semana anterior`})
        </p>
      )}
    </form>
  )
}
