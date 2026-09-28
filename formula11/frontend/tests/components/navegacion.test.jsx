import { render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it } from 'vitest'
import App from '../../src/App'
import { SesionProvider } from '../../src/context/SesionContext'

describe('encabezado de navegación', () => {
  beforeEach(() => window.sessionStorage.clear())

  it('muestra el nombre de la sesión y permite cerrarla desde el menú', async () => {
    window.sessionStorage.setItem('formula11.session', JSON.stringify({ token: 'secret', usuario: { nombre: 'Capitana', email: 'capitana@example.com' } }))
    render(<SesionProvider><App /></SesionProvider>)

    expect(screen.getByText('Capitana')).toBeInTheDocument()
    await screen.findByRole('button', { name: /Cerrar sesión/ })
    expect(screen.queryByText('secret')).not.toBeInTheDocument()
  })
})
