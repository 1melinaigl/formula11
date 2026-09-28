import { Link } from 'react-router-dom'

export default function AccesoDenegadoPage({ error }) {
  return (
    <section className="screen screen-center">
      <article className="panel auth-panel">
        <h1 className="panel-heading">Acceso denegado</h1>
        <div className="form-body">
          <p>{error?.api?.mensaje || 'No hay una sesión válida para consultar esta pantalla.'}</p>
          {error?.api?.correlationId && <small className="correlation-id">correlationId: {error.api.correlationId}</small>}
          <div className="form-actions">
            <Link className="btn" to="/ingresar">Ingresar</Link>
          </div>
        </div>
      </article>
    </section>
  )
}
