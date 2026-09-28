import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useSesion } from '../hooks/useSesion'
import FormularioIngreso from '../components/formularios/FormularioIngreso'
import FormularioRegistro from '../components/formularios/FormularioRegistro'

export default function AccesoPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const { ingresar, registrar, cargando } = useSesion()
  const modoRegistro = location.pathname === '/registro'
  const [error, setError] = useState(null)

  async function submit(datos) {
    setError(null)
    try {
      if (modoRegistro) {
        await registrar(datos)
        navigate('/cuenta-creada')
      } else {
        await ingresar(datos)
        navigate('/catalogo')
      }
    } catch (requestError) {
      setError(requestError)
    }
  }

  return (
    <section className="screen screen-center">
      <article className="panel auth-panel">
        <div className="auth-tabs" role="tablist" aria-label="Acceso a Formula 11">
          <button className={`auth-tab ${!modoRegistro ? 'active' : ''}`} onClick={() => navigate('/ingresar')} role="tab" aria-selected={!modoRegistro} type="button">Ingresar</button>
          <button className={`auth-tab ${modoRegistro ? 'active' : ''}`} onClick={() => navigate('/registro')} role="tab" aria-selected={modoRegistro} type="button">Crear cuenta</button>
        </div>
        {modoRegistro ? (
          <FormularioRegistro onSubmit={submit} cargando={cargando} error={error} />
        ) : (
          <FormularioIngreso onSubmit={submit} cargando={cargando} error={error} />
        )}
        <div className="form-actions" style={{ padding: '0 22px 22px' }}>
          <Link className="btn ghost" to="/">Volver</Link>
        </div>
      </article>
    </section>
  )
}
