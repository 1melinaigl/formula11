import { useContext } from 'react'
import { SesionContext } from '../context/contextoSesion'

export function useSesion() {
  const context = useContext(SesionContext)

  if (!context) {
    throw new Error('useSesion debe utilizarse dentro de SesionProvider.')
  }

  return context
}
