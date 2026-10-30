import { porcentajeAFraccion } from '../cotizaciones/cotizaciones'

export const TAMANO_PAGINA_RANKING = 20

// RankingPageResponse: página con estrategia, versión y fecha de la última cotización,
// y filas { puesto, jugador, precio, variacion (%), score } ya ordenadas por el backend.
export function normalizarRanking(data) {
  return {
    filas: (data?.content || [])
      .filter((fila) => fila?.jugador?.id != null && Number.isFinite(Number(fila.precio)))
      .map(({ jugador, ...fila }) => ({
        ...jugador,
        puesto: fila.puesto,
        precio: Number(fila.precio),
        variacion: porcentajeAFraccion(fila.variacion),
        score: fila.score,
      })),
    estrategia: data?.estrategia ?? null,
    version: data?.version ?? null,
    fecha: data?.fecha ?? null,
    page: Number(data?.page ?? 0),
    totalPages: Number(data?.totalPages ?? 0),
  }
}

export function construirConsultaRanking({ page, posicion, liga }) {
  const params = new URLSearchParams({ page: String(page), size: String(TAMANO_PAGINA_RANKING) })
  if (posicion) params.set('posicion', posicion)
  if (liga) params.set('liga', liga)
  return params.toString()
}
