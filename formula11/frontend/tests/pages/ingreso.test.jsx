import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import App from '../../src/App'
import { SesionProvider } from '../../src/context/SesionContext'
import { server } from '../../src/test/server'

function renderIngreso() {
  window.history.pushState({}, '', '/ingresar')
  return render(<SesionProvider><App /></SesionProvider>)
}

describe('ingreso', () => {
  beforeEach(() => window.sessionStorage.clear())

  it('muestra errores locales sin enviar un formulario inválido', async () => {
    const user = userEvent.setup()
    renderIngreso()

    await user.click(screen.getByRole('button', { name: 'Ingresar', exact: true }))

    expect(screen.getByText('Ingresá un email válido de hasta 254 caracteres.')).toBeInTheDocument()
    expect(screen.getByText('La contraseña debe tener entre 8 y 72 caracteres.')).toBeInTheDocument()
  })

  it('muestra el mensaje del servidor y correlationId para credenciales inválidas', async () => {
    const user = userEvent.setup()
    renderIngreso()
    await user.type(screen.getByLabelText('Email'), 'invalida@example.com')
    await user.type(screen.getByLabelText('Contraseña'), 'Secreto123!')
    await user.click(screen.getByRole('button', { name: 'Ingresar', exact: true }))

    expect(await screen.findByText('Email o contraseña incorrectos.')).toBeInTheDocument()
    expect(screen.getByText(/correlationId:/)).toBeInTheDocument()
    expect(window.sessionStorage.getItem('formula11.session')).toBeNull()
  })

  it('inicia sesión, mantiene la cuenta y no muestra el token', async () => {
    const user = userEvent.setup()
    renderIngreso()
    await user.type(screen.getByLabelText('Email'), 'ana@example.com')
    await user.type(screen.getByLabelText('Contraseña'), 'Secreto123!')
    await user.click(screen.getByRole('button', { name: 'Ingresar', exact: true }))

    expect(await screen.findByText('Catálogo vacío')).toBeInTheDocument()
    expect(screen.getByText('Ana Formula')).toBeInTheDocument()
    expect(screen.queryByText('test-token')).not.toBeInTheDocument()
    expect(window.sessionStorage.getItem('formula11.session')).toContain('test-token')
  })

  it('conserva una sesión existente cuando login responde 401', async () => {
    server.use(http.post('/api/usuarios/login', () => HttpResponse.json({ mensaje: 'Credenciales inválidas.', correlationId: 'login-401' }, { status: 401 })))
    window.sessionStorage.setItem('formula11.session', JSON.stringify({ token: 'existing-token', usuario: { id: 2, nombre: 'Usuario activo', email: 'activo@example.com' } }))
    const user = userEvent.setup()
    renderIngreso()
    await user.type(screen.getByLabelText('Email'), 'invalida@example.com')
    await user.type(screen.getByLabelText('Contraseña'), 'Secreto123!')
    await user.click(screen.getByRole('button', { name: 'Ingresar', exact: true }))

    expect(await screen.findByText('Credenciales inválidas.')).toBeInTheDocument()
    expect(window.sessionStorage.getItem('formula11.session')).toContain('existing-token')
  })
})
