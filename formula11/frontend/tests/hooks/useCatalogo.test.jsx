import { renderHook, waitFor, act } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { SesionProvider } from '../../src/context/SesionContext'
import { useCatalogo } from '../../src/hooks/useCatalogo'
import { server } from '../../src/test/server'

function wrapper({ children }) {
  return <SesionProvider>{children}</SesionProvider>
}

describe('useCatalogo', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
    window.sessionStorage.setItem('formula11.session', JSON.stringify({ token: 'catalog-token', usuario: { id: 11, nombre: 'Ana Formula' } }))
  })

  it('aplica debounce al equipo y reinicia la página al cambiar un filtro', async () => {
    const solicitudes = []
    server.use(http.get('/api/jugadores', ({ request }) => {
      solicitudes.push(new URL(request.url))
      return HttpResponse.json({ content: [], page: 0, size: 9, totalElements: 0, totalPages: 0 })
    }))

    const { result } = renderHook(() => useCatalogo(), { wrapper })
    await waitFor(() => expect(solicitudes).toHaveLength(1))
    expect(solicitudes[0].searchParams.get('page')).toBe('0')
    expect(solicitudes[0].searchParams.get('size')).toBe('9')
    expect(solicitudes[0].searchParams.get('sort')).toBe('nombre,asc')
    for (const vacio of ['liga', 'equipo', 'posicion']) expect(solicitudes[0].searchParams.has(vacio)).toBe(false)

    act(() => result.current.cambiarPagina(3))
    await waitFor(() => expect(solicitudes).toHaveLength(2))
    expect(solicitudes[1].searchParams.get('page')).toBe('3')

    act(() => result.current.actualizarFiltro('equipo', 'Real Madrid'))
    expect(solicitudes).toHaveLength(2)
    await waitFor(() => expect(solicitudes).toHaveLength(3), { timeout: 1000 })
    expect(solicitudes[2].searchParams.get('page')).toBe('0')
    expect(solicitudes[2].searchParams.get('equipo')).toBe('Real Madrid')
  })

  it('recupera una sola vez la última página ante una respuesta fuera de rango', async () => {
    const solicitudes = []
    server.use(http.get('/api/jugadores', ({ request }) => {
      const url = new URL(request.url)
      const page = Number(url.searchParams.get('page'))
      expect(url.searchParams.get('size')).toBe('9')
      solicitudes.push(page)
      if (page >= 2) return HttpResponse.json({ content: [], page, size: 9, totalElements: 21, totalPages: 2 })
      return HttpResponse.json({ content: page === 1 ? [{ id: 21, nombre: 'Último' }] : [], page, size: 9, totalElements: 21, totalPages: 2 })
    }))

    const { result } = renderHook(() => useCatalogo(), { wrapper })
    await waitFor(() => expect(solicitudes).toHaveLength(1))
    act(() => result.current.cambiarPagina(3))
    await waitFor(() => expect(solicitudes).toHaveLength(3))

    expect(solicitudes).toEqual([0, 3, 1])
    expect(result.current.page).toBe(1)
    expect(result.current.jugadores[0].nombre).toBe('Último')
  })
})
