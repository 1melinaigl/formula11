import { useCallback, useMemo, useState } from 'react'
import { solicitar } from '../api/clienteApi'
import { endpoints } from '../api/endpoints'
import { SesionContext } from './contextoSesion'

const STORAGE_KEY = 'formula11.session'

function leerSesion() {
  try {
    const stored = sessionStorage.getItem(STORAGE_KEY)
    return stored ? JSON.parse(stored) : null
  } catch {
    sessionStorage.removeItem(STORAGE_KEY)
    return null
  }
}

export function SesionProvider({ children }) {
  const [sesion, setSesion] = useState(leerSesion)
  const [cuentaCreada, setCuentaCreada] = useState(null)
  const [cargando, setCargando] = useState(false)

  const limpiarSesion = useCallback(() => {
    sessionStorage.removeItem(STORAGE_KEY)
    setSesion(null)
  }, [])

  const guardarSesion = useCallback((response) => {
    const { token, ...usuario } = response
    const nuevaSesion = { token, usuario }
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(nuevaSesion))
    setSesion(nuevaSesion)
    return usuario
  }, [])

  const ingresar = useCallback(async (datos) => {
    setCargando(true)
    try {
      const response = await solicitar(endpoints.login, { method: 'POST', body: datos })
      return guardarSesion(response)
    } finally {
      setCargando(false)
    }
  }, [guardarSesion])

  const registrar = useCallback(async (datos) => {
    setCargando(true)
    try {
      const response = await solicitar(endpoints.registro, { method: 'POST', body: datos })
      const usuario = guardarSesion(response)
      setCuentaCreada(usuario)
      return usuario
    } finally {
      setCargando(false)
    }
  }, [guardarSesion])

  const solicitarProtegida = useCallback((url, options = {}) => {
    const stored = leerSesion()
    return solicitar(url, {
      ...options,
      token: stored?.token,
      protegida: true,
      onUnauthorized: limpiarSesion,
    })
  }, [limpiarSesion])

  const value = useMemo(() => ({
    usuario: sesion?.usuario || null,
    estado: sesion ? 'autenticada' : 'anonima',
    restaurando: false,
    cargando,
    cuentaCreada,
    ingresar,
    registrar,
    cerrarSesion: limpiarSesion,
    solicitarProtegida,
  }), [sesion, cargando, cuentaCreada, ingresar, registrar, limpiarSesion, solicitarProtegida])

  return <SesionContext.Provider value={value}>{children}</SesionContext.Provider>
}
