import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import App from '../../src/App'
import { SesionProvider } from '../../src/context/SesionContext'
import { server } from '../../src/test/server'

const fila = { puesto: 1, jugador: { id: 5, nombre: 'Harry Kane', equipo: 'FC Bayern München', liga: 'Bundesliga', posicion: 'DEL' }, precio: 1.44, variacion: 20, score: 0.91 }

function pagina(estrategia) {
  return { content: [fila], page: 0, size: 20, totalElements: 1, totalPages: 1, estrategia, version: 1, fecha: '2026-10-25' }
}

function renderRanking(rol) {
  window.history.pushState({}, '', '/ranking')
  window.sessionStorage.setItem('formula11.session', JSON.stringify({ token: 'estrategia-token', usuario: { id: 8, nombre: 'Ana Formula', rol } }))
  return render(<SesionProvider><App /></SesionProvider>)
}

describe('cambiar la estrategia activa desde el ranking', () => {
  beforeEach(() => window.sessionStorage.clear())

  it('el superusuario activa otra estrategia y el ranking se vuelve a pedir', async () => {
    let activa = 'RENDIMIENTO_POSICION'
    let token = null
    server.use(
      http.get('/api/jugadores/ranking', () => HttpResponse.json(pagina(activa))),
      http.post('/api/estrategias/:codigo/activar', ({ request, params }) => {
        token = request.headers.get('Authorization')
        activa = params.codigo
        return HttpResponse.json({ codigo: activa, nombre: 'Consistencia', version: 1, activa: true })
      }),
    )
    renderRanking('SUPERUSUARIO')

    const selector = await screen.findByLabelText('Estrategia del mercado')
    const boton = screen.getByRole('button', { name: 'Activar' })
    expect(selector).toHaveValue('RENDIMIENTO_POSICION')
    expect(boton).toBeDisabled()
    expect(screen.getByText(/de todos los usuarios/)).toBeInTheDocument()

    await userEvent.selectOptions(selector, 'CONSISTENCIA')
    expect(activa).toBe('RENDIMIENTO_POSICION')
    await userEvent.click(boton)

    expect(await screen.findByText('Estrategia activa: Consistencia v1 · al 25/10/2026')).toBeInTheDocument()
    expect(token).toBe('Bearer estrategia-token')
    expect(activa).toBe('CONSISTENCIA')
    expect(screen.getByRole('button', { name: 'Activar' })).toBeDisabled()
  })

  it('un usuario común no ve el selector', async () => {
    server.use(http.get('/api/jugadores/ranking', () => HttpResponse.json(pagina('RENDIMIENTO_POSICION'))))
    renderRanking('USUARIO')

    expect(await screen.findByRole('table')).toBeInTheDocument()
    expect(screen.queryByLabelText('Estrategia del mercado')).not.toBeInTheDocument()
  })

  it('si el backend rechaza el cambio muestra el error y mantiene el ranking', async () => {
    server.use(
      http.get('/api/jugadores/ranking', () => HttpResponse.json(pagina('RENDIMIENTO_POSICION'))),
      http.post('/api/estrategias/:codigo/activar', () => HttpResponse.json(
        { mensaje: 'Solo el superusuario puede activar estrategias', timestamp: '2026-11-02T12:00:00Z', correlationId: 'activar-403' },
        { status: 403 },
      )),
    )
    renderRanking('SUPERUSUARIO')

    await userEvent.selectOptions(await screen.findByLabelText('Estrategia del mercado'), 'CONSISTENCIA')
    await userEvent.click(screen.getByRole('button', { name: 'Activar' }))

    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Solo el superusuario puede activar estrategias'))
    expect(screen.getByRole('alert')).toHaveTextContent('activar-403')
    expect(screen.getByRole('table')).toBeInTheDocument()
    expect(screen.getByText('Estrategia activa: Rendimiento por posición v1 · al 25/10/2026')).toBeInTheDocument()
  })
})
