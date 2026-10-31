import { Link } from 'react-router-dom'
import PosicionJugador from '../catalogo/PosicionJugador'
import { formatearFecha, formatearPrecio } from '../cotizaciones/cotizaciones'
import { TIPOS_SENAL, formatearPorcentaje } from './senales'

// El botón de compra solo se muestra si puedeComprar (rol USUARIO). Mientras una compra está en curso
// se deshabilitan todos, para no mandar dos órdenes a la vez.
export default function TablaSenales({ senales, puedeComprar, enviandoId, comprar }) {
  return (
    // El contenedor scrollea si la tabla no entra: la página nunca supera el ancho de la ventana.
    <div className="signals-table-wrap">
    <table className="ranking-table signals-table">
      <caption className="visually-hidden">Señales de jugadores subvaluados</caption>
      <thead>
        <tr>
          <th scope="col">Jugador</th>
          <th className="signals-optional" scope="col">Equipo</th>
          <th className="signals-liga" scope="col">Liga</th>
          <th scope="col">Tipo</th>
          <th className="ranking-number" scope="col">Brecha</th>
          <th className="ranking-number" scope="col">Precio</th>
          <th className="ranking-number signals-optional" scope="col">Precio estimado</th>
          <th scope="col">Semana</th>
          {puedeComprar && <th scope="col"><span className="visually-hidden">Acciones</span></th>}
        </tr>
      </thead>
      <tbody>
        {senales.map((senal) => {
          const tipo = TIPOS_SENAL[senal.tipo]
          return (
            <tr key={senal.id}>
              <td>
                <span className="ranking-player">
                  <PosicionJugador jugador={senal.jugador} />
                  <Link to={`/jugadores/${senal.jugador.id}`}>{senal.jugador.nombre}</Link>
                </span>
              </td>
              <td className="ranking-team signals-optional">{senal.jugador.equipo}</td>
              <td className="signals-liga">{senal.jugador.liga}</td>
              <td><span className="signal-type" title={tipo.ayuda}>{tipo.nombre}</span><small className="signal-help">{tipo.ayuda}</small></td>
              <td className="ranking-number"><span className="quote-change up">{formatearPorcentaje(senal.brecha)}</span></td>
              <td className="ranking-number">{formatearPrecio(senal.precio)}</td>
              <td className="ranking-number signals-optional">{formatearPrecio(senal.precioEstimado)}</td>
              <td className="history-date">al {formatearFecha(senal.fecha)}</td>
              {puedeComprar && (
                <td>
                  <button
                    aria-label={`Comprar 1 token de ${senal.jugador.nombre}`}
                    className="btn ghost signal-buy"
                    disabled={enviandoId != null}
                    type="button"
                    onClick={() => comprar(senal)}
                  >
                    {enviandoId === senal.id ? 'Comprando...' : <>Comprar<span className="signal-buy-extra"> 1 token</span></>}
                  </button>
                </td>
              )}
            </tr>
          )
        })}
      </tbody>
    </table>
    </div>
  )
}
