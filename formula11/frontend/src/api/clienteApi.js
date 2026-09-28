import { crearErrorApi } from './erroresApi'

function obtenerHeader(headers, nombre) {
  return headers.get(nombre) || headers.get(nombre.toLowerCase()) || null
}

export async function solicitar(url, { method = 'GET', body, token, protegida = false, onUnauthorized } = {}) {
  const correlationId = crypto.randomUUID()
  const headers = {
    Accept: 'application/json',
    'X-Correlation-Id': correlationId,
  }

  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch(url, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })

  const responseCorrelationId = obtenerHeader(response.headers, 'X-Correlation-Id')
  const contentType = response.headers.get('content-type') || ''
  const data = contentType.includes('application/json') ? await response.json() : null

  if (!response.ok) {
    if (response.status === 401 && protegida) {
      onUnauthorized?.()
    }

    throw crearErrorApi({
      mensaje: data?.mensaje,
      timestamp: data?.timestamp,
      correlationId: data?.correlationId || responseCorrelationId || correlationId,
      status: response.status,
    })
  }

  return data
}
