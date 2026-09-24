import { Link } from 'react-router-dom'

export default function AccesoDenegadoPage() {
  return (
    <section className="screen screen-center">
      <article className="panel auth-panel">
        <h1 className="panel-heading">Acceso denegado</h1>
        <div className="form-body">
          <p>No hay una sesión válida para consultar esta pantalla.</p>
          <div className="form-actions">
            <Link className="btn" to="/ingresar">Ingresar</Link>
          </div>
        </div>
      </article>
    </section>
  )
}
