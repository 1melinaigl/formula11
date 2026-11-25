import { renderHook, waitFor, act } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { SesionProvider } from '../../src/context/SesionContext'
import { ultimaPaginaValida, useConsultaPaginada } from '../../src/hooks/useConsultaPaginada'
import { server } from '../../src/test/server'

function wrapper({ children }) {
  return <SesionProvider>{children}</SesionProvider>
}

const construirUrl = (consulta) => `/api/prueba?page=${consulta.page}`
const normalizar = (data) => ({ items: data?.content ?? [], page: data?.page ?? 0 })

describe('ultimaPaginaValida', () => {
  it('devuelve la última página cuando se pidió una que ya no existe', () => {
    expect(ultimaPaginaValida({ content: [], totalElements: 21, totalPages: 3 }, 5)).toBe(2)
  })

  it('devuelve null si la página tiene contenido, si no hay resultados o si la página existe', () => {
    expect(ultimaPaginaValida({ content: [{}], totalElements: 21, totalPages: 3 }, 1)).toBeNull()
    expect(ultimaPaginaValida({ content: [], totalElements: 0, totalPages: 0 }, 2)).toBeNull()
    expect(ultimaPaginaValida({ content: [], totalElements: 21, totalPages: 3 }, 2)).toBeNull()
    expect(ultimaPaginaValida(null, 0)).toBeNull()
  })
})

describe('useConsultaPaginada', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
    window.sessionStorage.setItem('formula11.session', JSON.stringify({ token: 'token', usuario: { id: 7, nombre: 'Ana' } }))
  })

  it('no consulta nada si no está habilitada', async () => {
    const solicitudes = []
    server.use(http.get('/api/prueba', ({ request }) => {
      solicitudes.push(request.url)
      return HttpResponse.json({ content: [], page: 0, totalElements: 0, totalPages: 0 })
    }))

    const { result } = renderHook(() => useConsultaPaginada({
      consultaInicial: { page: 0 }, construirUrl, normalizar, habilitado: false,
    }), { wrapper })

    await new Promise((resolve) => setTimeout(resolve, 50))
    expect(solicitudes).toHaveLength(0)
    expect(result.current.cargando).toBe(false)
    expect(result.current.respuesta).toEqual({ items: [], page: 0 })
  })

  it('corrige una página fuera de rango y normaliza la respuesta válida', async () => {
    const paginas = []
    server.use(http.get('/api/prueba', ({ request }) => {
      const page = Number(new URL(request.url).searchParams.get('page'))
      paginas.push(page)
      const content = page === 1 ? [{ id: 1 }] : []
      return HttpResponse.json({ content, page, totalElements: 10, totalPages: 2 })
    }))

    const { result } = renderHook(() => useConsultaPaginada({
      consultaInicial: { page: 0 }, construirUrl, normalizar,
    }), { wrapper })
    await waitFor(() => expect(result.current.cargando).toBe(false))

    act(() => result.current.actualizarConsulta({ page: 4 }))
    await waitFor(() => expect(result.current.respuesta.items).toHaveLength(1))

    expect(paginas).toEqual([0, 4, 1])
    expect(result.current.consulta.page).toBe(1)
    expect(result.current.cargando).toBe(false)
  })
})
