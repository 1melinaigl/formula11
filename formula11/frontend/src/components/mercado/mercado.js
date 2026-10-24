import { formatearPrecio } from '../cotizaciones/cotizaciones'

export const OPERACIONES = {
  COMPRA: { verbo: 'Comprar', pasado: 'Compraste' },
  VENTA: { verbo: 'Vender', pasado: 'Vendiste' },
}

// OrdenRequest del contrato: cantidad entre 1 y 100 (cada jugador emite 100 tokens).
export const MAXIMO_TOKENS_POR_ORDEN = 100

// PortfolioResponse: { saldo, posiciones: [{ jugador, cantidad, precioPromedio, ... }] }.
export function normalizarPortfolio(data) {
  return {
    saldo: Number(data?.saldo ?? 0),
    tenencias: (data?.posiciones || []).map((posicion) => ({
      ...posicion,
      jugadorId: posicion.jugador?.id,
      cantidad: Number(posicion.cantidad ?? 0),
      precioPromedio: posicion.precioPromedio == null ? null : Number(posicion.precioPromedio),
    })),
  }
}

export function tenenciaDe(portfolio, jugadorId) {
  return portfolio.tenencias.find((tenencia) => String(tenencia.jugadorId) === String(jugadorId)) || null
}

// MercadoJugadorResponse: precio vigente según la estrategia activa y tokens que le quedan al superusuario.
// fechaCotizacion es null mientras no haya recálculo (precio inicial de 1 crédito).
export function normalizarMercado(data) {
  return {
    precio: data?.precioVigente == null ? null : Number(data.precioVigente),
    disponibles: data?.tokensDisponibles == null ? null : Number(data.tokensDisponibles),
    emitidos: data?.tokensEmitidos == null ? null : Number(data.tokensEmitidos),
    fechaCotizacion: data?.fechaCotizacion ?? null,
  }
}

export function validarOperacion({ tipo, cantidad, precio, saldo, tenencia = 0, disponibles = null }) {
  if (cantidad === '') return 'Ingresá una cantidad de tokens.'
  const numero = Number(cantidad)
  if (!Number.isInteger(numero) || numero < 1) return 'La cantidad debe ser un número entero mayor a 0.'
  if (numero > MAXIMO_TOKENS_POR_ORDEN) return `Podés operar hasta ${MAXIMO_TOKENS_POR_ORDEN} tokens por orden.`
  if (tipo === 'VENTA' && numero > tenencia) {
    return tenencia === 0 ? 'No tenés tokens de este jugador para vender.' : `Solo tenés ${tenencia} tokens para vender.`
  }
  if (tipo === 'COMPRA' && disponibles != null && numero > disponibles) return `Solo quedan ${disponibles} tokens disponibles.`
  if (tipo === 'COMPRA' && numero * precio > saldo) return `Saldo insuficiente: necesitás ${formatearPrecio(numero * precio)} créditos.`
  return null
}

// OperacionResponse: { tipo, cantidad, precioUnitario, total, saldoPosterior, ... }.
export function describirResultado(orden) {
  const cantidad = Number(orden.cantidad)
  const tokens = cantidad === 1 ? 'token' : 'tokens'
  return `${OPERACIONES[orden.tipo].pasado} ${cantidad} ${tokens} a ${formatearPrecio(Number(orden.precioUnitario))} créditos. `
    + `Total: ${formatearPrecio(Number(orden.total))} créditos. Saldo: ${formatearPrecio(Number(orden.saldoPosterior))} créditos.`
}
