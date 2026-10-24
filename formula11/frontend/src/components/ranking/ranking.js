// Acepta filas planas ({ id, nombre, ..., precio }) o anidadas ({ jugador: {...}, precio }), en lista o paginadas.
// Conserva el orden del backend, que ya viene rankeado según la estrategia activa.
export function normalizarRanking(data) {
  const lista = Array.isArray(data) ? data : data?.content || data?.ranking || []
  return lista
    .map((fila) => {
      const { jugador, ...cotizacion } = fila || {}
      const plana = { ...jugador, ...cotizacion, id: jugador?.id ?? cotizacion.id }
      const precio = Number(plana.precio)
      const precioAnterior = plana.precioAnterior == null ? null : Number(plana.precioAnterior)
      const variacion = plana.variacion != null
        ? Number(plana.variacion)
        : precioAnterior ? (precio - precioAnterior) / precioAnterior : null
      return { ...plana, precio, variacion }
    })
    .filter((fila) => fila.id != null && Number.isFinite(fila.precio))
}

export function filtrarPorPosicion(ranking, posicion) {
  if (!posicion) return ranking
  return ranking.filter((fila) => (fila.posicionCodigo || fila.posicion) === posicion)
}
