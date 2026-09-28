import { useLocation } from 'react-router-dom'
import { useSesion } from '../../hooks/useSesion'

const breadcrumbs = {
  '/': 'Menú principal',
  '/ingresar': 'Acceso',
  '/registro': 'Crear cuenta',
  '/cuenta-creada': 'Cuenta creada',
  '/catalogo': 'Catálogo de jugadores',
}

export default function Encabezado() {
  const { usuario } = useSesion()
  const { pathname } = useLocation()

  return (
    <header className="site-header">
      <div className="logo" aria-label="Formula 11">FORMULA <strong>11</strong></div>
      <div className="breadcrumb">{breadcrumbs[pathname] || 'Formula 11'}</div>
      <div className={`session-chip ${usuario ? 'active' : ''}`}>
        <span>{usuario ? usuario.nombre : 'Sin sesión'}</span>
      </div>
    </header>
  )
}
