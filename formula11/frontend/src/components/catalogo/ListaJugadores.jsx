import PosicionJugador from './PosicionJugador'

export default function ListaJugadores({ jugadores, seleccionado, seleccionar }) {
  return (
    <div className="player-list" aria-label="Lista de jugadores">
      {jugadores.map((jugador) => (
        <button className={`player-row ${seleccionado?.id === jugador.id ? 'selected' : ''}`} key={jugador.id} type="button" onClick={() => seleccionar(jugador)}>
          <PosicionJugador jugador={jugador} />
          <span className="player-name">{jugador.nombre}</span>
          <span className="player-team">{jugador.equipo}</span>
        </button>
      ))}
    </div>
  )
}