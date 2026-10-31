import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import App from '../../src/App'
import { SesionProvider } from '../../src/context/SesionContext'
import { server } from '../../src/test/server'

const messi = { id: 4, nombre: 'Lionel Messi', equipo: 'Inter Miami', liga: 'MLS', posicion: 'DEL' }

describe('filtro de posición del catálogo', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
    window.history.pushState({}, '', '/catalogo')
    window.sessionStorage.setItem('formula11.session', JSON.stringify({ token: 'catalogo-token', usuario: { id: 11, nombre: 'Ana Formula', rol: 'USUARIO' } }))
  })

  it('envía el código de la posición (como la guarda el backend) y muestra el nombre en el chip', async () => {
    const user = userEvent.setup()
    const posiciones = []
    server.use(http.get('/api/jugadores', ({ request }) => {
      posiciones.push(new URL(request.url).searchParams.get('posicion'))
      return HttpResponse.json({ content: [messi], page: 0, size: 9, totalElements: 1, totalPages: 1 })
    }))
    render(<SesionProvider><App /></SesionProvider>)
    await screen.findByText('Lionel Messi')

    await user.selectOptions(screen.getByLabelText('Posición'), 'Delantero')

    expect(screen.getByLabelText('Posición')).toHaveValue('DEL')
    expect(posiciones.at(-1)).toBe('DEL')
    const chips = screen.getByRole('group', { name: 'Filtros activos' })
    expect(within(chips).getByRole('button', { name: 'Quitar filtro Delantero' })).toHaveTextContent('Delantero ×')
  })
})
