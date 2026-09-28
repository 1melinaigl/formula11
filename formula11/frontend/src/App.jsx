import { BrowserRouter } from 'react-router-dom'
import Encabezado from './components/navegacion/Encabezado'
import CanchaFondo from './components/comun/CanchaFondo'
import AppRouter from './router/AppRouter'

function App() {
  return (
    <BrowserRouter>
      <CanchaFondo />
      <div id="app-shell">
        <Encabezado />
        <main className="app-main">
          <AppRouter />
        </main>
      </div>
    </BrowserRouter>
  )
}

export default App
