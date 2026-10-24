export const ESTRATEGIAS = {
  RENDIMIENTO: { nombre: 'Rendimiento por posición', color: '#ff8a1f' },
  CONSISTENCIA: { nombre: 'Consistencia', color: '#f4e9b8' },
}
const COLORES_EXTRA = ['#3ddc84', '#ff3b4e', '#b9d4b0']

const formatoPrecio = new Intl.NumberFormat('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
const formatoPorcentaje = new Intl.NumberFormat('es-AR', { minimumFractionDigits: 1, maximumFractionDigits: 1, signDisplay: 'exceptZero' })

export function nombreEstrategia(estrategia) {
  return ESTRATEGIAS[estrategia]?.nombre || estrategia
}

export function colorEstrategia(estrategia, indice = 0) {
  return ESTRATEGIAS[estrategia]?.color || COLORES_EXTRA[indice % COLORES_EXTRA.length]
}

// Acepta una lista o una respuesta paginada y devuelve las cotizaciones ordenadas por fecha ascendente.
export function normalizarCotizaciones(data) {
  const lista = Array.isArray(data) ? data : data?.content || data?.cotizaciones || []
  return lista
    .filter((cotizacion) => cotizacion?.fecha && Number.isFinite(Number(cotizacion.precio)))
    .map((cotizacion) => ({ ...cotizacion, precio: Number(cotizacion.precio), estrategia: cotizacion.estrategia || 'SIN_ESTRATEGIA' }))
    .sort((a, b) => a.fecha.localeCompare(b.fecha))
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

export function resumenPorEstrategia(cotizaciones) {
  return estrategiasPresentes(cotizaciones).map((estrategia) => {
    const propias = cotizaciones.filter((cotizacion) => cotizacion.estrategia === estrategia)
    const actual = propias.at(-1)
    const anterior = propias.at(-2)
    const variacion = anterior && anterior.precio !== 0 ? (actual.precio - anterior.precio) / anterior.precio : null
    return { estrategia, actual, anterior, variacion }
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

export function formatearVariacion(variacion) {
  return `${formatoPorcentaje.format(variacion * 100)}%`
}
