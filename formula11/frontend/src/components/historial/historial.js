import { formatearFecha } from '../cotizaciones/cotizaciones'

export const TAMANO_PAGINA_HISTORIAL = 10

export const TIPOS_OPERACION = {
  COMPRA: 'Compra',
  VENTA: 'Venta',
}

const formatoFechaHora = new Intl.DateTimeFormat('es-AR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit', hourCycle: 'h23' })

// TransaccionesPageResponse: página de OperacionResponse
// { id, tipo, jugador, cantidad, precioUnitario, total, saldoPosterior, contraparte, fecha, estrategia, version }.
export function normalizarHistorial(data) {
  return {
    operaciones: (data?.content || [])
      .filter((operacion) => TIPOS_OPERACION[operacion?.tipo])
      .map(({ jugador = {}, ...operacion }) => ({
        ...operacion,
        jugadorId: jugador.id,
        nombre: jugador.nombre,
        posicion: jugador.posicion,
        cantidad: Number(operacion.cantidad),
        precioUnitario: Number(operacion.precioUnitario),
        total: Number(operacion.total),
        saldoPosterior: Number(operacion.saldoPosterior),
      })),
    page: Number(data?.page ?? 0),
    totalPages: Number(data?.totalPages ?? 0),
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

// El backend ya ordena de la más nueva a la más vieja; tipo y jugadorId son filtros opcionales.
export function construirConsultaHistorial({ page, tipo, jugadorId }) {
  const params = new URLSearchParams({ page: String(page), size: String(TAMANO_PAGINA_HISTORIAL) })
  if (tipo) params.set('tipo', tipo)
  if (jugadorId != null) params.set('jugadorId', String(jugadorId))
  return params.toString()
}
