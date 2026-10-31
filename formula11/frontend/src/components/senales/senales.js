export const TAMANO_PAGINA_SENALES = 20
export const TAMANO_CONSULTA_ALERTAS = 100
export const INTERVALO_ALERTAS_MS = 30000
export const MAXIMO_VISTAS = 500

export const TIPOS_SENAL = {
  ENTRE_ESTRATEGIAS: { nombre: 'Entre estrategias', ayuda: 'Otra estrategia lo valora más' },
  PRECIO_ATRASADO: { nombre: 'Precio atrasado', ayuda: 'Jugadores con rendimiento parecido valen más' },
}

const formatoPorcentaje = new Intl.NumberFormat('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2, signDisplay: 'exceptZero' })
const formatoPorcentajeSinSigno = new Intl.NumberFormat('es-AR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

// SenalesPageResponse: página de Senal { id, jugador, fecha, tipo, brecha, precio, precioEstimado }.
// El orden lo fija el backend (fecha y brecha descendentes) y no se cambia.
export function normalizarSenales(data) {
  return {
    senales: (data?.content || [])
      .filter((senal) => senal?.id != null && senal.jugador && TIPOS_SENAL[senal.tipo])
      .map((senal) => ({
        ...senal,
        brecha: Number(senal.brecha),
        precio: Number(senal.precio),
        precioEstimado: Number(senal.precioEstimado),
      })),
    page: Number(data?.page ?? 0),
    totalPages: Number(data?.totalPages ?? 0),
    totalElements: Number(data?.totalElements ?? 0),
  }
}

// AciertosResponse { semanas: [{ fecha, senales, variacionSenalados, variacionMercado, aciertos }], tasaAcierto, diferenciaPromedio }.
// Las semanas llegan por fecha ascendente y se muestran así.
export function normalizarAciertos(data) {
  return {
    semanas: (data?.semanas || []).map((semana) => ({
      fecha: semana.fecha,
      senales: Number(semana.senales),
      variacionSenalados: Number(semana.variacionSenalados),
      variacionMercado: Number(semana.variacionMercado),
      aciertos: Number(semana.aciertos),
    })),
    tasaAcierto: Number(data?.tasaAcierto ?? 0),
    diferenciaPromedio: Number(data?.diferenciaPromedio ?? 0),
  }
}

// El contrato manda porcentajes (18 = 18 %); se muestran sin convertir, con dos decimales.
export function formatearPorcentaje(porcentaje, { signo = true } = {}) {
  return `${(signo ? formatoPorcentaje : formatoPorcentajeSinSigno).format(Number(porcentaje))}%`
}

export function construirConsultaSenales({ vigentes, page, size }) {
  return new URLSearchParams({ vigentes: String(vigentes), page: String(page), size: String(size) }).toString()
}

export function senalesNuevas(senales, vistas) {
  const yaVistas = new Set(vistas)
  return senales.filter((senal) => !yaVistas.has(senal.id))
}

// Señales vistas: por usuario y por pestaña, con la misma estrategia de almacenamiento que la sesión.
function claveVistas(usuarioId) {
  return `formula11.senales.vistas.${usuarioId}`
}

export function leerVistas(usuarioId) {
  try {
    const vistas = JSON.parse(sessionStorage.getItem(claveVistas(usuarioId)))
    return Array.isArray(vistas) ? vistas : []
  } catch {
    return []
  }
}

export function agregarVistas(usuarioId, ids) {
  const vistas = leerVistas(usuarioId)
  const nuevas = ids.filter((id) => !vistas.includes(id))
  if (nuevas.length === 0) return
  // Se descartan las más viejas para que el registro no crezca sin límite.
  sessionStorage.setItem(claveVistas(usuarioId), JSON.stringify([...vistas, ...nuevas].slice(-MAXIMO_VISTAS)))
}

export function borrarVistas(usuarioId) {
  sessionStorage.removeItem(claveVistas(usuarioId))
}
