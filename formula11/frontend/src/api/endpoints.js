export const endpoints = {
  registro: '/api/usuarios/registro',
  login: '/api/usuarios/login',
  jugadores: '/api/jugadores',
  ranking: '/api/jugadores/ranking',
  estrategias: '/api/estrategias',
  activarEstrategia: (codigo) => `/api/estrategias/${encodeURIComponent(codigo)}/activar`,
  jugador: (id) => `/api/jugadores/${encodeURIComponent(id)}`,
  cotizacionesJugador: (id, { estrategia, page = 0, size } = {}) => {
    const params = new URLSearchParams({ page: String(page) })
    if (estrategia) params.set('estrategia', estrategia)
    if (size) params.set('size', String(size))
    return `/api/jugadores/${encodeURIComponent(id)}/cotizaciones?${params.toString()}`
  },
  cotizacionAFecha: (id, fecha) => `/api/jugadores/${encodeURIComponent(id)}/cotizaciones/vigente?fecha=${encodeURIComponent(fecha)}`,
  mercadoJugador: (id) => `/api/jugadores/${encodeURIComponent(id)}/mercado`,
  comprar: '/api/ordenes/compra',
  vender: '/api/ordenes/venta',
  portfolio: (usuarioId) => `/api/usuarios/${encodeURIComponent(usuarioId)}/portfolio`,
  transacciones: (usuarioId) => `/api/usuarios/${encodeURIComponent(usuarioId)}/transacciones`,
  evolucionPortfolio: (usuarioId) => `/api/usuarios/${encodeURIComponent(usuarioId)}/portfolio/evolucion`,
  senales: '/api/senales',
  aciertos: '/api/senales/aciertos',
}
