import { porcentajeAFraccion } from '../cotizaciones/cotizaciones'

// Colores validados contra el fondo oscuro de la cancha (#06210f) con el validador de paletas:
// banda de luminosidad, croma y separación para daltonismo.
export const COLORES = {
  valor: '#e07210',
  invertido: '#2690b0',
  ganancia: '#1c9c8c',
  perdida: '#e2414f',
}

const MAXIMO_COMPOSICION = 7

// PosicionPortfolio del contrato: { jugador, cantidad, precioPromedio, precioActual, invertido, valorActual,
// ganancia, gananciaPorcentaje (%) }, ya valuada por el backend con la cotización vigente.
export function enriquecerTenencias(posiciones) {
  return posiciones.map(({ jugador = {}, ...posicion }) => ({
    ...jugador,
    jugadorId: jugador.id,
    cantidad: Number(posicion.cantidad),
    precioPromedio: Number(posicion.precioPromedio),
    precioActual: Number(posicion.precioActual),
    invertido: Number(posicion.invertido),
    valorActual: Number(posicion.valorActual),
    ganancia: Number(posicion.ganancia),
    rendimiento: porcentajeAFraccion(posicion.gananciaPorcentaje),
  }))
}

// Totales de PortfolioResponse; el valor total suma el saldo disponible y el valor de las tenencias.
export function resumenPortfolio(data) {
  const saldo = Number(data?.saldo ?? 0)
  const invertido = Number(data?.invertido ?? 0)
  const valorActual = Number(data?.valorTenencias ?? 0)
  return {
    saldo,
    invertido,
    valorActual,
    ganancia: Number(data?.ganancia ?? 0),
    rendimiento: invertido > 0 ? porcentajeAFraccion(data.gananciaPorcentaje) : null,
    total: saldo + valorActual,
    fechaValuacion: data?.fechaValuacion ?? null,
  }
}

// EvolucionPortfolioResponse: { usuarioId, puntos: [{ fecha, valor, invertido }] }.
export function normalizarEvolucion(data) {
  return (data?.puntos || [])
    .filter((punto) => punto?.fecha && Number.isFinite(Number(punto.valor)))
    .map((punto) => ({ fecha: punto.fecha, valor: Number(punto.valor), invertido: Number(punto.invertido ?? 0) }))
    .sort((a, b) => a.fecha.localeCompare(b.fecha))
}

// Ordenada de mayor a menor valor; a partir del 8.º jugador se agrupa en "Otros".
export function composicion(tenencias) {
  const conValor = tenencias.filter((tenencia) => tenencia.valorActual > 0)
  const total = conValor.reduce((suma, tenencia) => suma + tenencia.valorActual, 0)
  if (!total) return []
  const ordenadas = [...conValor].sort((a, b) => b.valorActual - a.valorActual)
  const principales = ordenadas.slice(0, MAXIMO_COMPOSICION).map((tenencia) => ({ nombre: tenencia.nombre, valor: tenencia.valorActual }))
  const resto = ordenadas.slice(MAXIMO_COMPOSICION)
  if (resto.length) principales.push({ nombre: `Otros (${resto.length})`, valor: resto.reduce((suma, tenencia) => suma + tenencia.valorActual, 0) })
  return principales.map((fila) => ({ ...fila, porcentaje: fila.valor / total }))
}

export function gananciasPorJugador(tenencias) {
  return tenencias
    .filter((tenencia) => tenencia.ganancia != null)
    .map((tenencia) => ({ nombre: tenencia.nombre, ganancia: tenencia.ganancia }))
    .sort((a, b) => b.ganancia - a.ganancia)
}
