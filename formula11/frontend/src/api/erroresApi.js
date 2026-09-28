export function crearErrorApi({ mensaje, timestamp = null, correlationId, status }) {
  const error = new Error(mensaje || 'No se pudo completar la solicitud.')
  error.api = {
    mensaje: mensaje || 'No se pudo completar la solicitud.',
    timestamp,
    correlationId: correlationId || 'No disponible',
    status,
  }
  return error
}

export function esErrorApi(error) {
  return Boolean(error?.api)
}
