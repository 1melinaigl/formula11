import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import App from '../../src/App'
import { SesionProvider } from '../../src/context/SesionContext'
import { server } from '../../src/test/server'

function renderRegistro() {
  window.history.pushState({}, '', '/registro')
  return render(<SesionProvider><App /></SesionProvider>)
}

describe('registro', () => {
  beforeEach(() => window.sessionStorage.clear())

  it('valida los límites del DTO junto a cada campo', async () => {
    const user = userEvent.setup()
    renderRegistro()
    await user.type(screen.getByLabelText('Nombre'), 'x'.repeat(101))
    await user.type(screen.getByLabelText('Email'), 'no-es-email')
    await user.type(screen.getByLabelText('Contraseña'), 'corta')
    await user.click(screen.getByRole('button', { name: 'Crear cuenta', exact: true }))

    expect(screen.getByText('El nombre debe tener entre 1 y 100 caracteres.')).toBeInTheDocument()
    expect(screen.getByText('Ingresá un email válido de hasta 254 caracteres.')).toBeInTheDocument()
    expect(screen.getByText('La contraseña debe tener entre 8 y 72 caracteres.')).toBeInTheDocument()
  })

  it('muestra un conflicto de email con mensaje y correlationId', async () => {
    const user = userEvent.setup()
    renderRegistro()
    await user.type(screen.getByLabelText('Nombre'), 'Ana')
    await user.type(screen.getByLabelText('Email'), 'duplicado@example.com')
    await user.type(screen.getByLabelText('Contraseña'), 'Secreto123!')
    await user.click(screen.getByRole('button', { name: 'Crear cuenta', exact: true }))

    expect(await screen.findByText('El email ya está registrado.')).toBeInTheDocument()
    expect(screen.getByText(/correlationId:/)).toBeInTheDocument()
  })

  it('muestra la cuenta creada sin token y deja la sesión iniciada', async () => {
    const user = userEvent.setup()
    renderRegistro()
    await user.type(screen.getByLabelText('Nombre'), 'Ana')
    await user.type(screen.getByLabelText('Email'), 'ana@example.com')
    await user.type(screen.getByLabelText('Contraseña'), 'Secreto123!')
    await user.click(screen.getByRole('button', { name: 'Crear cuenta', exact: true }))

    expect(await screen.findByRole('heading', { name: 'Cuenta creada' })).toBeInTheDocument()
    expect(screen.getAllByText('Ana')).toHaveLength(2)
    expect(screen.getByRole('link', { name: 'Ir al catálogo' })).toBeInTheDocument()
    expect(screen.queryByText('test-token')).not.toBeInTheDocument()
  })

  it('normaliza el error 400 del servidor', async () => {
    server.use(http.post('/api/usuarios/registro', () => HttpResponse.json({ mensaje: 'Datos inválidos.', correlationId: 'registro-400' }, { status: 400 })))
    const user = userEvent.setup()
    renderRegistro()
    await user.type(screen.getByLabelText('Nombre'), 'Ana')
    await user.type(screen.getByLabelText('Email'), 'ana@example.com')
    await user.type(screen.getByLabelText('Contraseña'), 'Secreto123!')
    await user.click(screen.getByRole('button', { name: 'Crear cuenta', exact: true }))

    expect(await screen.findByText('Datos inválidos.')).toBeInTheDocument()
    expect(screen.getByText(/registro-400/)).toBeInTheDocument()
  })
})
