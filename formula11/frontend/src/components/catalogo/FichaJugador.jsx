import PosicionJugador from './PosicionJugador'
import { posiciones } from './posiciones'

export default function FichaJugador({ jugador, volver }) {
  if (!jugador) return <div className="catalog-empty">Seleccioná un jugador para ver su ficha.</div>
  return (
    <article className="player-card">
      <div className="player-card-heading"><span>Ficha del jugador</span><PosicionJugador jugador={jugador} /></div>
      <h2>{jugador.nombre}</h2>
      <dl className="player-details">
        <dt>ID</dt><dd>#{jugador.id}</dd>
        <dt>Equipo</dt><dd>{jugador.equipo}</dd>
        <dt>Liga</dt><dd>{jugador.liga}</dd>
        <dt>Posición</dt><dd>{jugador.posicionNombre || posiciones[jugador.posicion] || jugador.posicion || 'No informada'}</dd>
      </dl>
      <button className="btn ghost back-to-list" type="button" onClick={volver}>Volver a la lista</button>
    </article>
  )
}