export const endpoints = {
  registro: '/api/usuarios/registro',
  login: '/api/usuarios/login',
  jugadores: '/api/jugadores',
  jugador: (id) => `/api/jugadores/${encodeURIComponent(id)}`,
  cotizacionesJugador: (id) => `/api/jugadores/${encodeURIComponent(id)}/cotizaciones`,
}
