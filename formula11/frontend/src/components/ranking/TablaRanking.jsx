import { Link } from 'react-router-dom'
import PosicionJugador from '../catalogo/PosicionJugador'
import { formatearPrecio, formatearVariacion } from '../cotizaciones/cotizaciones'

function Variacion({ variacion }) {
  if (variacion === null) return <span className="quote-change flat">—</span>
  const clase = variacion === 0 ? 'flat' : variacion > 0 ? 'up' : 'down'
  return <span className={`quote-change ${clase}`}>{formatearVariacion(variacion)}</span>
}

export default function TablaRanking({ ranking }) {
  return (
    <table className="ranking-table">
      <thead>
        <tr>
          <th scope="col">#</th>
          <th scope="col">Jugador</th>
          <th className="ranking-team" scope="col">Equipo</th>
          <th className="ranking-number" scope="col">Precio</th>
          <th className="ranking-number" scope="col">Variación</th>
          <th className="ranking-number ranking-score" scope="col">Score</th>
        </tr>
      </thead>
      <tbody>
        {ranking.map((fila) => (
          <tr key={fila.id}>
            <td className="ranking-position">{fila.puesto}</td>
            <td>
              <span className="ranking-player">
                <PosicionJugador jugador={fila} />
                <Link to={`/jugadores/${fila.id}`}>{fila.nombre}</Link>
              </span>
            </td>
            <td className="ranking-team">{fila.equipo}</td>
            <td className="ranking-number">{formatearPrecio(fila.precio)}</td>
            <td className="ranking-number"><Variacion variacion={fila.variacion} /></td>
            <td className="ranking-number ranking-score">{fila.score == null ? '—' : Number(fila.score).toFixed(2)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
