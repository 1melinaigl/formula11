import { Navigate, Route, Routes } from 'react-router-dom'
import AccesoPage from '../pages/AccesoPage'
import CuentaCreadaPage from '../pages/CuentaCreadaPage'
import CatalogoPage from '../pages/CatalogoPage'
import InicioPage from '../pages/InicioPage'

export default function AppRouter() {
  return (
    <Routes>
      <Route element={<InicioPage />} path="/" />
      <Route element={<AccesoPage />} path="/ingresar" />
      <Route element={<AccesoPage />} path="/registro" />
      <Route element={<CuentaCreadaPage />} path="/cuenta-creada" />
      <Route element={<CatalogoPage />} path="/catalogo" />
      <Route element={<Navigate replace to="/" />} path="*" />
    </Routes>
  )
}
