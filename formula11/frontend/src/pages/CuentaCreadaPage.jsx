import { Link, Navigate } from 'react-router-dom'
import { useSesion } from '../hooks/useSesion'

export default function CuentaCreadaPage() {
  const { cuentaCreada } = useSesion()

  if (!cuentaCreada) return <Navigate replace to="/" />

  return (
    <section className="screen screen-center">
      <article className="panel auth-panel">
        <h1 className="panel-heading">Cuenta creada</h1>
        <dl className="account-summary">
          <dt>Nombre</dt><dd>{cuentaCreada.nombre}</dd>
          <dt>Email</dt><dd>{cuentaCreada.email}</dd>
        </dl>
        <div className="form-actions" style={{ padding: '0 22px 22px' }}>
          <Link className="btn" to="/catalogo">Ir al catálogo</Link>
        </div>
      </article>
    </section>
  )
}
