import { Link } from 'react-router-dom'
import PosicionJugador from '../catalogo/PosicionJugador'
import { formatearConSigno, formatearPrecio } from '../cotizaciones/cotizaciones'
import { TIPOS_OPERACION, efectoEnSaldo, formatearFechaHora } from './historial'

export default function TablaHistorial({ operaciones }) {
  return (
    <table className="ranking-table history-table">
      <caption className="visually-hidden">Historial de operaciones</caption>
      <thead>
        <tr>
          <th scope="col">Fecha</th>
          <th scope="col">Operación</th>
          <th scope="col">Jugador</th>
          <th className="ranking-number" scope="col">Tokens</th>
          <th className="ranking-number history-optional" scope="col">Precio</th>
          <th className="ranking-number" scope="col">Saldo</th>
          <th className="history-optional" scope="col">Contraparte</th>
        </tr>
      </thead>
      <tbody>
        {operaciones.map((operacion, indice) => {
          const efecto = efectoEnSaldo(operacion)
          return (
            <tr key={operacion.id ?? `${operacion.fecha}-${indice}`}>
              <td className="history-date">{formatearFechaHora(operacion.fecha)}</td>
              <td><span className={`history-type ${operacion.tipo.toLowerCase()}`}>{TIPOS_OPERACION[operacion.tipo]}</span></td>
              <td>
                <span className="ranking-player">
                  <PosicionJugador jugador={operacion} />
                  {operacion.jugadorId == null ? operacion.nombre : <Link to={`/jugadores/${operacion.jugadorId}`}>{operacion.nombre}</Link>}
                </span>
              </td>
              <td className="ranking-number">{operacion.cantidad}</td>
              <td className="ranking-number history-optional">{formatearPrecio(operacion.precio)}</td>
              <td className="ranking-number"><span className={`quote-change ${efecto < 0 ? 'down' : 'up'}`}>{formatearConSigno(efecto)}</span></td>
              <td className="history-optional">{operacion.contraparte || '—'}</td>
            </tr>
          )
        })}
      </tbody>
    </table>
  )
}
