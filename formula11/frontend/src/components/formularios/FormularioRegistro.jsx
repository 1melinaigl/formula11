import { useState } from 'react'
import CampoFormulario from './CampoFormulario'
import AvisoServicio from './AvisoServicio'
import EstadoCarga from '../comun/EstadoCarga'

function validar({ nombre, email, password }) {
  const errors = {}
  if (nombre.length < 1 || nombre.length > 100) errors.nombre = 'El nombre debe tener entre 1 y 100 caracteres.'
  if (!email.trim() || !/^\S+@\S+\.\S+$/.test(email.trim()) || email.trim().length > 254) errors.email = 'Ingresá un email válido de hasta 254 caracteres.'
  if (password.length < 8 || password.length > 72) errors.password = 'La contraseña debe tener entre 8 y 72 caracteres.'
  return errors
}

export default function FormularioRegistro({ onSubmit, cargando, error }) {
  const [values, setValues] = useState({ nombre: '', email: '', password: '' })
  const [fieldErrors, setFieldErrors] = useState({})

  function update(event) {
    const { name, value } = event.target
    setValues((current) => ({ ...current, [name]: value }))
    setFieldErrors((current) => ({ ...current, [name]: undefined }))
  }

  async function submit(event) {
    event.preventDefault()
    const errors = validar(values)
    setFieldErrors(errors)
    if (Object.keys(errors).length) return
    await onSubmit({ nombre: values.nombre, email: values.email.trim(), password: values.password })
  }

  return (
    <form className="form-body" onSubmit={submit} noValidate>
      <AvisoServicio error={error} />
      <CampoFormulario id="registro-nombre" label="Nombre" name="nombre" type="text" autoComplete="name" value={values.nombre} onChange={update} error={fieldErrors.nombre} />
      <CampoFormulario id="registro-email" label="Email" name="email" type="email" autoComplete="email" value={values.email} onChange={update} error={fieldErrors.email} />
      <CampoFormulario id="registro-password" label="Contraseña" name="password" type="password" autoComplete="new-password" value={values.password} onChange={update} error={fieldErrors.password} />
      {cargando && <EstadoCarga mensaje="Creando cuenta..." />}
      <div className="form-actions">
        <button className="btn" disabled={cargando} type="submit">Crear cuenta</button>
      </div>
    </form>
  )
}
