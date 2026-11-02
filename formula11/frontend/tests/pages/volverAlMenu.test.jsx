import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import App from '../../src/App'
import { SesionProvider } from '../../src/context/SesionContext'

function renderApp(ruta) {
  window.history.pushState({}, '', ruta)
  window.sessionStorage.setItem('formula11.session', JSON.stringify({ token: 'menu-token', usuario: { id: 11, nombre: 'Ana Formula', rol: 'USUARIO' } }))
  return render(<SesionProvider><App /></SesionProvider>)
}

describe('volver al menú', () => {
  beforeEach(() => window.sessionStorage.clear())

  it('el logo del encabezado lleva al menú principal', async () => {
    const user = userEvent.setup()
    renderApp('/ranking')

    const logo = screen.getByRole('link', { name: 'Formula 11, ir al menú principal' })
    expect(logo).toHaveAttribute('href', '/')
    await user.click(logo)

    expect(window.location.pathname).toBe('/')
    expect(screen.getByRole('navigation', { name: 'Menú principal' })).toBeInTheDocument()
  })

  it.each([
    ['/senales', 'Señales'],
    ['/operaciones', 'Historial de operaciones'],
    ['/jugadores/7', 'Detalle del jugador'],
  ])('%s tiene el botón "Menú" que vuelve al inicio', async (ruta, titulo) => {
    const user = userEvent.setup()
    renderApp(ruta)

    expect(await screen.findByRole('heading', { name: titulo, level: 1 })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Menú' }))

    expect(window.location.pathname).toBe('/')
    expect(screen.getByRole('navigation', { name: 'Menú principal' })).toBeInTheDocument()
  })
})
