export const posiciones = {
  POR: 'Portero',
  DEF: 'Defensor',
  MED: 'Mediocampista',
  DEL: 'Delantero',
}

export function obtenerPosicionCodigo(jugador) {
  return jugador.posicionCodigo || jugador.posicion || 'ND'
}