import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import App from '../../src/App'
import { SesionProvider } from '../../src/context/SesionContext'

function renderApp() {
  return render(<SesionProvider><App /></SesionProvider>)
}

describe('menú principal', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
    window.history.pushState({}, '', '/')
  })

  it('navega con flechas y Enter hacia el ingreso', async () => {
    const user = userEvent.setup()
    renderApp()

    const access = screen.getByRole('button', { name: /Ingresar \/ Crear cuenta/ })
    expect(access).toHaveFocus()
    await user.keyboard('{Enter}')

    expect(screen.getByRole('tab', { name: 'Ingresar' })).toHaveAttribute('aria-selected', 'true')
  })

  it('muestra las opciones futuras bloqueadas', () => {
    renderApp()

    expect(screen.getAllByText('PRÓXIMAMENTE')).toHaveLength(2)
    expect(screen.getByText('Sin sesión')).toBeInTheDocument()
  })
})
