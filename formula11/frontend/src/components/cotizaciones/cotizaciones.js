// Códigos del contrato (CodigoEstrategia): A = RENDIMIENTO_POSICION, B = CONSISTENCIA.
export const ESTRATEGIAS = {
  RENDIMIENTO_POSICION: { nombre: 'Rendimiento por posición', color: '#ff8a1f' },
  CONSISTENCIA: { nombre: 'Consistencia', color: '#f4e9b8' },
}
const COLORES_EXTRA = ['#3ddc84', '#ff3b4e', '#b9d4b0']
const CODIGOS_ESTRATEGIA = Object.keys(ESTRATEGIAS)

const formatoPrecio = new Intl.NumberFormat('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
const formatoPorcentaje = new Intl.NumberFormat('es-AR', { minimumFractionDigits: 1, maximumFractionDigits: 1, signDisplay: 'exceptZero' })

export function nombreEstrategia(estrategia) {
  return ESTRATEGIAS[estrategia]?.nombre || estrategia
}

export function colorEstrategia(estrategia, indice = 0) {
  return ESTRATEGIAS[estrategia]?.color || COLORES_EXTRA[indice % COLORES_EXTRA.length]
}

// El contrato manda porcentajes (12.5 = 12,5 %); en el front se manejan como fracción (0.125).
export function porcentajeAFraccion(porcentaje) {
  return porcentaje == null ? null : Number(porcentaje) / 100
}

// HistorialCotizacionesResponse: { jugadorId, estrategia, cotizaciones: Cotizacion[] }, ordenadas por fecha ascendente.
export function normalizarCotizaciones(data) {
  return (data?.cotizaciones || [])
    .filter((cotizacion) => cotizacion?.fecha && Number.isFinite(Number(cotizacion.precio)))
    .map((cotizacion) => ({
      ...cotizacion,
      estrategia: cotizacion.estrategia || data.estrategia,
      precio: Number(cotizacion.precio),
      variacion: porcentajeAFraccion(cotizacion.variacion),
    }))
    .sort((a, b) => a.fecha.localeCompare(b.fecha))
}

export function obtenerEstrategiaActiva(estrategias) {
  const activas = (Array.isArray(estrategias) ? estrategias : []).filter((estrategia) => estrategia?.activa === true)
  if (activas.length !== 1 || !CODIGOS_ESTRATEGIA.includes(activas[0].codigo)) {
    const error = new Error('No se pudo identificar una única estrategia activa.')
    error.api = { mensaje: error.message }
    throw error
  }
  return activas[0].codigo
}

export function estrategiasPresentes(cotizaciones) {
  const presentes = [...new Set(cotizaciones.map((cotizacion) => cotizacion.estrategia))]
  const conocidas = Object.keys(ESTRATEGIAS)
  return presentes.sort((a, b) => {
    const ia = conocidas.indexOf(a)
    const ib = conocidas.indexOf(b)
    return (ia === -1 ? conocidas.length : ia) - (ib === -1 ? conocidas.length : ib) || a.localeCompare(b)
  })
}

// Una fila por fecha con el precio de cada estrategia como columna, el formato que espera el gráfico.
export function seriesPorFecha(cotizaciones) {
  const filas = new Map()
  for (const { fecha, estrategia, precio } of cotizaciones) {
    const fila = filas.get(fecha) || { fecha }
    fila[estrategia] = precio
    filas.set(fecha, fila)
  }
  return [...filas.values()].sort((a, b) => a.fecha.localeCompare(b.fecha))
}

export function resumenPorEstrategia(cotizaciones, estrategias = estrategiasPresentes(cotizaciones)) {
  return estrategias.map((estrategia) => {
    const propias = cotizaciones.filter((cotizacion) => cotizacion.estrategia === estrategia)
    const actual = propias.at(-1)
    return { estrategia, actual: actual || null, variacion: actual?.variacion ?? null }
  })
}

// Las fechas llegan como YYYY-MM-DD; se formatean sin pasar por Date para no correrlas de día por la zona horaria.
export function formatearFecha(fecha, { corta = false } = {}) {
  const [anio, mes, dia] = String(fecha).slice(0, 10).split('-')
  if (!anio || !mes || !dia) return String(fecha)
  return corta ? `${dia}/${mes}` : `${dia}/${mes}/${anio}`
}

export function formatearPrecio(precio) {
  return formatoPrecio.format(precio)
}

export function formatearConSigno(valor) {
  return `${valor > 0 ? '+' : ''}${formatoPrecio.format(valor)}`
}

export function formatearVariacion(variacion) {
  return `${formatoPorcentaje.format(variacion * 100)}%`
}
