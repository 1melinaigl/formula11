import { Link } from 'react-router-dom'
import PosicionJugador from '../catalogo/PosicionJugador'
import { formatearConSigno, formatearPrecio, formatearVariacion } from '../cotizaciones/cotizaciones'

function Monto({ valor }) {
  return valor == null ? '—' : formatearPrecio(valor)
}

function Ganancia({ ganancia, rendimiento }) {
  if (ganancia == null) return <span className="quote-change flat">—</span>
  const clase = ganancia === 0 ? 'flat' : ganancia > 0 ? 'up' : 'down'
  return (
    <span className={`quote-change ${clase}`}>
      {formatearConSigno(ganancia)}{rendimiento != null && <small> ({formatearVariacion(rendimiento)})</small>}
    </span>
  )
}

export default function TablaTenencias({ tenencias }) {
  return (
    <table className="ranking-table portfolio-table">
      <caption className="visually-hidden">Tokens por jugador</caption>
      <thead>
        <tr>
          <th scope="col">Jugador</th>
          <th className="ranking-number" scope="col">Tokens</th>
          <th className="ranking-number portfolio-optional" scope="col">Precio prom.</th>
          <th className="ranking-number portfolio-optional" scope="col">Precio actual</th>
          <th className="ranking-number" scope="col">Valor</th>
          <th className="ranking-number" scope="col">Ganancia</th>
          <th scope="col"><span className="visually-hidden">Acciones</span></th>
        </tr>
      </thead>
      <tbody>
        {tenencias.map((tenencia) => (
          <tr key={tenencia.jugadorId}>
            <td>
              <span className="ranking-player">
                <PosicionJugador jugador={tenencia} />
                <Link to={`/jugadores/${tenencia.jugadorId}`}>{tenencia.nombre}</Link>
              </span>
            </td>
            <td className="ranking-number">{tenencia.cantidad}</td>
            <td className="ranking-number portfolio-optional"><Monto valor={tenencia.precioPromedio} /></td>
            <td className="ranking-number portfolio-optional"><Monto valor={tenencia.precioActual} /></td>
            <td className="ranking-number"><Monto valor={tenencia.valorActual} /></td>
            <td className="ranking-number"><Ganancia ganancia={tenencia.ganancia} rendimiento={tenencia.rendimiento} /></td>
            <td>
              <Link aria-label={`Vender tokens de ${tenencia.nombre}`} className="filter-chip portfolio-sell" to={`/jugadores/${tenencia.jugadorId}?operacion=venta`}>Vender</Link>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
