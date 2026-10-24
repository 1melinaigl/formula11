// Colores validados contra el fondo oscuro de la cancha (#06210f) con el validador de paletas:
// banda de luminosidad, croma y separación para daltonismo.
export const COLORES = {
  valor: '#e07210',
  invertido: '#2690b0',
  ganancia: '#1c9c8c',
  perdida: '#e2414f',
}

const MAXIMO_COMPOSICION = 7

function numero(valor) {
  return valor == null || valor === '' ? null : Number(valor)
}

// Completa cada tenencia con los datos del jugador y calcula lo que el backend no mande.
export function enriquecerTenencias(tenencias) {
  return tenencias.map((tenencia) => {
    const jugador = tenencia.jugador || {}
    const cantidad = Number(tenencia.cantidad ?? 0)
    const precioPromedio = numero(tenencia.precioPromedio)
    const precioActual = numero(tenencia.precioActual ?? tenencia.cotizacionActual ?? jugador.precioActual)
    const invertido = numero(tenencia.invertido) ?? (precioPromedio == null ? null : cantidad * precioPromedio)
    const valorActual = numero(tenencia.valorActual) ?? (precioActual == null ? null : cantidad * precioActual)
    const ganancia = numero(tenencia.ganancia) ?? (valorActual == null || invertido == null ? null : valorActual - invertido)
    return {
      ...tenencia,
      jugadorId: tenencia.jugadorId ?? jugador.id,
      nombre: tenencia.nombre ?? jugador.nombre ?? `Jugador ${tenencia.jugadorId ?? jugador.id}`,
      equipo: tenencia.equipo ?? jugador.equipo,
      posicion: tenencia.posicion ?? jugador.posicion,
      cantidad,
      precioPromedio,
      precioActual,
      invertido,
      valorActual,
      ganancia,
      rendimiento: ganancia != null && invertido ? ganancia / invertido : null,
    }
  })
}

export function resumenPortfolio(tenencias, saldo) {
  const sumar = (campo) => tenencias.reduce((total, tenencia) => total + (tenencia[campo] ?? 0), 0)
  const invertido = sumar('invertido')
  const valorActual = sumar('valorActual')
  const ganancia = valorActual - invertido
  return {
    saldo,
    invertido,
    valorActual,
    ganancia,
    rendimiento: invertido ? ganancia / invertido : null,
    total: saldo + valorActual,
  }
}

export function normalizarEvolucion(data) {
  const lista = Array.isArray(data) ? data : data?.content || data?.puntos || []
  return lista
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
