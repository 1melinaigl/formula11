import { renderHook, act } from '@testing-library/react'
import { beforeEach, describe, expect, it } from 'vitest'
import { SesionProvider } from '../../src/context/SesionContext'
import { useSesion } from '../../src/hooks/useSesion'

const wrapper = ({ children }) => <SesionProvider>{children}</SesionProvider>

describe('useSesion', () => {
  beforeEach(() => window.sessionStorage.clear())

  it('restaura usuario desde sessionStorage sin exponer token en el valor público', () => {
    window.sessionStorage.setItem('formula11.session', JSON.stringify({ token: 'secret', usuario: { nombre: 'Ana' } }))
    const { result } = renderHook(() => useSesion(), { wrapper })

    expect(result.current.usuario.nombre).toBe('Ana')
    expect(result.current).not.toHaveProperty('token')
  })

  it('cierra sesión y limpia sessionStorage', () => {
    window.sessionStorage.setItem('formula11.session', JSON.stringify({ token: 'secret', usuario: { nombre: 'Ana' } }))
    const { result } = renderHook(() => useSesion(), { wrapper })

    act(() => result.current.cerrarSesion())

    expect(result.current.usuario).toBeNull()
    expect(window.sessionStorage.getItem('formula11.session')).toBeNull()
  })
})
