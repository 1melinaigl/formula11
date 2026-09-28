import { obtenerPosicionCodigo } from './posiciones'
import { posiciones } from './posiciones'

export default function PosicionJugador({ jugador }) {
  const codigo = obtenerPosicionCodigo(jugador)
  return <span className={`player-position position-${posiciones[codigo] ? codigo : 'unknown'}`}>{codigo}</span>
}

