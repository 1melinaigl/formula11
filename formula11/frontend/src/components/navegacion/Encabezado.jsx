import { Link, useLocation } from 'react-router-dom'
import { useSesion } from '../../hooks/useSesion'

const breadcrumbs = {
  '/': 'Menú principal',
  '/ingresar': 'Acceso',
  '/registro': 'Crear cuenta',
  '/cuenta-creada': 'Cuenta creada',
  '/catalogo': 'Catálogo de jugadores',
  '/ranking': 'Ranking',
  '/portfolio': 'Mi portfolio',
  '/operaciones': 'Historial de operaciones',
  '/senales': 'Señales',
}

function obtenerBreadcrumb(pathname) {
  if (pathname.startsWith('/jugadores/')) return 'Detalle del jugador'
  return breadcrumbs[pathname] || 'Formula 11'
}

export default function Encabezado() {
  const { usuario } = useSesion()
  const { pathname } = useLocation()

  return (
    <header className="site-header">
      <Link className="logo" to="/" aria-label="Formula 11, ir al menú principal">FORMULA <strong>11</strong></Link>
      <div className="breadcrumb">{obtenerBreadcrumb(pathname)}</div>
      <div className={`session-chip ${usuario ? 'active' : ''}`}>
        <span>{usuario ? usuario.nombre : 'Sin sesión'}</span>
      </div>
    </header>
  )
}
