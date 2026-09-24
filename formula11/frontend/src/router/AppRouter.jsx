import { Navigate, Route, Routes } from 'react-router-dom'
import { useSesion } from '../hooks/useSesion'
import AccesoDenegadoPage from '../pages/AccesoDenegadoPage'
import AccesoPage from '../pages/AccesoPage'
import CuentaCreadaPage from '../pages/CuentaCreadaPage'
import InicioPage from '../pages/InicioPage'

function CatalogoPendiente() {
  const { usuario } = useSesion()

  if (!usuario) return <AccesoDenegadoPage />

  return (
    <section className="screen screen-center">
      <article className="panel auth-panel">
        <h1 className="panel-heading">Catálogo de jugadores</h1>
        <div className="form-body">
          <p>La consulta del catálogo estará disponible en la siguiente fase.</p>
        </div>
      </article>
    </section>
  )
}

export default function AppRouter() {
  return (
    <Routes>
      <Route element={<InicioPage />} path="/" />
      <Route element={<AccesoPage />} path="/ingresar" />
      <Route element={<AccesoPage />} path="/registro" />
      <Route element={<CuentaCreadaPage />} path="/cuenta-creada" />
      <Route element={<CatalogoPendiente />} path="/catalogo" />
      <Route element={<Navigate replace to="/" />} path="*" />
    </Routes>
  )
}
