import { lazy, Suspense } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import EstadoCarga from '../components/comun/EstadoCarga'
import AccesoPage from '../pages/AccesoPage'
import CuentaCreadaPage from '../pages/CuentaCreadaPage'
import CatalogoPage from '../pages/CatalogoPage'
import HistorialPage from '../pages/HistorialPage'
import InicioPage from '../pages/InicioPage'
import RankingPage from '../pages/RankingPage'

// Carga diferida: estas páginas traen la librería de gráficos y no hace falta en el resto de las pantallas.
const DetalleJugadorPage = lazy(() => import('../pages/DetalleJugadorPage'))
const PortfolioPage = lazy(() => import('../pages/PortfolioPage'))

export default function AppRouter() {
  return (
    <Suspense fallback={<EstadoCarga />}>
      <Routes>
        <Route element={<InicioPage />} path="/" />
        <Route element={<AccesoPage />} path="/ingresar" />
        <Route element={<AccesoPage />} path="/registro" />
        <Route element={<CuentaCreadaPage />} path="/cuenta-creada" />
        <Route element={<CatalogoPage />} path="/catalogo" />
        <Route element={<RankingPage />} path="/ranking" />
        <Route element={<PortfolioPage />} path="/portfolio" />
        <Route element={<HistorialPage />} path="/operaciones" />
        <Route element={<DetalleJugadorPage />} path="/jugadores/:id" />
        <Route element={<Navigate replace to="/" />} path="*" />
      </Routes>
    </Suspense>
  )
}
