export const endpoints = {
  registro: '/api/usuarios/registro',
  login: '/api/usuarios/login',
  jugadores: '/api/jugadores',
  ranking: '/api/jugadores/ranking',
  estrategias: '/api/estrategias',
  jugador: (id) => `/api/jugadores/${encodeURIComponent(id)}`,
  cotizacionesJugador: (id, estrategia) => `/api/jugadores/${encodeURIComponent(id)}/cotizaciones${estrategia ? `?estrategia=${estrategia}` : ''}`,
  mercadoJugador: (id) => `/api/jugadores/${encodeURIComponent(id)}/mercado`,
  comprar: '/api/ordenes/compra',
  vender: '/api/ordenes/venta',
  portfolio: (usuarioId) => `/api/usuarios/${encodeURIComponent(usuarioId)}/portfolio`,
}
