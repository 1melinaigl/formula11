import { formatearPrecio } from '../cotizaciones/cotizaciones'

export const OPERACIONES = {
  COMPRA: { verbo: 'Comprar', pasado: 'Compraste' },
  VENTA: { verbo: 'Vender', pasado: 'Vendiste' },
}

// El mercado opera con la estrategia activa; la A (RENDIMIENTO) es la activa por defecto.
const ESTRATEGIA_POR_DEFECTO = 'RENDIMIENTO'

export function normalizarPortfolio(data) {
  const lista = data?.tenencias || data?.posiciones || data?.content || []
  return {
    saldo: Number(data?.saldo ?? 0),
    tenencias: lista.map((tenencia) => ({
      ...tenencia,
      jugadorId: tenencia.jugadorId ?? tenencia.jugador?.id,
      cantidad: Number(tenencia.cantidad ?? 0),
      precioPromedio: tenencia.precioPromedio == null ? null : Number(tenencia.precioPromedio),
    })),
  }
}

export function tenenciaDe(portfolio, jugadorId) {
  return portfolio.tenencias.find((tenencia) => String(tenencia.jugadorId) === String(jugadorId)) || null
}

// Precio que se muestra como referencia; el precio final lo fija el backend al ejecutar la orden.
export function precioVigente(jugador, cotizaciones) {
  const directo = jugador?.precioActual ?? jugador?.cotizacionActual?.precio
  if (directo != null && Number.isFinite(Number(directo))) return Number(directo)
  const activa = cotizaciones.filter((cotizacion) => cotizacion.estrategia === ESTRATEGIA_POR_DEFECTO)
  return (activa.at(-1) || cotizaciones.at(-1))?.precio ?? null
}

export function validarOperacion({ tipo, cantidad, precio, saldo, tenencia = 0, disponibles = null }) {
  if (cantidad === '') return 'Ingresá una cantidad de tokens.'
  const numero = Number(cantidad)
  if (!Number.isInteger(numero) || numero < 1) return 'La cantidad debe ser un número entero mayor a 0.'
  if (tipo === 'VENTA' && numero > tenencia) {
    return tenencia === 0 ? 'No tenés tokens de este jugador para vender.' : `Solo tenés ${tenencia} tokens para vender.`
  }
  if (tipo === 'COMPRA' && disponibles != null && numero > disponibles) return `Solo quedan ${disponibles} tokens disponibles.`
  if (tipo === 'COMPRA' && numero * precio > saldo) return `Saldo insuficiente: necesitás ${formatearPrecio(numero * precio)} créditos.`
  return null
}

export function describirResultado(tipo, orden) {
  const cantidad = Number(orden.cantidad)
  const precio = Number(orden.precio)
  const total = orden.total == null ? cantidad * precio : Number(orden.total)
  const tokens = cantidad === 1 ? 'token' : 'tokens'
  return `${OPERACIONES[tipo].pasado} ${cantidad} ${tokens} a ${formatearPrecio(precio)} créditos. Total: ${formatearPrecio(total)} créditos.`
}
