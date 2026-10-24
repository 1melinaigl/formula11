import { formatearFecha } from '../cotizaciones/cotizaciones'

export const TAMANO_PAGINA_HISTORIAL = 10

export const TIPOS_OPERACION = {
  COMPRA: 'Compra',
  VENTA: 'Venta',
}

const formatoFechaHora = new Intl.DateTimeFormat('es-AR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit', hourCycle: 'h23' })

// Acepta transacciones planas o con el jugador anidado, en lista o paginadas.
export function normalizarHistorial(data) {
  const lista = Array.isArray(data) ? data : data?.content || data?.transacciones || []
  const operaciones = lista
    .filter((operacion) => operacion && TIPOS_OPERACION[String(operacion.tipo).toUpperCase()])
    .map((operacion) => {
      const jugador = operacion.jugador || {}
      const cantidad = Number(operacion.cantidad ?? 0)
      const precio = Number(operacion.precio ?? 0)
      return {
        ...operacion,
        tipo: String(operacion.tipo).toUpperCase(),
        jugadorId: operacion.jugadorId ?? jugador.id,
        nombre: operacion.nombre ?? jugador.nombre ?? `Jugador ${operacion.jugadorId ?? jugador.id}`,
        posicion: operacion.posicion ?? jugador.posicion,
        cantidad,
        precio,
        total: operacion.total == null ? cantidad * precio : Number(operacion.total),
      }
    })
  return {
    operaciones,
    page: Array.isArray(data) ? 0 : Number(data?.page ?? 0),
    totalPages: Array.isArray(data) ? (operaciones.length ? 1 : 0) : Number(data?.totalPages ?? (operaciones.length ? 1 : 0)),
  }
}

// Las fechas sin hora (YYYY-MM-DD) se muestran tal cual; los instantes, en la hora local.
export function formatearFechaHora(fecha) {
  if (!fecha) return '—'
  if (/^\d{4}-\d{2}-\d{2}$/.test(fecha)) return formatearFecha(fecha)
  const instante = new Date(fecha)
  return Number.isNaN(instante.getTime()) ? String(fecha) : formatoFechaHora.format(instante)
}

// Efecto de la operación sobre el saldo: una compra resta créditos y una venta los suma.
export function efectoEnSaldo(operacion) {
  return operacion.tipo === 'COMPRA' ? -operacion.total : operacion.total
}

export function construirConsultaHistorial({ page, tipo }) {
  const params = new URLSearchParams({ page: String(page), size: String(TAMANO_PAGINA_HISTORIAL), sort: 'fecha,desc' })
  if (tipo) params.set('tipo', tipo)
  return params.toString()
}
